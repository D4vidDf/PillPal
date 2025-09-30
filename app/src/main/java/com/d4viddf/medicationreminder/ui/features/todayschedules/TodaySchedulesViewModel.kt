package com.d4viddf.medicationreminder.ui.features.todayschedules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.data.repository.MedicationDosageRepository
import com.d4viddf.medicationreminder.data.repository.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationTypeRepository
import com.d4viddf.medicationreminder.domain.usecase.ReminderCalculator
import com.d4viddf.medicationreminder.ui.features.todayschedules.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.navigation.SHOW_MISSED_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TodaySchedulesViewModel @Inject constructor(
    private val medicationReminderRepository: MedicationReminderRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationTypeRepository: MedicationTypeRepository,
    private val dosageRepository: MedicationDosageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- State & Configuration ---

    // Determines the screen's mode: showing missed doses or today's schedule.
    val showMissed: StateFlow<Boolean> = savedStateHandle.getStateFlow(SHOW_MISSED_ARG, false)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- State for Filter Controls ---
    private val _allMedications = MutableStateFlow<List<Medication>>(emptyList())
    val allMedications: StateFlow<List<Medication>> = _allMedications.asStateFlow()

    private val _selectedMedicationIds = MutableStateFlow<List<Int>>(emptyList())
    val selectedMedicationIds: StateFlow<List<Int>> = _selectedMedicationIds.asStateFlow()

    private val _selectedColorName = MutableStateFlow<String?>(null)
    val selectedColorName: StateFlow<String?> = _selectedColorName.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow<ClosedRange<LocalTime>?>(null)
    val selectedTimeRange: StateFlow<ClosedRange<LocalTime>?> = _selectedTimeRange.asStateFlow()


    // --- Core Reactive Data Flow ---

    // 1. Base flow: Switches between fetching today's or missed reminders based on the nav argument.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val sourceReminders: Flow<List<TodayScheduleUiItem>> = showMissed
        .flatMapLatest { isMissedMode ->
            _isLoading.value = true
            val reminderFlow = if (isMissedMode) {
                medicationReminderRepository.getMissedReminders(LocalDateTime.now().toString())
            } else {
                // Also fetch all medications for the filter dropdown when in normal mode
                viewModelScope.launch { fetchAllMedicationsForFilter() }
                medicationReminderRepository.getRemindersForDay(
                    LocalDate.now().atStartOfDay().toString(),
                    LocalDate.now().atTime(LocalTime.MAX).toString()
                )
            }
            reminderFlow.map { mapToTodayScheduleUiItem(it) }
        }

    // 2. Filtered flow: Combines the source data with all the filter states.
    //    This will re-evaluate and emit a new list whenever the data or any filter changes.
    val scheduleItems: StateFlow<Map<String, List<TodayScheduleUiItem>>> = combine(
        sourceReminders,
        selectedMedicationIds,
        selectedColorName,
        selectedTimeRange
    ) { items, medIds, color, timeRange ->
        _isLoading.value = true
        val filteredList = items.filter { item ->
            val medicationMatch = medIds.isEmpty() || item.reminder.medicationId in medIds
            val colorMatch = color == null || item.medicationColorName == color
            val timeMatch = timeRange == null ||
                    try { LocalTime.parse(item.formattedReminderTime) in timeRange }
                    catch (e: Exception) { true }
            medicationMatch && colorMatch && timeMatch
        }
        _isLoading.value = false
        filteredList.groupBy { it.formattedReminderTime }.toSortedMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    // --- User Actions & Filter Updates ---

    fun onMedicationFilterChanged(medicationIds: List<Int>) {
        _selectedMedicationIds.value = medicationIds
    }

    fun onColorFilterChanged(colorName: String?) {
        _selectedColorName.value = colorName
    }

    fun onTimeRangeFilterChanged(startTime: LocalTime?, endTime: LocalTime?) {
        _selectedTimeRange.value = if (startTime != null && endTime != null) startTime..endTime else null
    }

    fun updateReminderStatus(reminder: MedicationReminder, isTaken: Boolean) = viewModelScope.launch {
        val updatedReminder = reminder.copy(
            isTaken = isTaken,
            takenAt = if (isTaken) LocalDateTime.now().format(ReminderCalculator.storableDateTimeFormatter) else null
        )
        if (reminder.isTaken != isTaken) {
            medicationReminderRepository.updateReminder(updatedReminder)
        }
    }


    // --- Private Helper Functions ---

    private suspend fun fetchAllMedicationsForFilter() {
        if (_allMedications.value.isEmpty()) {
            _allMedications.value = medicationRepository.getAllMedications().stateIn(viewModelScope).value
        }
    }

    private suspend fun mapToTodayScheduleUiItem(reminders: List<MedicationReminder>): List<TodayScheduleUiItem> {
        return reminders.mapNotNull { reminder ->
            medicationRepository.getMedicationById(reminder.medicationId)?.let { medication ->
                val typeDetails = medication.typeId?.let { medicationTypeRepository.getMedicationTypeById(it) }
                val activeDosage = dosageRepository.getActiveDosage(medication.id)
                TodayScheduleUiItem(
                    reminder = reminder,
                    medicationName = medication.name,
                    medicationDosage = activeDosage?.dosage ?: "",
                    medicationColorName = medication.color,
                    medicationIconUrl = typeDetails?.imageUrl,
                    medicationTypeName = typeDetails?.name,
                    formattedReminderTime = formatTime(reminder.reminderTime)
                )
            }
        }.sortedBy { it.reminder.reminderTime }
    }

    private fun formatTime(isoTimestamp: String): String {
        return try {
            LocalDateTime.parse(isoTimestamp, ReminderCalculator.storableDateTimeFormatter)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) { "N/A" }
    }
}