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
import java.time.format.DateTimeFormatter
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

    private val TAG = "MedicationGraphVM"

    fun loadWeeklyGraphData(medicationId: Int, currentWeekDays: List<LocalDate>) {
        if (currentWeekDays.isEmpty() || currentWeekDays.size != 7) {
            Log.w(TAG, "Invalid currentWeekDays list size: ${currentWeekDays.size}. Expected 7.")
            _graphData.value = emptyMap() // Reset or handle error appropriately
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading weekly graph data for medicationId: $medicationId, week: $currentWeekDays")

                // Fetch medication name
                val medication = medicationRepository.getMedicationById(medicationId)
                _medicationName.value = medication?.name ?: "Medication $medicationId"
                Log.d(TAG, "Medication name: ${_medicationName.value}")

                // Fetch all reminders for the medication
                val allReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                Log.d(TAG, "Total reminders fetched: ${allReminders.size}")

                val weekStartDateTime = currentWeekDays.first().atStartOfDay()
                val weekEndDateTime = currentWeekDays.last().atTime(LocalTime.MAX)
                Log.d(TAG, "Week date range: $weekStartDateTime to $weekEndDateTime")

                // Filter taken reminders within the current week
                val takenRemindersThisWeek = allReminders.filter { reminder ->
                    if (!reminder.isTaken || reminder.takenAt.isNullOrEmpty()) {
                        false
                    } else {
                        try {
                            val takenDateTime = LocalDateTime.parse(reminder.takenAt) // Assumes ISO_LOCAL_DATE_TIME
                            !takenDateTime.isBefore(weekStartDateTime) && !takenDateTime.isAfter(weekEndDateTime)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing takenAt timestamp: ${reminder.takenAt}", e)
                            false
                        }
                    }
                }
                Log.d(TAG, "Taken reminders this week: ${takenRemindersThisWeek.size}")

                // Group taken reminders by date
                val dosesByDate = takenRemindersThisWeek.groupBy {
                    try {
                        LocalDateTime.parse(it.takenAt).toLocalDate()
                    } catch (e: Exception) {
                        null // Should not happen if filtered correctly, but good for safety
                    }
                }
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
                Log.e(TAG, "Error loading weekly graph data", e)
                // Optionally update graphData to an error state or emptyMap()
                _graphData.value = emptyMap()
            }
        }
    }
}
