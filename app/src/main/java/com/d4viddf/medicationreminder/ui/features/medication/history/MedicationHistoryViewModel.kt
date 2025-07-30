package com.d4viddf.medicationreminder.ui.features.medication.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.model.MedicationHistoryEntry
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class MedicationHistoryViewModel @Inject constructor(
    private val reminderRepository: MedicationReminderRepository,
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private val _medicationName = MutableStateFlow<String>("")
    val medicationName: StateFlow<String> = _medicationName.asStateFlow()

    private val _rawHistory = MutableStateFlow<List<MedicationReminder>>(emptyList())

    private val _filteredAndSortedHistory =
        MutableStateFlow<List<MedicationHistoryEntry>>(emptyList())
    val filteredAndSortedHistory: StateFlow<List<MedicationHistoryEntry>> = _filteredAndSortedHistory.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _dateFilter = MutableStateFlow<Pair<LocalDate?, LocalDate?>?>(null)
    val dateFilter: StateFlow<Pair<LocalDate?, LocalDate?>?> = _dateFilter.asStateFlow()

    private val _sortAscending = MutableStateFlow<Boolean>(false) // Default to descending
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val TAG = "MedHistoryVM"

    fun loadInitialHistory(medicationId: Int, filterDate: LocalDate? = null, filterMonth: YearMonth? = null) {
        Log.d(TAG, "loadInitialHistory called for medicationId: $medicationId, filterDate: $filterDate, filterMonth: $filterMonth")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val medication = medicationRepository.getMedicationById(medicationId)
                _medicationName.value = medication?.name ?: "Medication $medicationId"
                Log.d(TAG, "Medication name set to: ${_medicationName.value}")

                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                Log.d(TAG, "Fetched ${allReminders.size} reminders in total.")

                _rawHistory.value = allReminders.filter { it.isTaken && !it.takenAt.isNullOrBlank() }
                Log.d(TAG, "Raw history (taken reminders with takenAt): ${_rawHistory.value.size}")

                // Apply the initial date filter if provided
                if (filterDate != null) {
                    _dateFilter.value = Pair(filterDate, filterDate)
                    Log.d(TAG, "Applying date filter: $filterDate")
                } else if (filterMonth != null) {
                    _dateFilter.value = Pair(filterMonth.atDay(1), filterMonth.atEndOfMonth())
                    Log.d(TAG, "Applying month filter: $filterMonth")
                } else {
                    _dateFilter.value = null // No filter
                    Log.d(TAG, "No initial date or month filter applied.")
                }
                processHistory() // processHistory will use the _dateFilter value

            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial history for medId $medicationId, filterDate: $filterDate, filterMonth: $filterMonth", e)
                _error.value = "Failed to load medication history."
                _rawHistory.value = emptyList()
                _filteredAndSortedHistory.value = emptyList()
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Initial history loading finished. isLoading: false")
            }
        }
    }

    fun setDateFilter(startDate: LocalDate?, endDate: LocalDate?) {
        Log.d(TAG, "setDateFilter (for UI) called with startDate: $startDate, endDate: $endDate")
        // This function is primarily for the UI DateRangePicker.
        // If both are null, it effectively clears the filter.
        // If one is null and the other is not, it's an open-ended range.
        _dateFilter.value = if (startDate == null && endDate == null) null else Pair(startDate, endDate)
        Log.d(TAG, "Date filter set by UI to: ${_dateFilter.value}")
        processHistory()
    }

    fun setSortOrder(ascending: Boolean) {
        Log.d(TAG, "setSortOrder called with ascending: $ascending")
        _sortAscending.value = ascending
        processHistory()
    }

    private fun parseTakenAt(takenAtString: String?): LocalDateTime? {
        if (takenAtString.isNullOrEmpty()) return null
        return try {
            LocalDateTime.parse(takenAtString) // Assumes ISO_LOCAL_DATE_TIME
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Error parsing takenAt timestamp: $takenAtString", e)
            null
        }
    }

    private fun processHistory() {
        Log.d(TAG, "processHistory called. Current filter: ${_dateFilter.value}, sortAscending: ${_sortAscending.value}")
        viewModelScope.launch { // Use Default dispatcher for processing
            _isLoading.value = true // Indicate processing starts

            val currentRawHistory = _rawHistory.value
            val nameToUse = _medicationName.value.ifEmpty { "Unknown Medication" }

            // Filter by date
            val dateFiltered = if (_dateFilter.value?.first != null || _dateFilter.value?.second != null) {
                currentRawHistory.filter { reminder ->
                    val takenDateTime = parseTakenAt(reminder.takenAt)
                    if (takenDateTime == null) {
                        Log.d(TAG, "Filtering reminderId ${reminder.id}: takenAt='${reminder.takenAt}'. Parse failed or null takenAt.")
                        return@filter false
                    }
                    // Log 1: After parsing takenAt
                    Log.d(TAG, "Filtering reminderId ${reminder.id}: takenAt='${reminder.takenAt}'. Parsed takenDateTime: $takenDateTime")

                    val startDate = _dateFilter.value?.first
                    val endDate = _dateFilter.value?.second
                    val takenDate = takenDateTime.toLocalDate()
                    // Log 2: After extracting takenDate and getting filter dates
                    Log.d(TAG, "ReminderId ${reminder.id}: takenDate=$takenDate, filterStartDate=$startDate, filterEndDate=$endDate")

                    val afterOrOnStartDate = startDate == null || !takenDate.isBefore(startDate)
                    val beforeOrOnEndDate = endDate == null || !takenDate.isAfter(endDate)
                    // Log 3: After comparison logic
                    Log.d(TAG, "ReminderId ${reminder.id}: afterOrOnStartDate=$afterOrOnStartDate, beforeOrOnEndDate=$beforeOrOnEndDate. Will be included: ${afterOrOnStartDate && beforeOrOnEndDate}")

                    afterOrOnStartDate && beforeOrOnEndDate
                }
            } else {
                currentRawHistory
            }
            Log.d(TAG, "After date filtering: ${dateFiltered.size} items.")

            // Transform and Sort
            val transformedAndSorted = dateFiltered.mapNotNull { reminder ->
                parseTakenAt(reminder.takenAt)?.let { originalDateTime ->
                    MedicationHistoryEntry(
                        id = reminder.id.toString(), // Convert Int to String
                        medicationName = nameToUse,
                        dateTaken = originalDateTime.toLocalDate(),
                        timeTaken = originalDateTime.toLocalTime(),
                        originalDateTimeTaken = originalDateTime
                    )
                }
            }.let { list ->
                if (_sortAscending.value) {
                    list.sortedBy { it.originalDateTimeTaken }
                } else {
                    list.sortedByDescending { it.originalDateTimeTaken }
                }
            }
            Log.d(TAG, "After transform and sort: ${transformedAndSorted.size} items. First few: ${transformedAndSorted.take(3)}")

            _filteredAndSortedHistory.value = transformedAndSorted
            _isLoading.value = false // Indicate processing finished
            Log.d(TAG, "processHistory finished. isLoading: false")
        }
    }
}