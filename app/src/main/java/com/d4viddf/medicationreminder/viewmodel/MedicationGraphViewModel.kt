package com.d4viddf.medicationreminder.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.MedicationReminderRepository // Corrected import
import com.d4viddf.medicationreminder.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth // Added for monthly graph
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException // Added for parseTakenAt helper
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MedicationGraphViewModel @Inject constructor(
    private val reminderRepository: MedicationReminderRepository,
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private val _graphData = MutableStateFlow<Map<String, Int>>(emptyMap())
    val graphData: StateFlow<Map<String, Int>> = _graphData.asStateFlow()

    private val _medicationName = MutableStateFlow<String>("")
    val medicationName: StateFlow<String> = _medicationName.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false) // Added isLoading StateFlow
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null) // Added error StateFlow
    val error: StateFlow<String?> = _error.asStateFlow()

    private val TAG = "MedicationGraphVM"

    private fun parseTakenAt(takenAtString: String?): LocalDateTime? {
        if (takenAtString.isNullOrEmpty()) return null
        return try {
            LocalDateTime.parse(takenAtString) // Assumes ISO_LOCAL_DATE_TIME
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Error parsing takenAt timestamp: $takenAtString", e)
            null
        }
    }

    fun clearGraphData() {
        _graphData.value = emptyMap()
        // _medicationName.value = "" // Optionally clear name too, or keep it
        Log.d(TAG, "Graph data cleared.")
    }

    fun loadWeeklyGraphData(medicationId: Int, currentWeekDays: List<LocalDate>) {
        if (currentWeekDays.isEmpty()) { // Allow empty list to clear data
            Log.d(TAG, "currentWeekDays is empty, clearing graph data for weekly view.")
            _graphData.value = emptyMap()
            // Potentially set an error or specific state if a non-empty list was expected but not 7 days
            return
        }
         if (currentWeekDays.count() != 7) { // Changed .size to .count()
            Log.w(TAG, "Invalid currentWeekDays list size: ${currentWeekDays.count()}. Expected 7 or 0 to clear.") // Changed .size to .count()
            _graphData.value = emptyMap() // Reset or handle error appropriately
            return
        }


        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d(TAG, "Loading weekly graph data for medicationId: $medicationId, week: $currentWeekDays")

                val medication = medicationRepository.getMedicationById(medicationId)
                if (medication?.name != _medicationName.value) { // Fetch name only if different or not set
                    _medicationName.value = medication?.name ?: "Medication $medicationId"
                }
                Log.d(TAG, "Medication name: ${_medicationName.value}")

                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                Log.d(TAG, "Total reminders fetched: ${allReminders.size}")

                val weekStartDateTime = currentWeekDays.first().atStartOfDay()
                val weekEndDateTime = currentWeekDays.last().atTime(LocalTime.MAX)
                Log.d(TAG, "Week date range: $weekStartDateTime to $weekEndDateTime")

                // Filter taken reminders within the current week
                val takenRemindersThisWeek = allReminders.mapNotNull { reminder ->
                    if (!reminder.isTaken) return@mapNotNull null
                    val takenDateTime = parseTakenAt(reminder.takenAt) ?: return@mapNotNull null
                    if (!takenDateTime.isBefore(weekStartDateTime) && !takenDateTime.isAfter(weekEndDateTime)) {
                        takenDateTime.toLocalDate() // Return the LocalDate if within range
                    } else {
                        null
                    }
                }
                Log.d(TAG, "Taken reminder dates this week: ${takenRemindersThisWeek.size}")

                // Group taken reminders by date
                val dosesByDate = takenRemindersThisWeek.groupingBy { it }.eachCount()
                Log.d(TAG, "Doses grouped by date: $dosesByDate")

                // Initialize map for the week with 0 counts
                val weeklyDataMap = LinkedHashMap<String, Int>() // Use LinkedHashMap to preserve day order
                val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) // Short day name, e.g., "Mon"

                currentWeekDays.forEach { day ->
                    // Using getDisplayName for potentially localized short day name might be better
                    // but EEE is standard. For this example, EEE is fine.
                    // val dayName = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val dayName = day.format(dayFormatter)
                    weeklyDataMap[dayName] = dosesByDate[day]?.size ?: 0
                }
                Log.d(TAG, "Final weekly data map: $weeklyDataMap")

                _graphData.value = weeklyDataMap

            } catch (e: Exception) {
                Log.e(TAG, "Error loading weekly graph data for medId $medicationId", e)
                _error.value = "Failed to load weekly graph data."
                _graphData.value = emptyMap()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMonthlyGraphData(medicationId: Int, targetMonth: YearMonth) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            // _graphData.value = emptyMap() // Clear previous graph data immediately

            try {
                Log.d(TAG, "Loading monthly graph data for medicationId: $medicationId, month: $targetMonth")

                val medication = medicationRepository.getMedicationById(medicationId)
                 if (medication?.name != _medicationName.value) {
                    _medicationName.value = medication?.name ?: "Medication $medicationId"
                }
                Log.d(TAG, "Medication name: ${_medicationName.value}")

                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                Log.d(TAG, "Total reminders fetched: ${allReminders.size}")

                val monthStartDateTime = targetMonth.atDay(1).atStartOfDay()
                val monthEndDateTime = targetMonth.atEndOfMonth().atTime(LocalTime.MAX)
                Log.d(TAG, "Month date range: $monthStartDateTime to $monthEndDateTime")

                val takenRemindersThisMonth = allReminders.mapNotNull { reminder ->
                    if (!reminder.isTaken) return@mapNotNull null
                    val takenDateTime = parseTakenAt(reminder.takenAt) ?: return@mapNotNull null
                    if (takenDateTime.year == targetMonth.year && takenDateTime.month == targetMonth.month) {
                        takenDateTime.dayOfMonth // Return day of month if within target month
                    } else {
                        null
                    }
                }
                Log.d(TAG, "Taken reminder days this month: ${takenRemindersThisMonth.size}")

                val dosesByDayOfMonth = takenRemindersThisMonth.groupingBy { it }.eachCount()
                Log.d(TAG, "Doses grouped by day of month: $dosesByDayOfMonth")

                val monthlyDataMap = LinkedHashMap<String, Int>()
                val daysInMonth = targetMonth.lengthOfMonth()
                for (day in 1..daysInMonth) {
                    monthlyDataMap[day.toString()] = dosesByDayOfMonth[day] ?: 0
                }
                Log.d(TAG, "Final monthly data map: $monthlyDataMap")
                _graphData.value = monthlyDataMap

            } catch (e: Exception) {
                Log.e(TAG, "Error loading monthly graph data for medId $medicationId, month $targetMonth", e)
                _error.value = "Failed to load monthly graph data."
                _graphData.value = emptyMap()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadYearlyGraphData(medicationId: Int, targetYear: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            // _graphData.value = emptyMap() // Clear previous graph data immediately

            try {
                Log.d(TAG, "Loading yearly graph data for medicationId: $medicationId, year: $targetYear")

                val medication = medicationRepository.getMedicationById(medicationId)
                if (medication?.name != _medicationName.value) {
                     _medicationName.value = medication?.name ?: "Medication $medicationId"
                }
                Log.d(TAG, "Medication name: ${_medicationName.value}")

                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                Log.d(TAG, "Total reminders fetched for yearly graph: ${allReminders.size}")

                val takenRemindersThisYear = allReminders.mapNotNull { reminder ->
                    if (!reminder.isTaken) return@mapNotNull null
                    val takenDateTime = parseTakenAt(reminder.takenAt) ?: return@mapNotNull null
                    if (takenDateTime.year == targetYear) {
                        takenDateTime.month // Return the Month enum
                    } else {
                        null
                    }
                }
                Log.d(TAG, "Taken reminder months this year: ${takenRemindersThisYear.size}")

                val dosesByMonth = takenRemindersThisYear.groupingBy { it }.eachCount()
                Log.d(TAG, "Doses grouped by month enum: $dosesByMonth")

                val yearlyDataMap = LinkedHashMap<String, Int>()
                val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) // Short month name, e.g., "Jan"

                for (month in 1..12) {
                    val monthEnum = java.time.Month.of(month)
                    val monthName = monthEnum.getDisplayName(TextStyle.SHORT, Locale.getDefault()) // More reliable for short names
                    // val monthName = YearMonth.of(targetYear, month).format(monthFormatter) // Alternative if full YearMonth needed
                    yearlyDataMap[monthName] = dosesByMonth[monthEnum] ?: 0
                }
                Log.d(TAG, "Final yearly data map: $yearlyDataMap")
                _graphData.value = yearlyDataMap

            } catch (e: Exception) {
                Log.e(TAG, "Error loading yearly graph data for medId $medicationId, year $targetYear", e)
                _error.value = "Failed to load yearly graph data."
                _graphData.value = emptyMap()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
