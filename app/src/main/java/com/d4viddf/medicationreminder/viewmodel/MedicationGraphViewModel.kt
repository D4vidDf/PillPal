package com.d4viddf.medicationreminder.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.MedicationReminderRepository // Corrected import
import com.d4viddf.medicationreminder.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// import kotlinx.coroutines.flow.firstOrNull // No longer needed directly in loadWeeklyGraphData due to collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
// import java.time.YearMonth // No longer needed after removing month view
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException // Added for parseTakenAt helper
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

// Data class for Charty
data class ChartyGraphEntry(
    val xValue: String,      // Label for X-axis (e.g., "Mon", "15", "Jan")
    val yValue: Float,       // Value for Y-axis (bar height)
    val isHighlighted: Boolean // Flag to indicate if this bar should be highlighted
)

@HiltViewModel
class MedicationGraphViewModel @Inject constructor(
    private val reminderRepository: MedicationReminderRepository,
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private val _chartyGraphData = MutableStateFlow<List<ChartyGraphEntry>>(emptyList())
    val chartyGraphData: StateFlow<List<ChartyGraphEntry>> = _chartyGraphData.asStateFlow() // For MedicationDetailScreen compatibility

    private val _weeklyChartData = MutableStateFlow<List<ChartyGraphEntry>>(emptyList())
    val weeklyChartData: StateFlow<List<ChartyGraphEntry>> = _weeklyChartData.asStateFlow()

    private val _yearlyChartData = MutableStateFlow<List<ChartyGraphEntry>>(emptyList())
    val yearlyChartData: StateFlow<List<ChartyGraphEntry>> = _yearlyChartData.asStateFlow()

    private val _medicationName = MutableStateFlow<String>("") // This will store current medication name
    val medicationName: StateFlow<String> = _medicationName.asStateFlow()

    // New state flows for tracking current medication and week for reactive updates
    private val _currentMedicationIdForWeeklyChart = MutableStateFlow<Int?>(null)
    // No public getter needed for this, it's internal logic

    private val _currentWeekDaysForWeeklyChart = MutableStateFlow<List<LocalDate>>(emptyList())
    // No public getter needed for this, it's internal logic

    private val _currentDosageQuantity = MutableStateFlow<Float>(1.0f) // Default dosage quantity
    // No public getter needed for this, it's internal logic

    private val _isLoading = MutableStateFlow<Boolean>(false) // Added isLoading StateFlow
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null) // Added error StateFlow
    val error: StateFlow<String?> = _error.asStateFlow()

    private var reminderObserverJob: Job? = null // Job to manage reminder observation lifecycle

    private val TAG = "MedicationGraphVM"

    private fun parseDosageQuantity(dosageString: String?): Float {
        if (dosageString.isNullOrBlank()) {
            return 1.0f
        }
        return try {
            // Extract leading numbers. Handles cases like "1 tablet", "2.5 mg", "500 units"
            val regex = Regex("^(\\d*\\.?\\d+)")
            val matchResult = regex.find(dosageString)
            matchResult?.groups?.get(1)?.value?.toFloat() ?: 1.0f
        } catch (e: NumberFormatException) {
            1.0f // Fallback if conversion fails
        }
    }

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
        _chartyGraphData.value = emptyList()
        _weeklyChartData.value = emptyList()
        _yearlyChartData.value = emptyList()
        // _medicationName.value = "" // Optionally clear name too, or keep it
        Log.d(TAG, "All chart data cleared.")
    }

    fun loadWeeklyGraphData(medicationId: Int, currentWeekDays: List<LocalDate>) {
        Log.d(TAG, "loadWeeklyGraphData: Called with medicationId: $medicationId, currentWeekDays count: ${currentWeekDays.size}. First day: ${currentWeekDays.firstOrNull()}, Last day: ${currentWeekDays.lastOrNull()}")

        if (currentWeekDays.count() != 7) {
            Log.w(TAG, "Invalid currentWeekDays list size: ${currentWeekDays.count()}. Expected 7. Clearing data and cancelling observer.")
            _weeklyChartData.value = emptyList()
            _chartyGraphData.value = emptyList()
            _currentMedicationIdForWeeklyChart.value = null
            _currentWeekDaysForWeeklyChart.value = emptyList()
            reminderObserverJob?.cancel()
            _isLoading.value = false
            return
        }

        val previousMedicationId = _currentMedicationIdForWeeklyChart.value
        _currentMedicationIdForWeeklyChart.value = medicationId
        _currentWeekDaysForWeeklyChart.value = currentWeekDays

        reminderObserverJob?.cancel() // Cancel any existing observer job
        Log.d(TAG, "Previous reminderObserverJob cancelled.")

        reminderObserverJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            Log.i(TAG, "ReminderObserverJob: Started for medId: $medicationId, week: $currentWeekDays")

            // Fetch Medication Details if ID changed or name is missing
            if (medicationId != previousMedicationId || _medicationName.value.isEmpty()) {
                try {
                    Log.d(TAG, "ReminderObserverJob: Fetching medication details for medId: $medicationId")
                    val medication = medicationRepository.getMedicationById(medicationId)
                    if (medication == null) {
                        Log.e(TAG, "ReminderObserverJob: Medication with ID $medicationId not found.")
                        _error.value = "Medication details not found."
                        _weeklyChartData.value = emptyList()
                        _chartyGraphData.value = emptyList()
                        _isLoading.value = false
                        return@launch
                    }
                    _medicationName.value = medication.name ?: "Medication $medicationId"
                    _currentDosageQuantity.value = parseDosageQuantity(medication.dosage)
                    Log.d(TAG, "ReminderObserverJob: Fetched medication: Name='${_medicationName.value}', DosageQty=${_currentDosageQuantity.value}")
                } catch (e: Exception) {
                    Log.e(TAG, "ReminderObserverJob: Error fetching medication details for medId $medicationId", e)
                    _error.value = "Failed to load medication details."
                    _weeklyChartData.value = emptyList()
                    _chartyGraphData.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
            }

            reminderRepository.getRemindersForMedication(medicationId).collect { allReminders ->
                Log.d(TAG, "ReminderObserverJob: Reminders updated for medId $medicationId. Count: ${allReminders.size}. Current week days: ${_currentWeekDaysForWeeklyChart.value.size}")

                val weekDaysToProcess = _currentWeekDaysForWeeklyChart.value
                if (weekDaysToProcess.isEmpty()) {
                    Log.w(TAG, "ReminderObserverJob: weekDaysToProcess is empty inside collect. Clearing chart data.")
                    _weeklyChartData.value = emptyList()
                    _chartyGraphData.value = emptyList()
                    // _isLoading.value = false; // Do not set isLoading to false here, finally block will do it
                    return@collect
                }

                try {
                    val weekStartDateTime = weekDaysToProcess.first().atStartOfDay()
                    val weekEndDateTime = weekDaysToProcess.last().atTime(LocalTime.MAX)
                    Log.d(TAG, "ReminderObserverJob: Processing reminders for week: $weekStartDateTime to $weekEndDateTime")

                    val takenRemindersThisWeek = allReminders.mapNotNull { reminder ->
                        if (!reminder.isTaken) return@mapNotNull null
                        val takenDateTime = parseTakenAt(reminder.takenAt)
                        if (takenDateTime == null) {
                            // Log inside parseTakenAt is sufficient
                            return@mapNotNull null
                        }
                        if (!takenDateTime.isBefore(weekStartDateTime) && !takenDateTime.isAfter(weekEndDateTime)) {
                            takenDateTime.toLocalDate()
                        } else {
                            null
                        }
                    }
                    Log.d(TAG, "ReminderObserverJob: Filtered to ${takenRemindersThisWeek.size} taken reminder dates within this week.")

                    val dosesCountByDate = takenRemindersThisWeek
                        .groupBy { it }
                        .mapValues { entry -> entry.value.size.toFloat() }
                    Log.d(TAG, "ReminderObserverJob: Doses count by date: $dosesCountByDate")

                    val weeklyDataMap = LinkedHashMap<String, Float>()
                    val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                    val today = LocalDate.now()

                    weekDaysToProcess.forEach { day ->
                        val dayName = day.format(dayFormatter)
                        weeklyDataMap[dayName] = dosesCountByDate[day] ?: 0f
                    }

                    val chartEntries = weeklyDataMap.entries.map { entry ->
                        val dayDate = weekDaysToProcess.find { day -> day.format(dayFormatter) == entry.key } // Find the date for highlight check
                        ChartyGraphEntry(
                            xValue = entry.key,
                            yValue = entry.value,
                            isHighlighted = dayDate == today
                        )
                    }
                    Log.d(TAG, "ReminderObserverJob: Reactive update - Final chartEntries size: ${chartEntries.size}. Data: $chartEntries")

                    _weeklyChartData.value = chartEntries
                    _chartyGraphData.value = chartEntries // For compatibility
                    _error.value = null // Clear previous error on success

                } catch (e: Exception) {
                    Log.e(TAG, "ReminderObserverJob: Error processing reminder update for medId $medicationId, week $weekDaysToProcess", e)
                    _error.value = "Failed to process reminder update for week."
                    // Potentially clear chart data here if partial update is not desired
                    // _weeklyChartData.value = emptyList()
                    // _chartyGraphData.value = emptyList()
                } finally {
                    // This finally is for the collect block's try-catch.
                    // isLoading should be set to false at the end of the initial data load or if flow collection stops.
                    // For a continuous flow, isLoading might represent the initial load, then subsequent updates happen "silently".
                    // However, the prompt implies isLoading should be false after processing each update.
                    _isLoading.value = false
                }
            }
        }
    }

    // loadMonthlyGraphData removed

    fun loadYearlyGraphData(medicationId: Int, targetYear: Int) {
        Log.d(TAG, "loadYearlyGraphData: Called with medicationId: $medicationId, targetYear: $targetYear")
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            Log.i(TAG, "loadYearlyGraphData: Starting for medId: $medicationId, year: $targetYear") // Enhanced log
            try {
                val currentMedName = _medicationName.value // Use the already fetched name
                val medication = medicationRepository.getMedicationById(medicationId) // Re-fetch to ensure consistency or use stored one
                if (medication?.name != currentMedName && medication?.name != null) {
                     _medicationName.value = medication.name // Update if different and not null
                }
                // val dosageQuantity = _currentDosageQuantity.value // Use the already parsed dosage
                // Log.d(TAG, "loadYearlyGraphData: Using Name='${_medicationName.value}', DosageQty=$dosageQuantity")
                // For yearly, we might still want to fetch dosage again if not guaranteed to be fresh
                val dosageQuantity = parseDosageQuantity(medication?.dosage)
                if (medication?.name != null && _medicationName.value.isEmpty()) { // If name wasn't set by weekly loader
                    _medicationName.value = medication.name
                }


                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList() // Kept firstOrNull for yearly as it's not reactive yet
                Log.d(TAG, "loadYearlyGraphData: Fetched ${allReminders.size} total raw reminders. Sample: ${allReminders.take(5).map { r -> "ID: ${r.id}, Taken: ${r.isTaken}, Time: ${r.takenAt}" }}")

                val takenRemindersThisYear = allReminders.mapNotNull { reminder ->
                    if (!reminder.isTaken) return@mapNotNull null
                    val takenDateTime = parseTakenAt(reminder.takenAt)
                    if (takenDateTime == null) {
                        Log.w(TAG, "loadYearlyGraphData: Reminder ID ${reminder.id} skipped (takenAt parsing failed or null). takenAt: '${reminder.takenAt}'")
                        return@mapNotNull null
                    }
                    if (takenDateTime.year == targetYear) {
                        takenDateTime.month // Collect only the month for counting events
                    } else {
                        null
                    }
                }
                Log.d(TAG, "loadYearlyGraphData: Filtered to ${takenRemindersThisYear.size} taken reminder events in this year. Sample: ${takenRemindersThisYear.take(5)}")

                val dosesByMonth = takenRemindersThisYear
                    .groupBy { it } // Group by Month enum directly
                    .mapValues { entry ->
                        // Count the number of events for each month
                        entry.value.size.toFloat()
                    }
                Log.d(TAG, "loadYearlyGraphData: Event counts by month enum: $dosesByMonth")

                val yearlyDataMap = LinkedHashMap<String, Float>()
                for (month in 1..12) {
                    val monthEnum = java.time.Month.of(month)
                    val monthName = monthEnum.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    // dosesByMonth[monthEnum] will now be a Float?
                    yearlyDataMap[monthName] = dosesByMonth[monthEnum] ?: 0f // Use 0f
                }
                Log.d(TAG, "loadYearlyGraphData: Constructed yearlyDataMap: $yearlyDataMap")

                val today = LocalDate.now()
                val currentMonthShortNameForHighlight = if (targetYear == today.year) {
                    today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                } else {
                    null
                }

                val chartEntries = yearlyDataMap.map { (monthName, totalDosage) -> // totalDosage is Float
                    ChartyGraphEntry(
                        xValue = monthName,
                        yValue = totalDosage, // Already a Float
                        isHighlighted = monthName.equals(currentMonthShortNameForHighlight, ignoreCase = true)
                    )
                }
                Log.d(TAG, "loadYearlyGraphData: Final chartEntries: $chartEntries")
                _yearlyChartData.value = chartEntries
                // _chartyGraphData.value = chartEntries // Decide if yearly should also go to the generic one
            } catch (e: Exception) {
                Log.e(TAG, "loadYearlyGraphData: Error for medId $medicationId, year $targetYear", e)
                _error.value = "Failed to load yearly graph data."
                _yearlyChartData.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
