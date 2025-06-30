package com.d4viddf.medicationreminder.ui.features.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.MedicationTypeRepository // Added
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject
// Removed: import com.d4viddf.medicationreminder.ui.common.utils.MedicationVisualsUtil


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val medicationReminderRepository: MedicationReminderRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationTypeRepository: MedicationTypeRepository // Injected
) : ViewModel() {

    data class HomeState(
        val nextDoseGroup: List<NextDoseUiItem> = emptyList(), // Changed to NextDoseUiItem
        val todaysReminders: Map<String, List<MedicationReminder>> = emptyMap(),
        val hasUnreadAlerts: Boolean = false,
        val isLoading: Boolean = true,
        val currentGreeting: String = "Good day!"
    )

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    init {
        loadTodaysSchedule()
        updateGreeting()
        // TODO: Load alert status here
    }

    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good morning! 🌤️"
            in 12..17 -> "Good afternoon! ☀️"
            else -> "Good evening! 🌙"
        }
        _uiState.value = _uiState.value.copy(currentGreeting = greeting)
    }

    private fun loadTodaysSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val today = LocalDate.now()
            val startOfDayString = today.atStartOfDay().format(ReminderCalculator.storableDateTimeFormatter)
            val endOfDayString = today.atTime(LocalTime.MAX).format(ReminderCalculator.storableDateTimeFormatter)

            medicationReminderRepository.getRemindersForDay(startOfDayString, endOfDayString)
                .collect { remindersList: List<MedicationReminder> ->
                    val currentTimeMillis = System.currentTimeMillis()

                    // Find all upcoming reminders that haven't been taken yet
                    val upcomingReminders = remindersList.filter { reminder: MedicationReminder ->
                        try {
                            val reminderTime = LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                            val reminderMillis = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            reminderMillis > currentTimeMillis && !reminder.isTaken
                        } catch (e: Exception) {
                            // Log error or handle parsing exception
                            false
                        }
                    }

                    // Group them by their scheduled time to find the next group
                    val nextGroupTimeMillis = upcomingReminders.minOfOrNull { reminder: MedicationReminder ->
                        try {
                            LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } catch (e: Exception) {
                            Long.MAX_VALUE
                        }
                    }

                    val nextDoseReminders = if (nextGroupTimeMillis != null && nextGroupTimeMillis != Long.MAX_VALUE) {
                        upcomingReminders.filter { reminder: MedicationReminder ->
                            try {
                                LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() == nextGroupTimeMillis
                            } catch (e: Exception) {
                                false
                            }
                        }
                    } else {
                        emptyList()
                    }

                    // Fetch medication details for the nextDoseGroup
                    val nextDoseUiItemsDeferred = nextDoseReminders.map { reminder ->
                        viewModelScope.async {
                            val medication = medicationRepository.getMedicationById(reminder.medicationId)
                            // Assuming medication type is stored in Medication.type or similar
                            // and you have a way to get MedicationType details if needed for the name.
                            // For now, let's assume medication.type (if it exists) is a string like "PILL".
                            // If medication.typeId is used, you'd fetch MedicationType by ID.
                            val medication = medicationRepository.getMedicationById(reminder.medicationId)
                            var medicationTypeName: String? = null
                            var medicationImageUrl: String? = null

                            if (medication?.typeId != null) {
                                val medicationTypeDetails = medicationTypeRepository.getMedicationTypeById(medication.typeId)
                                medicationTypeName = medicationTypeDetails?.name // For potential future use if name is needed
                                medicationImageUrl = medicationTypeDetails?.imageUrl
                            }

                            NextDoseUiItem(
                                reminderId = reminder.id,
                                medicationId = reminder.medicationId,
                                medicationName = medication?.name ?: "Unknown Medication",
                                medicationDosage = medication?.dosage ?: "N/A",
                                medicationColorName = medication?.color ?: "LIGHT_ORANGE", // Changed default
                                medicationImageUrl = medicationImageUrl,
                                rawReminderTime = reminder.reminderTime, // Added raw time
                                formattedReminderTime = try {
                                    LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                                } catch (e: Exception) { "N/A" }
                            )
                        }
                    }
                    val nextDoseUiItems = nextDoseUiItemsDeferred.awaitAll().sortedBy { it.rawReminderTime } // Sort by raw time


                    val groupedReminders = remindersList.groupBy { reminder: MedicationReminder -> getPartOfDay(reminder.reminderTime) }
                        .mapValues { entry: Map.Entry<String, List<MedicationReminder>> ->
                            entry.value.sortedBy { medicationReminder: MedicationReminder -> medicationReminder.reminderTime }
                        }

                    val allPartsOfDay = listOf("Morning", "Afternoon", "Evening", "Night")
                    val sortedGroupedReminders = mutableMapOf<String, List<MedicationReminder>>()
                    allPartsOfDay.forEach { part: String ->
                        sortedGroupedReminders[part] = groupedReminders[part] ?: emptyList()
                    }

                    _uiState.value = _uiState.value.copy(
                        nextDoseGroup = nextDoseUiItems, // Assign the new list of NextDoseUiItem
                        todaysReminders = sortedGroupedReminders,
                        isLoading = false
                    )
                }
        }
    }

    private fun getPartOfDay(isoTimestamp: String): String {
        return try {
            val dateTime = LocalDateTime.parse(isoTimestamp, ReminderCalculator.storableDateTimeFormatter)
            when (dateTime.hour) {
                in 0..11 -> "Morning"  // Morning: 00:00 - 11:59
                in 12..16 -> "Afternoon" // Afternoon: 12:00 - 16:59
                in 17..20 -> "Evening"   // Evening: 17:00 - 20:59
                else -> "Night"         // Night: 21:00 - 23:59
            }
        } catch (e: Exception) {
            // Log error or handle parsing exception
            "Unknown"
        }
    }

    fun markAsTaken(reminder: MedicationReminder) {
        viewModelScope.launch {
            val nowString = LocalDateTime.now().format(ReminderCalculator.storableDateTimeFormatter)
            medicationReminderRepository.updateReminder(reminder.copy(isTaken = true, takenAt = nowString))
            // No need to manually reload; the Flow from getRemindersForDay should automatically emit the new list.
        }
    }
}
