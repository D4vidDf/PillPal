package com.d4viddf.medicationreminder.ui.features.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.HeartRate
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.data.repository.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationTypeRepository
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.domain.usecase.ReminderCalculator
import com.d4viddf.medicationreminder.ui.features.home.model.NextDoseUiItem
import com.d4viddf.medicationreminder.ui.features.home.model.WatchStatus
import com.d4viddf.medicationreminder.ui.common.model.UiItemState
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeSection
import com.d4viddf.medicationreminder.ui.features.todayschedules.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.utils.WearConnectivityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val medicationReminderRepository: MedicationReminderRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationTypeRepository: MedicationTypeRepository,
    private val wearConnectivityHelper: WearConnectivityHelper,
    userPreferencesRepository: UserPreferencesRepository,
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    // --- UI State & Events ---

    // For showing confirmation dialogs (e.g., mark as taken, skip)
    data class DialogState(val title: String, val text: String, val onConfirm: () -> Unit)

    private val _dialogState = MutableStateFlow<DialogState?>(null)
    val dialogState: StateFlow<DialogState?> = _dialogState.asStateFlow()

    // For pull-to-refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // For navigation events
    private val _navigationChannel = Channel<String>()
    val navigationEvents: Flow<String> = _navigationChannel.receiveAsFlow()

    // --- Reactive Data Flows ---

    // Greeting message based on time of day
    val greeting: StateFlow<String> = MutableStateFlow(getGreeting()).asStateFlow()

    // Watch connection status
    val watchStatus: StateFlow<WatchStatus> = flow {
        emit(getWatchStatus())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WatchStatus.UNKNOWN)

    // Determines if any medications are registered in the app
    val hasRegisteredMedications: StateFlow<Boolean> = medicationRepository.getAllMedications()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Single source of truth for today's reminders from the database
    private val todaysRemindersFromDb: Flow<List<MedicationReminder>> =
        medicationReminderRepository.getRemindersForDay(
            LocalDate.now().atStartOfDay().toString(),
            LocalDate.now().atTime(LocalTime.MAX).toString()
        )

    // The carousel of next medications to be taken
    val nextDoseGroup: StateFlow<UiItemState<List<NextDoseUiItem>>> = todaysRemindersFromDb
        .map { reminders -> findNextDoseGroup(reminders) }
        .map { UiItemState.Success(it) as UiItemState<List<NextDoseUiItem>> }
        .onStart { emit(UiItemState.Loading) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UiItemState.Loading
        )


    // The countdown timer to the next dose
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val nextDoseTimeInSeconds: StateFlow<Long?> = nextDoseGroup.flatMapLatest { nextGroup ->
        when (nextGroup) {
            is UiItemState.Success -> {
                if (nextGroup.data.isEmpty()) {
                    flow { emit(null) }
                } else {
                    flow {
                        val reminderTime = LocalDateTime.parse(
                            nextGroup.data.first().rawReminderTime,
                            ReminderCalculator.storableDateTimeFormatter
                        )
                        while (true) {
                            val duration = Duration.between(LocalDateTime.now(), reminderTime).seconds
                            if (duration > 0) {
                                emit(duration)
                                kotlinx.coroutines.delay(1000)
                            } else {
                                emit(0L)
                                break
                            }
                        }
                    }
                }
            }

            else -> flow { emit(null) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Today's progress (e.g., 5 of 8 doses taken)
    val todayProgress: StateFlow<UiItemState<Pair<Int, Int>>> = todaysRemindersFromDb
        .map { reminders ->
            UiItemState.Success(reminders.count { it.isTaken } to reminders.size) as UiItemState<Pair<Int, Int>>
        }
        .onStart { emit(UiItemState.Loading) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiItemState.Loading)

    // Fully mapped schedule for the "Today's Schedule" section (if you use it on home)
    val todaySchedules: StateFlow<Map<String, List<TodayScheduleUiItem>>> = todaysRemindersFromDb
        .map { reminders -> groupAndMapReminders(reminders) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Missed Reminders Functionality ---
    val missedReminders: StateFlow<UiItemState<List<TodayScheduleUiItem>>> =
        medicationReminderRepository.getMissedReminders(LocalDateTime.now().toString())
            .map { reminders -> UiItemState.Success(mapToTodayScheduleUiItem(reminders)) as UiItemState<List<TodayScheduleUiItem>> }
            .onStart { emit(UiItemState.Loading) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiItemState.Loading)


    // --- Health Data & Layout ---
    val homeLayout: StateFlow<UiItemState<List<HomeSection>>> =
        userPreferencesRepository.homeLayoutFlow
            .map { UiItemState.Success(it) as UiItemState<List<HomeSection>> }
            .onStart { emit(UiItemState.Loading) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiItemState.Loading)


    val latestWeight: StateFlow<UiItemState<Weight?>> = healthDataRepository.getLatestWeight()
        .map { UiItemState.Success(it) as UiItemState<Weight?> }
        .onStart { emit(UiItemState.Loading) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiItemState.Loading)

    val latestTemperature: StateFlow<UiItemState<BodyTemperature?>> =
        healthDataRepository.getLatestBodyTemperature()
            .map { UiItemState.Success(it) as UiItemState<BodyTemperature?> }
            .onStart { emit(UiItemState.Loading) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiItemState.Loading)

    val waterIntakeToday: StateFlow<UiItemState<Pair<Double?, Boolean>>> =
        healthDataRepository.getTotalWaterIntakeSince(
            System.currentTimeMillis() - 24 * 60 * 60 * 1000
        ).map { UiItemState.Success(it) as UiItemState<Pair<Double?, Boolean>> }
            .onStart { emit(UiItemState.Loading) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiItemState.Loading)

    val heartRateRange: StateFlow<UiItemState<Pair<Long, Long>?>> =
        healthDataRepository.getHeartRateBetween(
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(),
            Instant.now()
        ).map { heartRates ->
            if (heartRates.isEmpty()) {
                UiItemState.Success(null)
            } else {
                val min = heartRates.minOf { it.beatsPerMinute }
                val max = heartRates.maxOf { it.beatsPerMinute }
                UiItemState.Success(Pair(min, max))
            }
        }.map { it as UiItemState<Pair<Long, Long>?> }
            .onStart { emit(UiItemState.Loading) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiItemState.Loading)

    val latestHeartRate: StateFlow<UiItemState<HeartRate?>> =
        healthDataRepository.getLatestHeartRate()
            .map { UiItemState.Success(it) as UiItemState<HeartRate?> }
            .onStart { emit(UiItemState.Loading) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiItemState.Loading)

    val isHealthConnectEnabled: StateFlow<Boolean> = userPreferencesRepository.showHealthConnectDataFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val waterIntakeProgress: StateFlow<Float> =
        combine(
            waterIntakeToday,
            userPreferencesRepository.waterIntakeGoalFlow
        ) { waterIntakeState, goal ->
            val intake = (waterIntakeState as? UiItemState.Success)?.data?.first ?: 0.0
            if (goal > 0) (intake / (goal / 1000.0)).toFloat() else 0f
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val weightProgress: StateFlow<Float> =
        combine(
            latestWeight,
            userPreferencesRepository.weightGoalMaxFlow
        ) { weightState, goal ->
            if (weightState is UiItemState.Success) {
                val weight = weightState.data?.weightKilograms ?: 0.0
                if (goal > 0) (weight / goal).toFloat() else 0f
            } else {
                0f
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val heartRateProgress: StateFlow<Float> =
        combine(
            latestHeartRate,
            userPreferencesRepository.heartRateGoalMaxFlow
        ) { heartRateState, goal ->
            if (heartRateState is UiItemState.Success) {
                val heartRate = heartRateState.data?.beatsPerMinute ?: 0
                if (goal > 0) (heartRate.toFloat() / goal) else 0f
            } else {
                0f
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)


    // --- User Actions ---

    fun refreshData() = viewModelScope.launch {
        _isRefreshing.value = true
        // Data flows will refresh automatically. We just need to hide the indicator.
        delay(1000) // Simulate network/db delay
        _isRefreshing.value = false
    }

    fun handleWatchIconClick() = viewModelScope.launch {
        _navigationChannel.send(Screen.ConnectedDevices.route)
    }

    fun markAsTaken(reminder: MedicationReminder) {
        _dialogState.value = DialogState(
            title = "Mark as Taken",
            text = "Are you sure you want to mark this dose as taken?",
            onConfirm = {
                viewModelScope.launch {
                    val nowString = LocalDateTime.now().format(ReminderCalculator.storableDateTimeFormatter)
                    medicationReminderRepository.updateReminder(reminder.copy(isTaken = true, takenAt = nowString))
                    dismissConfirmationDialog() // Close dialog
                }
            }
        )
    }

    fun skipDose(reminder: MedicationReminder) {
        _dialogState.value = DialogState(
            title = "Skip Dose",
            text = "Are you sure you want to skip this dose?",
            onConfirm = {
                viewModelScope.launch {
                    // Here you might want to set a 'skipped' flag instead of deleting
                    medicationReminderRepository.deleteReminder(reminder)
                    dismissConfirmationDialog()
                }
            }
        )
    }

    fun dismissConfirmationDialog() {
        _dialogState.value = null
    }

    // --- Private Helper Functions ---

    private suspend fun findNextDoseGroup(reminders: List<MedicationReminder>): List<NextDoseUiItem> {
        val upcoming = reminders.filter {
            !it.isTaken &&
                    try { LocalDateTime.parse(it.reminderTime, ReminderCalculator.storableDateTimeFormatter).isAfter(LocalDateTime.now()) }
                    catch (e: Exception) { false }
        }
        if (upcoming.isEmpty()) return emptyList()

        val nextTime = upcoming.minOf { it.reminderTime }
        return upcoming.filter { it.reminderTime == nextTime }.mapNotNull { mapToNextDoseUiItem(it) }
    }

    private suspend fun mapToNextDoseUiItem(reminder: MedicationReminder): NextDoseUiItem? {
        return medicationRepository.getMedicationById(reminder.medicationId)?.let { med ->
            val type = med.typeId?.let { medicationTypeRepository.getMedicationTypeById(it) }
            NextDoseUiItem(
                reminderId = reminder.id,
                medicationId = med.id,
                medicationName = med.name,
                medicationDosage = med.dosage!!,
                medicationColorName = med.color,
                medicationImageUrl = type?.imageUrl,
                rawReminderTime = reminder.reminderTime,
                formattedReminderTime = formatTime(reminder.reminderTime)
            )
        }
    }

    private suspend fun mapToTodayScheduleUiItem(reminders: List<MedicationReminder>): List<TodayScheduleUiItem> {
        return reminders.mapNotNull { reminder ->
            medicationRepository.getMedicationById(reminder.medicationId)?.let { med ->
                val type = med.typeId?.let { medicationTypeRepository.getMedicationTypeById(it) }
                TodayScheduleUiItem(
                    reminder = reminder,
                    medicationName = med.name,
                    medicationDosage = med.dosage!!,
                    medicationColorName = med.color,
                    medicationIconUrl = type?.imageUrl,
                    medicationTypeName = type?.name,
                    formattedReminderTime = formatTime(reminder.reminderTime)
                )
            }
        }.sortedBy { it.reminder.reminderTime }
    }

    private suspend fun groupAndMapReminders(reminders: List<MedicationReminder>): Map<String, List<TodayScheduleUiItem>> {
        val mappedItems = mapToTodayScheduleUiItem(reminders)
        return mappedItems.groupBy { getPartOfDay(it.reminder.reminderTime) }
    }

    private fun getPartOfDay(isoTimestamp: String): String {
        return try {
            val hour = LocalDateTime.parse(isoTimestamp, ReminderCalculator.storableDateTimeFormatter).hour
            when (hour) {
                in 0..11 -> "Morning"
                in 12..16 -> "Afternoon"
                in 17..20 -> "Evening"
                else -> "Night"
            }
        } catch (e: Exception) { "Unknown" }
    }

    private fun formatTime(isoTimestamp: String): String {
        return try {
            LocalDateTime.parse(isoTimestamp, ReminderCalculator.storableDateTimeFormatter)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) { "N/A" }
    }

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingResId = when (hour) {
            in 0..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
        return application.getString(greetingResId)
    }

    private suspend fun getWatchStatus(): WatchStatus {
        return if (!wearConnectivityHelper.isWatchConnected()) {
            WatchStatus.NOT_CONNECTED
        } else if (wearConnectivityHelper.isWatchAppInstalled()) {
            WatchStatus.CONNECTED_APP_INSTALLED
        } else {
            WatchStatus.CONNECTED_APP_NOT_INSTALLED
        }
    }
}