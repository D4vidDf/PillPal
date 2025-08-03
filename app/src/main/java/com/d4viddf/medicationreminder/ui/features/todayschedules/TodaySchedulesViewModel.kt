package com.d4viddf.medicationreminder.ui.features.todayschedules

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.domain.usecase.ReminderCalculator
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationTypeRepository
import com.d4viddf.medicationreminder.ui.features.todayschedules.model.TodayScheduleUiItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TodaySchedulesViewModel @Inject constructor(
    private val application: Application,
    private val medicationReminderRepository: MedicationReminderRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationTypeRepository: MedicationTypeRepository
) : ViewModel() {

    data class TodaySchedulesState(
        val scheduleItems: Map<String, List<TodayScheduleUiItem>> = emptyMap(),
        val isLoading: Boolean = true,
        val allMedications: List<Medication> = emptyList(),
        val selectedMedicationId: Int? = null,
        val selectedColorName: String? = null,
        val selectedTimeRange: ClosedRange<LocalTime>? = null
    )

    private val _uiState = MutableStateFlow(TodaySchedulesState())
    val uiState: StateFlow<TodaySchedulesState> = _uiState.asStateFlow()

    // Holds the original, unfiltered list of all schedules for the day
    private val originalScheduleItems = MutableStateFlow<List<TodayScheduleUiItem>>(emptyList())

    init {
        loadInitialData()
        observeFilters()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Fetch all medications once to populate the filter dropdown
            val medications = medicationRepository.getAllMedications().first()
            _uiState.value = _uiState.value.copy(allMedications = medications)

            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay().format(ReminderCalculator.storableDateTimeFormatter)
            val endOfDay = today.atTime(LocalTime.MAX).format(ReminderCalculator.storableDateTimeFormatter)

            // Collect the reminders for today
            medicationReminderRepository.getRemindersForDay(startOfDay, endOfDay).collect { reminders ->
                val uiItemsDeferred = reminders.map { reminder ->
                    async {
                        val medication = medicationRepository.getMedicationById(reminder.medicationId)
                        val typeDetails = medication?.typeId?.let {
                            medicationTypeRepository.getMedicationTypeById(it)
                        }
                        TodayScheduleUiItem(
                            reminder = reminder,
                            medicationName = medication?.name
                                ?: application.getString(R.string.info_not_available),
                            medicationDosage = medication?.dosage
                                ?: application.getString(R.string.info_not_available_short),
                            medicationColorName = medication?.color ?: "LIGHT_ORANGE",
                            medicationIconUrl = typeDetails?.imageUrl,
                            medicationTypeName = typeDetails?.name,
                            formattedReminderTime = try {
                                LocalDateTime.parse(
                                    reminder.reminderTime,
                                    ReminderCalculator.storableDateTimeFormatter
                                )
                                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                            } catch (e: Exception) {
                                "N/A"
                            }
                        )
                    }
                }
                // Store the full, unprocessed list
                originalScheduleItems.value = uiItemsDeferred.awaitAll()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // This function continuously observes the filters and the original data,
    // and updates the UI with the filtered list.
    private fun observeFilters() {
        viewModelScope.launch {
            combine(
                originalScheduleItems,
                _uiState
            ) { items, state ->
                val filteredItems = items.filter { item ->
                    val medicationMatch = state.selectedMedicationId == null || item.reminder.medicationId == state.selectedMedicationId
                    val colorMatch = state.selectedColorName == null || item.medicationColorName == state.selectedColorName
                    val timeMatch = state.selectedTimeRange == null ||
                            try {
                                LocalTime.parse(item.formattedReminderTime) in state.selectedTimeRange!!
                            } catch (e: Exception) {
                                true // Don't filter out items with bad time format
                            }
                    medicationMatch && colorMatch && timeMatch
                }
                // Update the publicly exposed state with the filtered and grouped items
                _uiState.value = _uiState.value.copy(
                    scheduleItems = filteredItems.sortedBy { it.formattedReminderTime }.groupBy { it.formattedReminderTime }
                )
            }.collect {} // Terminal operator to keep the flow alive
        }
    }

    fun onMedicationFilterChanged(medicationId: Int?) {
        _uiState.value = _uiState.value.copy(selectedMedicationId = medicationId)
    }

    fun onColorFilterChanged(colorName: String?) {
        _uiState.value = _uiState.value.copy(selectedColorName = colorName)
    }

    fun onTimeRangeFilterChanged(startTime: LocalTime?, endTime: LocalTime?) {
        val range = if (startTime != null && endTime != null) startTime..endTime else null
        _uiState.value = _uiState.value.copy(selectedTimeRange = range)
    }

    fun updateReminderStatus(reminder: MedicationReminder, isTaken: Boolean) {
        viewModelScope.launch {
            val updatedReminder = reminder.copy(
                isTaken = isTaken,
                takenAt = if (isTaken) LocalDateTime.now().format(ReminderCalculator.storableDateTimeFormatter) else null
            )
            if (reminder.isTaken != isTaken) {
                medicationReminderRepository.updateReminder(updatedReminder)
            }
        }
    }
}
