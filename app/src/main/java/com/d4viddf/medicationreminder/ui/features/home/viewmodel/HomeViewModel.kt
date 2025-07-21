package com.d4viddf.medicationreminder.ui.features.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.MedicationReminder
import android.app.Application // Added for context
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.WatchStatus // Added WatchStatus
import com.d4viddf.medicationreminder.utils.WearConnectivityHelper // Added WearConnectivityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext // Added for Hilt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.Intent // For launching intents
import android.provider.Settings // For Bluetooth settings
import android.util.Log // For logging
import com.d4viddf.medicationreminder.repository.MedicationTypeRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject
// Removed: import com.d4viddf.medicationreminder.ui.common.utils.MedicationVisualsUtil


@HiltViewModel
open class HomeViewModel @Inject constructor(
    private val application: Application, // Injected Application context
    private val medicationReminderRepository: MedicationReminderRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationTypeRepository: MedicationTypeRepository, // Injected
    private val wearConnectivityHelper: WearConnectivityHelper // Injected WearConnectivityHelper
) : ViewModel() {

    data class HomeState(
        val nextDoseGroup: List<NextDoseUiItem> = emptyList(), // Changed to NextDoseUiItem
        val todaysReminders: Map<String, List<TodayScheduleUiItem>> = emptyMap(), // Changed to TodayScheduleUiItem
        val nextDoseTimeRemaining: String? = null, // Added for time remaining
        val nextDoseAtTime: String? = null, // Added for the time of the next dose
        val hasUnreadAlerts: Boolean = false,
        val isLoading: Boolean = true,
        val currentGreeting: String = "", // Initialize with empty or default from strings
        val isRefreshing: Boolean = false,
        val showConfirmationDialog: Boolean = false,
        val confirmationDialogTitle: String = "",
        val confirmationDialogText: String = "",
        val confirmationAction: () -> Unit = {},
        val watchStatus: WatchStatus = WatchStatus.UNKNOWN // Added watch status
    )

    private val _uiState = MutableStateFlow(HomeState())
    open val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    // Channel for navigation events
    private val _navigationChannel = Channel<String>()
    val navigationEvents = _navigationChannel.receiveAsFlow()

    init {
        loadTodaysSchedule()
        updateGreeting()
        updateWatchStatus() // Call new function
        // TODO: Load alert status here
    }

    fun handleWatchIconClick() {
        viewModelScope.launch {
            Log.i("HomeViewModel", "Watch icon clicked. Navigating to Connected Devices screen.")
            _navigationChannel.send(com.d4viddf.medicationreminder.ui.navigation.Screen.ConnectedDevices.route)
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            // Call existing loading functions
            updateGreeting() // Greetings might change based on time
            loadTodaysSchedule() // This will handle isLoading and eventually set isRefreshing to false
            updateWatchStatus() // Also update watch status on refresh
        }
    }

    private fun updateWatchStatus() {
        viewModelScope.launch {
            val isConnected = wearConnectivityHelper.isWatchConnected()
            if (!isConnected) {
                _uiState.value = _uiState.value.copy(watchStatus = WatchStatus.NOT_CONNECTED)
                return@launch
            }
            val isAppInstalled = wearConnectivityHelper.isWatchAppInstalled()
            _uiState.value = _uiState.value.copy(
                watchStatus = if (isAppInstalled) WatchStatus.CONNECTED_APP_INSTALLED else WatchStatus.CONNECTED_APP_NOT_INSTALLED
            )
        }
    }

    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greetingResId = when (hour) {
            in 0..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
        _uiState.value = _uiState.value.copy(currentGreeting = application.getString(greetingResId))
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

                    // Process todaysReminders to include full medication details
                    val processedTodaysRemindersMapAsync = remindersList
                        .groupBy { reminder -> getPartOfDay(reminder.reminderTime) }
                        .mapValues { (_, remindersInPart) -> // For each part of day
                            remindersInPart.map { reminder -> // For each reminder in that part
                                viewModelScope.async { // Fetch details asynchronously
                                    val medication = medicationRepository.getMedicationById(reminder.medicationId)
                                    var medicationTypeName: String? = null
                                    var medicationIconUrl: String? = null
                                    if (medication?.typeId != null) {
                                        val typeDetails = medicationTypeRepository.getMedicationTypeById(medication.typeId)
                                        medicationTypeName = typeDetails?.name
                                        medicationIconUrl = typeDetails?.imageUrl
                                    }
                                    TodayScheduleUiItem(
                                        reminder = reminder, // Keep original reminder
                                        medicationName = medication?.name ?: application.getString(R.string.info_not_available),
                                        medicationDosage = medication?.dosage ?: application.getString(R.string.info_not_available_short),
                                        medicationColorName = medication?.color ?: "LIGHT_ORANGE", // Default color
                                        medicationIconUrl = medicationIconUrl,
                                        medicationTypeName = medicationTypeName,
                                        formattedReminderTime = try {
                                            LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                                        } catch (e: Exception) { application.getString(R.string.info_not_available_short) }
                                    )
                                }
                            }
                        }

                    val allPartsOfDay = listOf("Morning", "Afternoon", "Evening", "Night")
                    val finalTodaysReminders = mutableMapOf<String, List<TodayScheduleUiItem>>()
                    allPartsOfDay.forEach { part ->
                        val itemsInPartDeferred = processedTodaysRemindersMapAsync[part]
                        if (itemsInPartDeferred != null) {
                            finalTodaysReminders[part] = itemsInPartDeferred.awaitAll().sortedBy { it.reminder.reminderTime }
                        } else {
                            finalTodaysReminders[part] = emptyList()
                        }
                    }

                    // Calculate time remaining for the very next dose
                    val (timeRemainingString, nextDoseAtTimeString) = if (nextDoseUiItems.isNotEmpty()) {
                        val nextDoseItem = nextDoseUiItems.first()
                        try {
                            val reminderDateTime = LocalDateTime.parse(nextDoseItem.rawReminderTime, ReminderCalculator.storableDateTimeFormatter)
                            val now = LocalDateTime.now()
                            if (reminderDateTime.isAfter(now)) {
                                val duration = java.time.Duration.between(now, reminderDateTime)
                                formatDuration(duration) to nextDoseItem.formattedReminderTime
                            } else {
                                // This case should ideally not happen if logic is correct (nextDoseGroup is for future doses)
                                null to nextDoseItem.formattedReminderTime // Or handle as overdue
                            }
                        } catch (e: Exception) {
                            // Log error
                            null to nextDoseItem.formattedReminderTime // Or handle error string
                        }
                    } else {
                        // Potentially look for the next dose tomorrow if none today - for future enhancement
                        // For now, if nextDoseGroup is empty, no specific time remaining shown here for today's next dose
                        null to null
                    }

                    _uiState.value = _uiState.value.copy(
                        nextDoseGroup = nextDoseUiItems,
                        todaysReminders = finalTodaysReminders, // Use the processed list
                        nextDoseTimeRemaining = timeRemainingString,
                        nextDoseAtTime = nextDoseAtTimeString,
                        isLoading = false,
                        isRefreshing = false // Ensure refreshing is also stopped
                    )
                }
        }
    }

    private fun formatDuration(duration: java.time.Duration): String {
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60

        return when {
            days > 0 -> application.getString(R.string.time_remaining_days_hours, days, hours)
            hours > 0 -> application.getString(R.string.time_remaining_hours_minutes, hours, minutes)
            minutes > 0 -> application.getString(R.string.time_remaining_minutes, minutes)
            else -> application.getString(R.string.time_remaining_less_than_minute)
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

    open fun markAsTaken(reminder: MedicationReminder) {
        _uiState.value = _uiState.value.copy(
            showConfirmationDialog = true,
            confirmationDialogTitle = "Mark as Taken",
            confirmationDialogText = "Are you sure you want to mark this dose as taken?",
            confirmationAction = {
                viewModelScope.launch {
                    val nowString = LocalDateTime.now().format(ReminderCalculator.storableDateTimeFormatter)
                    medicationReminderRepository.updateReminder(reminder.copy(isTaken = true, takenAt = nowString))
                    _uiState.value = _uiState.value.copy(showConfirmationDialog = false)
                }
            }
        )
    }

    open fun skipDose(reminder: MedicationReminder) {
        _uiState.value = _uiState.value.copy(
            showConfirmationDialog = true,
            confirmationDialogTitle = "Skip Dose",
            confirmationDialogText = "Are you sure you want to skip this dose?",
            confirmationAction = {
                viewModelScope.launch {
                    medicationReminderRepository.deleteReminder(reminder)
                    _uiState.value = _uiState.value.copy(showConfirmationDialog = false)
                }
            }
        )
    }

    fun dismissConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showConfirmationDialog = false)
    }

    fun isFutureDose(reminderTime: String): Boolean {
        return try {
            val reminderDateTime = LocalDateTime.parse(reminderTime, ReminderCalculator.storableDateTimeFormatter)
            reminderDateTime.isAfter(LocalDateTime.now())
        } catch (e: Exception) {
            false
        }
    }
}
