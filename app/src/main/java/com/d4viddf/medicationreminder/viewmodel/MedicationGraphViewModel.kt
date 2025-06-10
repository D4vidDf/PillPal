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
        if (takenAtString.isNullOrEmpty()) {
            Log.d(TAG, "parseTakenAt: input string is null or empty.") // Added log
            return null
        }
        return try {
            val parsedTime = LocalDateTime.parse(takenAtString) // Assumes ISO_LOCAL_DATE_TIME
            Log.d(TAG, "parseTakenAt: Successfully parsed '$takenAtString' to '$parsedTime'") // Added log
            parsedTime
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "parseTakenAt: Error parsing takenAt timestamp: '$takenAtString'", e) // Ensure input is logged
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
            Log.i(TAG, "loadWeeklyGraphData: Starting for medId: $medicationId, week: $currentWeekDays") // Enhanced log
            try {
                val medication = medicationRepository.getMedicationById(medicationId)
                if (medication?.name != _medicationName.value) {
                    _medicationName.value = medication?.name ?: "Medication $medicationId"
                }
                Log.d(TAG, "loadWeeklyGraphData: Medication name: ${_medicationName.value}")

                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                Log.d(TAG, "loadWeeklyGraphData: Fetched ${allReminders.size} total raw reminders from repository.")

                val weekStartDateTime = currentWeekDays.first().atStartOfDay()
                val weekEndDateTime = currentWeekDays.last().atTime(LocalTime.MAX)
                Log.d(TAG, "loadWeeklyGraphData: Week date range: $weekStartDateTime to $weekEndDateTime")

                val takenRemindersThisWeek = allReminders.mapNotNull { reminder ->
                    if (!reminder.isTaken) {
                        // Log.v(TAG, "loadWeeklyGraphData: Reminder ID ${reminder.id} skipped (not taken)."); // Optional: very verbose
                        return@mapNotNull null
                    }
                    val takenDateTime = parseTakenAt(reminder.takenAt)
                    if (takenDateTime == null) {
                        Log.w(TAG, "loadWeeklyGraphData: Reminder ID ${reminder.id} skipped (takenAt parsing failed or null). takenAt: '${reminder.takenAt}'")
                        return@mapNotNull null
                    }
                    if (!takenDateTime.isBefore(weekStartDateTime) && !takenDateTime.isAfter(weekEndDateTime)) {
                        takenDateTime.toLocalDate()
                    } else {
                        // Log.v(TAG, "loadWeeklyGraphData: Reminder ID ${reminder.id} skipped (out of date range $weekStartDateTime - $weekEndDateTime). TakenAt: $takenDateTime"); // Optional: verbose
                        null
                    }
                }
                Log.d(TAG, "loadWeeklyGraphData: Filtered to ${takenRemindersThisWeek.size} taken reminders within this week.")
                if (takenRemindersThisWeek.isNotEmpty()) {
                     Log.d(TAG, "loadWeeklyGraphData: First 5 taken dates this week: ${takenRemindersThisWeek.take(5)}")
                }

                val dosesByDate = takenRemindersThisWeek.groupingBy { it }.eachCount()
                Log.d(TAG, "loadWeeklyGraphData: Doses grouped by date: $dosesByDate")

                val weeklyDataMap = LinkedHashMap<String, Int>()
                val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

                currentWeekDays.forEach { day ->
                    val dayName = day.format(dayFormatter)
                    weeklyDataMap[dayName] = dosesByDate[day] ?: 0
                }
                Log.i(TAG, "loadWeeklyGraphData: Final weekly data map for medId $medicationId: $weeklyDataMap") // Enhanced log
                _graphData.value = weeklyDataMap
            } catch (e: Exception) {
                Log.e(TAG, "loadWeeklyGraphData: Error loading weekly graph data for medId $medicationId", e)
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
            Log.i(TAG, "loadMonthlyGraphData: Starting for medId: $medicationId, month: $targetMonth") // Enhanced log
            try {
                val medication = medicationRepository.getMedicationById(medicationId)
                 if (medication?.name != _medicationName.value) {
                    _medicationName.value = medication?.name ?: "Medication $medicationId"
                }
                Log.d(TAG, "loadMonthlyGraphData: Medication name: ${_medicationName.value}")

                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                Log.d(TAG, "loadMonthlyGraphData: Fetched ${allReminders.size} total raw reminders.")

                val monthStartDateTime = targetMonth.atDay(1).atStartOfDay()
                val monthEndDateTime = targetMonth.atEndOfMonth().atTime(LocalTime.MAX)
                Log.d(TAG, "loadMonthlyGraphData: Month date range: $monthStartDateTime to $monthEndDateTime")

                val takenRemindersThisMonth = allReminders.mapNotNull { reminder ->
                    if (!reminder.isTaken) return@mapNotNull null
                    val takenDateTime = parseTakenAt(reminder.takenAt)
                    if (takenDateTime == null) {
                        Log.w(TAG, "loadMonthlyGraphData: Reminder ID ${reminder.id} skipped (takenAt parsing failed or null). takenAt: '${reminder.takenAt}'")
                        return@mapNotNull null
                    }
                    if (takenDateTime.year == targetMonth.year && takenDateTime.month == targetMonth.month) {
                        takenDateTime.dayOfMonth
                    } else {
                        null
                    }
                }
                Log.d(TAG, "loadMonthlyGraphData: Filtered to ${takenRemindersThisMonth.size} taken reminder-days in this month.")
                 if (takenRemindersThisMonth.isNotEmpty()) {
                     Log.d(TAG, "loadMonthlyGraphData: First 5 taken days this month (day of month): ${takenRemindersThisMonth.take(5)}")
                }

                val dosesByDayOfMonth = takenRemindersThisMonth.groupingBy { it }.eachCount()
                Log.d(TAG, "loadMonthlyGraphData: Doses grouped by day of month: $dosesByDayOfMonth")

                val monthlyDataMap = LinkedHashMap<String, Int>()
                val daysInMonth = targetMonth.lengthOfMonth()
                for (day in 1..daysInMonth) {
                    monthlyDataMap[day.toString()] = dosesByDayOfMonth[day] ?: 0
                }
                Log.i(TAG, "loadMonthlyGraphData: Final monthly data map for medId $medicationId: $monthlyDataMap") // Enhanced log
                _graphData.value = monthlyDataMap
            } catch (e: Exception) {
                Log.e(TAG, "loadMonthlyGraphData: Error for medId $medicationId, month $targetMonth", e)
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
            Log.i(TAG, "loadYearlyGraphData: Starting for medId: $medicationId, year: $targetYear") // Enhanced log
            try {
                val medication = medicationRepository.getMedicationById(medicationId)
                if (medication?.name != _medicationName.value) {
                     _medicationName.value = medication?.name ?: "Medication $medicationId"
                }
                Log.d(TAG, "loadYearlyGraphData: Medication name: ${_medicationName.value}")

                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                Log.d(TAG, "loadYearlyGraphData: Fetched ${allReminders.size} total raw reminders.")

                val takenRemindersThisYear = allReminders.mapNotNull { reminder ->
                    if (!reminder.isTaken) return@mapNotNull null
                    val takenDateTime = parseTakenAt(reminder.takenAt)
                    if (takenDateTime == null) {
                        Log.w(TAG, "loadYearlyGraphData: Reminder ID ${reminder.id} skipped (takenAt parsing failed or null). takenAt: '${reminder.takenAt}'")
                        return@mapNotNull null
                    }
                    if (takenDateTime.year == targetYear) {
                        takenDateTime.month
                    } else {
                        null
                    }
                }
                Log.d(TAG, "loadYearlyGraphData: Filtered to ${takenRemindersThisYear.size} taken reminder-months in this year.")
                if (takenRemindersThisYear.isNotEmpty()) {
                     Log.d(TAG, "loadYearlyGraphData: First 5 taken months this year: ${takenRemindersThisYear.take(5)}")
                }

                val dosesByMonth = takenRemindersThisYear.groupingBy { it }.eachCount()
                Log.d(TAG, "loadYearlyGraphData: Doses grouped by month enum: $dosesByMonth")

                val yearlyDataMap = LinkedHashMap<String, Int>()
                // val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) // Not needed if using TextStyle.SHORT

                for (month in 1..12) {
                    val monthEnum = java.time.Month.of(month)
                    val monthName = monthEnum.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    yearlyDataMap[monthName] = dosesByMonth[monthEnum] ?: 0
                }
                Log.i(TAG, "loadYearlyGraphData: Final yearly data map for medId $medicationId: $yearlyDataMap") // Enhanced log
                _graphData.value = yearlyDataMap
            } catch (e: Exception) {
                Log.e(TAG, "loadYearlyGraphData: Error for medId $medicationId, year $targetYear", e)
                _error.value = "Failed to load yearly graph data."
                _graphData.value = emptyMap()
            } finally {
                _isLoading.value = false

            }
        }
    }
}
