package com.d4viddf.medicationreminder.ui.features.medication.graph

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.repository.MedicationDosageRepository
import com.d4viddf.medicationreminder.data.repository.MedicationReminderRepository
import com.d4viddf.medicationreminder.domain.usecase.ReminderCalculator
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val dosageRepository: MedicationDosageRepository
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
    val currentWeekDaysForWeeklyChart: StateFlow<List<LocalDate>> = _currentWeekDaysForWeeklyChart.asStateFlow()

    private val _currentDosageQuantity = MutableStateFlow<Float>(1.0f) // Default dosage quantity
    // No public getter needed for this, it's internal logic

    private val _isLoading = MutableStateFlow<Boolean>(false) // Added isLoading StateFlow
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null) // Added missing declaration
    val error: StateFlow<String?> = _error.asStateFlow()

    // StateFlows for Max Y-Values
    private val _weeklyMaxYValue = MutableStateFlow<Float>(5f)
    val weeklyMaxYValue: StateFlow<Float> = _weeklyMaxYValue.asStateFlow()

    private val _yearlyMaxYValue = MutableStateFlow<Float>(30f) // Default for yearly, e.g. 30
    val yearlyMaxYValue: StateFlow<Float> = _yearlyMaxYValue.asStateFlow()

    private var reminderObserverJob: Job? = null

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

        reminderObserverJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.i(TAG, "ReminderObserverJob: Started for medId: $medicationId, week: $currentWeekDays")

            var medicationForJob = try { // Renamed to avoid conflict if medication is fetched again
                medicationRepository.getMedicationById(medicationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching medication for job", e)
                null
            }

            // Fetch Medication Details if ID changed or name is missing (or medicationForJob is null)
            if (medicationId != previousMedicationId || _medicationName.value.isEmpty() || medicationForJob == null) {
                try {
                    Log.d(TAG, "ReminderObserverJob: Fetching medication details for medId: $medicationId")
                    medicationForJob = medicationRepository.getMedicationById(medicationId) // Ensure it's fetched
                    if (medicationForJob == null) {
                        Log.e(TAG, "ReminderObserverJob: Medication with ID $medicationId not found.")
                        _error.value = "Medication details not found."
                        _weeklyChartData.value = emptyList()
                        _chartyGraphData.value = emptyList()
                        _weeklyMaxYValue.value = 5f // Reset to default
                        _isLoading.value = false
                        return@launch
                    }
                    val activeDosage = dosageRepository.getActiveDosage(medicationId)
                    _medicationName.value = medicationForJob!!.name ?: "Medication $medicationId"
                    _currentDosageQuantity.value = parseDosageQuantity(activeDosage?.dosage)
                    Log.d(TAG, "ReminderObserverJob: Fetched medication: Name='${_medicationName.value}', DosageQty=${_currentDosageQuantity.value}")
                } catch (e: Exception) {
                    Log.e(TAG, "ReminderObserverJob: Error fetching medication details for medId $medicationId", e)
                    _error.value = "Failed to load medication details."
                    _weeklyChartData.value = emptyList()
                    _chartyGraphData.value = emptyList()
                    _weeklyMaxYValue.value = 5f // Reset to default
                    _isLoading.value = false
                    return@launch
                }
            }
            // Calculate Max Y value for weekly chart
            if (medicationForJob != null) {
                try {
                    val schedule = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()?.firstOrNull()
                    if (schedule != null) {
                        var maxScheduledDosesForWeek = 0
                        currentWeekDays.forEach { day ->
                            val remindersForDayMap = ReminderCalculator.generateRemindersForPeriod(
                                medication = medicationForJob!!, // medicationForJob is not null here
                                schedule = schedule,
                                periodStartDate = day,
                                periodEndDate = day
                            )
                            val dosesOnDay = remindersForDayMap[day]?.size ?: 0
                            if (dosesOnDay > maxScheduledDosesForWeek) {
                                maxScheduledDosesForWeek = dosesOnDay
                            }
                        }
                        _weeklyMaxYValue.value = maxScheduledDosesForWeek.toFloat().coerceAtLeast(5f)
                        Log.d(TAG, "Updated weeklyMaxYValue: ${_weeklyMaxYValue.value}")
                    } else {
                        _weeklyMaxYValue.value = 5f // Default if no active schedule
                        Log.d(TAG, "No active schedule found for medId $medicationId, weeklyMaxYValue reset to default.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error calculating max scheduled doses for week for medId $medicationId", e)
                    _weeklyMaxYValue.value = 5f // Default on error
                }
            } else {
                 _weeklyMaxYValue.value = 5f // Default if no medication
                 Log.w(TAG, "MedicationForJob is null before calculating weekly max Y value.")
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
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.i(TAG, "loadYearlyGraphData: Starting for medId: $medicationId, year: $targetYear")
            try {
                // Fetch medication details if not already available or if they might have changed
                var medication = medicationRepository.getMedicationById(medicationId) // Fetch fresh for yearly calculation
                if (medication == null) {
                    Log.e(TAG, "loadYearlyGraphData: Medication with ID $medicationId not found.")
                    _error.value = "Medication details not found for yearly graph."
                    _yearlyChartData.value = emptyList()
                    _yearlyMaxYValue.value = 30f // Reset to default
                    _isLoading.value = false
                    return@launch
                }
                // Update medication name if it's not set or different
                if (_medicationName.value.isEmpty() || _medicationName.value != medication.name) {
                    _medicationName.value = medication.name ?: "Medication $medicationId"
                }
                // _currentDosageQuantity could also be updated here if needed for yearly, but not directly used for graph values

                // Calculate Max Y value for yearly chart
                try {
                    val schedule = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()?.firstOrNull()
                    if (schedule != null) {
                        var maxScheduledDosesInAnyMonth = 0
                        for (monthValue in 1..12) {
                            val yearMonth = YearMonth.of(targetYear, monthValue)
                            val monthStartDate = yearMonth.atDay(1)
                            val monthEndDate = yearMonth.atEndOfMonth()

                            val remindersForMonthMap = ReminderCalculator.generateRemindersForPeriod(
                                medication = medication, // Not null here
                                schedule = schedule,
                                periodStartDate = monthStartDate,
                                periodEndDate = monthEndDate
                            )

                            var dosesInMonth = 0
                            remindersForMonthMap.values.forEach { dailyDosesList ->
                                dosesInMonth += dailyDosesList.size
                            }

                            if (dosesInMonth > maxScheduledDosesInAnyMonth) {
                                maxScheduledDosesInAnyMonth = dosesInMonth
                            }
                        }
                        _yearlyMaxYValue.value = maxScheduledDosesInAnyMonth.toFloat().coerceAtLeast(30f)
                        Log.d(TAG, "Updated yearlyMaxYValue: ${_yearlyMaxYValue.value}")
                    } else {
                        _yearlyMaxYValue.value = 30f // Default if no active schedule
                        Log.d(TAG, "No active schedule found for medId $medicationId, yearlyMaxYValue reset to default.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error calculating max scheduled doses for year for medId $medicationId", e)
                    _yearlyMaxYValue.value = 30f // Default on error
                }

                // Process taken reminders for the yearly chart
                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
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
                    val monthEnum = Month.of(month)
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