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

    private val _medicationName = MutableStateFlow<String>("")
    val medicationName: StateFlow<String> = _medicationName.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false) // Added isLoading StateFlow
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null) // Added error StateFlow
    val error: StateFlow<String?> = _error.asStateFlow()

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
        Log.d(TAG, "loadWeeklyGraphData: Called with medicationId: $medicationId, currentWeekDays: $currentWeekDays")
        if (currentWeekDays.isEmpty()) {
            Log.d(TAG, "currentWeekDays is empty, clearing weekly chart data.")
            _weeklyChartData.value = emptyList()
            _chartyGraphData.value = emptyList() // Also clear the general one
            return
        }
         if (currentWeekDays.count() != 7) {
            Log.w(TAG, "Invalid currentWeekDays list size: ${currentWeekDays.count()}. Expected 7 or 0 to clear.")
            _weeklyChartData.value = emptyList()
            _chartyGraphData.value = emptyList() // Also clear the general one
            return
        }


        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            Log.i(TAG, "loadWeeklyGraphData: Starting for medId: $medicationId, week: $currentWeekDays") // Enhanced log
            try {
                val medication = medicationRepository.getMedicationById(medicationId)
                Log.d(TAG, "loadWeeklyGraphData: Fetched medication: ${medication?.name ?: "N/A"} (Dosage: ${medication?.dosage ?: "N/A"})")
                // _medicationName.value = medication?.name ?: ... (already exists)
                if (medication?.name != _medicationName.value) { // Ensure name is updated if it changed
                    _medicationName.value = medication?.name ?: "Medication $medicationId"
                }
                val dosageQuantity = parseDosageQuantity(medication?.dosage)
                Log.d(TAG, "loadWeeklyGraphData: Parsed dosage quantity: $dosageQuantity")

                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                Log.d(TAG, "loadWeeklyGraphData: Fetched ${allReminders.size} total raw reminders. Sample: ${allReminders.take(5).map { r -> "ID: ${r.id}, Taken: ${r.isTaken}, Time: ${r.takenAt}" }}")

                val weekStartDateTime = currentWeekDays.first().atStartOfDay()
                val weekEndDateTime = currentWeekDays.last().atTime(LocalTime.MAX)
                Log.d(TAG, "loadWeeklyGraphData: Calculated week date range: $weekStartDateTime to $weekEndDateTime")

                val takenRemindersThisWeek = allReminders.mapNotNull { reminder ->
                    if (!reminder.isTaken) {
                        return@mapNotNull null
                    }
                    val takenDateTime = parseTakenAt(reminder.takenAt)
                    if (takenDateTime == null) {
                        Log.w(TAG, "loadWeeklyGraphData: Reminder ID ${reminder.id} skipped (takenAt parsing failed or null). takenAt: '${reminder.takenAt}'")
                        return@mapNotNull null
                    }
                    if (!takenDateTime.isBefore(weekStartDateTime) && !takenDateTime.isAfter(weekEndDateTime)) {
                        takenDateTime.toLocalDate() // Collect only the date for counting events
                    } else {
                        null
                    }
                }
                Log.d(TAG, "loadWeeklyGraphData: Filtered to ${takenRemindersThisWeek.size} taken reminder events within this week.")
                if (takenRemindersThisWeek.isNotEmpty()) {
                     Log.d(TAG, "loadWeeklyGraphData: First 5 taken dates this week: ${takenRemindersThisWeek.take(5)}")
                }

                val dosesByDate = takenRemindersThisWeek
                    .groupBy { it } // Group by LocalDate directly
                    .mapValues { entry ->
                        // Count the number of events for each date
                        entry.value.size.toFloat()
                    }
                Log.d(TAG, "loadWeeklyGraphData: Event counts by date: $dosesByDate")

                val weeklyDataMap = LinkedHashMap<String, Float>()
                val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

                currentWeekDays.forEach { day ->
                    val dayName = day.format(dayFormatter)
                    weeklyDataMap[dayName] = dosesByDate[day] ?: 0f // Ensure this handles Float
                }
                Log.d(TAG, "loadWeeklyGraphData: Constructed weeklyDataMap: $weeklyDataMap")

                val today = LocalDate.now()
                val todayShortName = today.format(dayFormatter)

                val chartEntries = weeklyDataMap.map { (dayName, totalDosage) -> // totalDosage is now Float
                    ChartyGraphEntry(
                        xValue = dayName,
                        yValue = totalDosage, // Already a Float
                        isHighlighted = dayName.equals(todayShortName, ignoreCase = true)
                    )
                }
                Log.d(TAG, "loadWeeklyGraphData: Final chartEntries: $chartEntries")
                _weeklyChartData.value = chartEntries
                _chartyGraphData.value = chartEntries // For compatibility

            } catch (e: Exception) {
                Log.e(TAG, "loadWeeklyGraphData: Error loading weekly graph data for medId $medicationId", e)
                _error.value = "Failed to load weekly graph data."
                _weeklyChartData.value = emptyList()
                _chartyGraphData.value = emptyList()
            } finally {
                _isLoading.value = false
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
                val medication = medicationRepository.getMedicationById(medicationId)
                Log.d(TAG, "loadYearlyGraphData: Fetched medication: ${medication?.name ?: "N/A"} (Dosage: ${medication?.dosage ?: "N/A"})")
                if (medication?.name != _medicationName.value) { // Ensure name is updated
                     _medicationName.value = medication?.name ?: "Medication $medicationId"
                }
                val dosageQuantity = parseDosageQuantity(medication?.dosage)
                Log.d(TAG, "loadYearlyGraphData: Parsed dosage quantity: $dosageQuantity")

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
