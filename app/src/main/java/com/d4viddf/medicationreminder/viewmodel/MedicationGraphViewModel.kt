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
    val chartyGraphData: StateFlow<List<ChartyGraphEntry>> = _chartyGraphData.asStateFlow()

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
        _chartyGraphData.value = emptyList() // Updated
        // _medicationName.value = "" // Optionally clear name too, or keep it
        Log.d(TAG, "Charty graph data cleared.")
    }

    fun loadWeeklyGraphData(medicationId: Int, currentWeekDays: List<LocalDate>) {
        Log.d(TAG, "loadWeeklyGraphData: Called with medicationId: $medicationId, currentWeekDays: $currentWeekDays")
        if (currentWeekDays.isEmpty()) {
            Log.d(TAG, "currentWeekDays is empty, clearing charty graph data for weekly view.")
            _chartyGraphData.value = emptyList() // Updated
            return
        }
         if (currentWeekDays.count() != 7) {
            Log.w(TAG, "Invalid currentWeekDays list size: ${currentWeekDays.count()}. Expected 7 or 0 to clear.")
            _chartyGraphData.value = emptyList() // Updated
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
                        // Pair the date with the parsed dosage quantity
                        Pair(takenDateTime.toLocalDate(), dosageQuantity)
                    } else {
                        null
                    }
                }
                Log.d(TAG, "loadWeeklyGraphData: Filtered to ${takenRemindersThisWeek.size} taken reminders (with dosage) within this week.")
                if (takenRemindersThisWeek.isNotEmpty()) {
                     Log.d(TAG, "loadWeeklyGraphData: First 5 taken items this week: ${takenRemindersThisWeek.take(5)}")
                }

                val dosesByDate = takenRemindersThisWeek
                    .groupBy { it.first } // Group by LocalDate
                    .mapValues { entry ->
                        // Sum the dosage quantities (it.second) for each group
                        entry.value.sumOf { pair -> pair.second.toDouble() }.toFloat()
                    }
                Log.d(TAG, "loadWeeklyGraphData: Doses summed by date: $dosesByDate")

                val weeklyDataMap = LinkedHashMap<String, Float>() // Changed to Float
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
                _chartyGraphData.value = chartEntries

            } catch (e: Exception) {
                Log.e(TAG, "loadWeeklyGraphData: Error loading weekly graph data for medId $medicationId", e)
                _error.value = "Failed to load weekly graph data."
                _chartyGraphData.value = emptyList() // Updated
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMonthlyGraphData(medicationId: Int, targetMonth: YearMonth) {
        Log.d(TAG, "loadMonthlyGraphData: Called with medicationId: $medicationId, targetMonth: $targetMonth")
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            Log.i(TAG, "loadMonthlyGraphData: Starting for medId: $medicationId, month: $targetMonth") // Enhanced log
            try {
                val medication = medicationRepository.getMedicationById(medicationId)
                Log.d(TAG, "loadMonthlyGraphData: Fetched medication: ${medication?.name ?: "N/A"} (Dosage: ${medication?.dosage ?: "N/A"})")
                if (medication?.name != _medicationName.value) { // Ensure name is updated
                    _medicationName.value = medication?.name ?: "Medication $medicationId"
                }
                val dosageQuantity = parseDosageQuantity(medication?.dosage)
                Log.d(TAG, "loadMonthlyGraphData: Parsed dosage quantity: $dosageQuantity")

                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                Log.d(TAG, "loadMonthlyGraphData: Fetched ${allReminders.size} total raw reminders. Sample: ${allReminders.take(5).map { r -> "ID: ${r.id}, Taken: ${r.isTaken}, Time: ${r.takenAt}" }}")

                val monthStartDateTime = targetMonth.atDay(1).atStartOfDay()
                val monthEndDateTime = targetMonth.atEndOfMonth().atTime(LocalTime.MAX)
                Log.d(TAG, "loadMonthlyGraphData: Calculated month date range: $monthStartDateTime to $monthEndDateTime")

                val takenRemindersThisMonth = allReminders.mapNotNull { reminder ->
                    if (!reminder.isTaken) return@mapNotNull null
                    val takenDateTime = parseTakenAt(reminder.takenAt)
                    if (takenDateTime == null) {
                        Log.w(TAG, "loadMonthlyGraphData: Reminder ID ${reminder.id} skipped (takenAt parsing failed or null). takenAt: '${reminder.takenAt}'")
                        return@mapNotNull null
                    }
                    if (takenDateTime.year == targetMonth.year && takenDateTime.month == targetMonth.month) {
                        // Pair the day of month with the parsed dosage quantity
                        Pair(takenDateTime.dayOfMonth, dosageQuantity)
                    } else {
                        null
                    }
                }
                Log.d(TAG, "loadMonthlyGraphData: Filtered to ${takenRemindersThisMonth.size} taken reminders (with dosage) in this month.")
                 if (takenRemindersThisMonth.isNotEmpty()) {
                     Log.d(TAG, "loadMonthlyGraphData: First 5 taken items this month: ${takenRemindersThisMonth.take(5)}")
                }

                val dosesByDayOfMonth = takenRemindersThisMonth
                    .groupBy { it.first } // Group by day of month (Int)
                    .mapValues { entry ->
                        // Sum the dosage quantities (it.second) for each group
                        entry.value.sumOf { pair -> pair.second.toDouble() }.toFloat()
                    }
                Log.d(TAG, "loadMonthlyGraphData: Doses summed by day of month: $dosesByDayOfMonth")

                val monthlyDataMap = LinkedHashMap<String, Float>() // Changed type
                val daysInMonth = targetMonth.lengthOfMonth()
                for (day in 1..daysInMonth) {
                    // dosesByDayOfMonth[day] will now be a Float?
                    monthlyDataMap[day.toString()] = dosesByDayOfMonth[day] ?: 0f // Use 0f
                }
                Log.d(TAG, "loadMonthlyGraphData: Constructed monthlyDataMap: $monthlyDataMap")

                val today = LocalDate.now()
                val currentDayOfMonthForHighlight = if (targetMonth.year == today.year && targetMonth.month == today.month) {
                    today.dayOfMonth.toString()
                } else {
                    null
                }

                val chartEntries = monthlyDataMap.map { (dayOfMonthStr, totalDosage) -> // totalDosage is Float
                    ChartyGraphEntry(
                        xValue = dayOfMonthStr,
                        yValue = totalDosage, // Already a Float
                        isHighlighted = dayOfMonthStr == currentDayOfMonthForHighlight
                    )
                }
                Log.d(TAG, "loadMonthlyGraphData: Final chartEntries: $chartEntries")
                _chartyGraphData.value = chartEntries
            } catch (e: Exception) {
                Log.e(TAG, "loadMonthlyGraphData: Error for medId $medicationId, month $targetMonth", e)
                _error.value = "Failed to load monthly graph data."
                _chartyGraphData.value = emptyList() // Updated
            } finally {
                _isLoading.value = false
            }
        }
    }

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
                        // Pair the Month enum with the parsed dosage quantity
                        Pair(takenDateTime.month, dosageQuantity)
                    } else {
                        null
                    }
                }
                Log.d(TAG, "loadYearlyGraphData: Filtered to ${takenRemindersThisYear.size} taken reminders (with dosage) in this year. Sample: ${takenRemindersThisYear.take(5)}")

                val dosesByMonth = takenRemindersThisYear
                    .groupBy { it.first } // Group by Month enum
                    .mapValues { entry ->
                        // Sum the dosage quantities (it.second) for each group
                        entry.value.sumOf { pair -> pair.second.toDouble() }.toFloat()
                    }
                Log.d(TAG, "loadYearlyGraphData: Doses summed by month enum: $dosesByMonth")

                val yearlyDataMap = LinkedHashMap<String, Float>() // Changed type
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
                _chartyGraphData.value = chartEntries
            } catch (e: Exception) {
                Log.e(TAG, "loadYearlyGraphData: Error for medId $medicationId, year $targetYear", e)
                _error.value = "Failed to load yearly graph data."
                _chartyGraphData.value = emptyList() // Updated
            } finally {
                _isLoading.value = false

            }
        }
    }
}
