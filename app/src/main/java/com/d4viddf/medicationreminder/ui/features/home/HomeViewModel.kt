package com.d4viddf.medicationreminder.ui.features.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationTypeRepository
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.domain.usecase.ReminderCalculator
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.WatchStatus
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeSection
import com.d4viddf.medicationreminder.ui.features.todayschedules.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.utils.WearConnectivityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
open class HomeViewModel @Inject constructor(
    private val application: Application,
    private val medicationReminderRepository: MedicationReminderRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationTypeRepository: MedicationTypeRepository,
    private val wearConnectivityHelper: WearConnectivityHelper,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    data class HomeState(
        val nextDoseGroup: List<NextDoseUiItem> = emptyList(),
        val todaysReminders: Map<String, List<TodayScheduleUiItem>> = emptyMap(),
        val hasRegisteredMedications: Boolean = false, // Default to false
        val nextDoseTimeRemaining: String? = null,
        val nextDoseAtTime: String? = null,
        val hasUnreadAlerts: Boolean = false,
        val isLoading: Boolean = true,
        val currentGreeting: String = "",
        val isRefreshing: Boolean = false,
        val showConfirmationDialog: Boolean = false,
        val confirmationDialogTitle: String = "",
        val confirmationDialogText: String = "",
        val confirmationAction: () -> Unit = {},
        val watchStatus: WatchStatus = WatchStatus.UNKNOWN
    )

    private val _uiState = MutableStateFlow(HomeState())
    open val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _navigationChannel = Channel<String>()
    val navigationEvents = _navigationChannel.receiveAsFlow()

    init {
        loadTodaysSchedule()
        updateGreeting()
        updateWatchStatus()
    }

    val homeLayout: StateFlow<List<HomeSection>> = userPreferencesRepository.homeLayoutFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayProgressTaken: Int = 9
    val todayProgressTotal: Int = 12
    val nextDoseTimeInSeconds: Long? = 930L
    val heartRate: String? = "46-97"
    val weight: String? = "80 kg"
    val missedDoses: Int = 2
    val lastMissedMedication: String? = "Ibuprofen"


    fun handleWatchIconClick() {
        viewModelScope.launch {
            Log.i("HomeViewModel", "Watch icon clicked. Navigating to Connected Devices screen.")
            _navigationChannel.send(Screen.ConnectedDevices.route)
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            updateGreeting()
            loadTodaysSchedule()
            updateWatchStatus()
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

            // **1. Check if there are any medications in the database.**
            val allMedications = medicationRepository.getAllMedications().first()
            val hasMeds = allMedications.isNotEmpty()

            val today = LocalDate.now()
            val startOfDayString = today.atStartOfDay().format(ReminderCalculator.storableDateTimeFormatter)
            val endOfDayString = today.atTime(LocalTime.MAX).format(ReminderCalculator.storableDateTimeFormatter)

            medicationReminderRepository.getRemindersForDay(startOfDayString, endOfDayString)
                .collect { remindersList ->
                    val currentTimeMillis = System.currentTimeMillis()

                    val upcomingReminders = remindersList.filter { reminder ->
                        try {
                            val reminderTime = LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                            reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() > currentTimeMillis && !reminder.isTaken
                        } catch (e: Exception) {
                            false
                        }
                    }

                    val nextGroupTimeMillis = upcomingReminders.minOfOrNull { reminder ->
                        try {
                            LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } catch (e: Exception) {
                            Long.MAX_VALUE
                        }
                    }

                    val nextDoseReminders = if (nextGroupTimeMillis != null && nextGroupTimeMillis != Long.MAX_VALUE) {
                        upcomingReminders.filter { reminder ->
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

                    val nextDoseUiItemsDeferred = nextDoseReminders.map { reminder ->
                        async {
                            val medication = medicationRepository.getMedicationById(reminder.medicationId)
                            val medicationTypeDetails = medication?.typeId?.let { medicationTypeRepository.getMedicationTypeById(it) }

                            NextDoseUiItem(
                                reminderId = reminder.id,
                                medicationId = reminder.medicationId,
                                medicationName = medication?.name ?: "Unknown",
                                medicationDosage = medication?.dosage ?: "N/A",
                                medicationColorName = medication?.color ?: "LIGHT_ORANGE",
                                medicationImageUrl = medicationTypeDetails?.imageUrl,
                                rawReminderTime = reminder.reminderTime,
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
                    val nextDoseUiItems = nextDoseUiItemsDeferred.awaitAll().sortedBy { it.rawReminderTime }

                    val processedTodaysRemindersMapAsync = remindersList
                        .groupBy { getPartOfDay(it.reminderTime) }
                        .mapValues { (_, remindersInPart) ->
                            remindersInPart.map { reminder ->
                                async {
                                    val medication = medicationRepository.getMedicationById(reminder.medicationId)
                                    val typeDetails = medication?.typeId?.let { medicationTypeRepository.getMedicationTypeById(it) }
                                    TodayScheduleUiItem(
                                        reminder = reminder,
                                        medicationName = medication?.name ?: "Unknown",
                                        medicationDosage = medication?.dosage ?: "N/A",
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
                        }

                    val allPartsOfDay = listOf("Morning", "Afternoon", "Evening", "Night")
                    val finalTodaysReminders = mutableMapOf<String, List<TodayScheduleUiItem>>()
                    allPartsOfDay.forEach { part ->
                        finalTodaysReminders[part] = processedTodaysRemindersMapAsync[part]?.awaitAll()?.sortedBy { it.reminder.reminderTime } ?: emptyList()
                    }

                    val (timeRemainingString, nextDoseAtTimeString) = if (nextDoseUiItems.isNotEmpty()) {
                        val nextDoseItem = nextDoseUiItems.first()
                        try {
                            val reminderDateTime = LocalDateTime.parse(nextDoseItem.rawReminderTime, ReminderCalculator.storableDateTimeFormatter)
                            val now = LocalDateTime.now()
                            if (reminderDateTime.isAfter(now)) {
                                val duration = Duration.between(now, reminderDateTime)
                                formatDuration(duration) to nextDoseItem.formattedReminderTime
                            } else {
                                null to nextDoseItem.formattedReminderTime
                            }
                        } catch (e: Exception) {
                            null to nextDoseItem.formattedReminderTime
                        }
                    } else {
                        null to null
                    }

                    _uiState.value = _uiState.value.copy(
                        nextDoseGroup = nextDoseUiItems,
                        todaysReminders = finalTodaysReminders,
                        nextDoseTimeRemaining = timeRemainingString,
                        nextDoseAtTime = nextDoseAtTimeString,
                        hasRegisteredMedications = hasMeds, // **2. Update the state here**
                        isLoading = false,
                        isRefreshing = false
                    )
                }
        }
    }

    private fun formatDuration(duration: Duration): String {
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
                in 0..11 -> "Morning"
                in 12..16 -> "Afternoon"
                in 17..20 -> "Evening"
                else -> "Night"
            }
        } catch (e: Exception) {
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