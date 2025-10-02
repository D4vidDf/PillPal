package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.data.model.ScheduleType
import com.d4viddf.medicationreminder.data.repository.MedicationDosageRepository
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationScheduleRepository
import com.d4viddf.medicationreminder.ui.navigation.MEDICATION_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class LowStockReminderViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val dosageRepository: MedicationDosageRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(LowStockReminderState())
    val uiState: StateFlow<LowStockReminderState> = _uiState.asStateFlow()

    private val medicationId: Int = savedStateHandle.get<Int>(MEDICATION_ID_ARG)!!

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val medication = medicationRepository.getMedicationById(medicationId)
            val schedule = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()?.firstOrNull()
            val dosage = dosageRepository.getActiveDosage(medicationId)

            if (medication != null && schedule != null && dosage != null) {
                val dosesPerDay = calculateDosesPerDay(schedule, dosage.dosage)
                val runsOutInDays = if (dosesPerDay > 0) {
                    ceil(medication.remainingDoses / dosesPerDay).toInt()
                } else {
                    null
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        medicationName = medication.name,
                        runsOutInDays = runsOutInDays,
                        selectedDays = medication.lowStockReminderDays ?: it.selectedDays
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun calculateDosesPerDay(schedule: MedicationSchedule, dosageString: String): Double {
        val dosageAmount = dosageString.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 1.0
        return when (schedule.scheduleType) {
            ScheduleType.DAILY, ScheduleType.CUSTOM_ALARMS -> (schedule.specificTimes?.size ?: 0) * dosageAmount
            ScheduleType.WEEKLY -> ((schedule.specificTimes?.size ?: 0) * (schedule.daysOfWeek?.size ?: 0) / 7.0) * dosageAmount
            ScheduleType.INTERVAL -> {
                val startTime = schedule.intervalStartTime?.let { java.time.LocalTime.parse(it) } ?: return 0.0
                val endTime = schedule.intervalEndTime?.let { java.time.LocalTime.parse(it) } ?: return 0.0
                val intervalHours = schedule.intervalHours ?: 0
                val intervalMinutes = schedule.intervalMinutes ?: 0
                val totalIntervalMinutes = intervalHours * 60 + intervalMinutes
                if (totalIntervalMinutes == 0) return 0.0

                val durationMinutes = Duration.between(startTime, endTime).toMinutes()
                (durationMinutes / totalIntervalMinutes.toDouble()) * dosageAmount
            }
            ScheduleType.AS_NEEDED -> 0.0
        }
    }

    fun onDaysChanged(days: Int) {
        _uiState.update { it.copy(selectedDays = days) }
    }

    fun onSave() {
        viewModelScope.launch {
            val medication = medicationRepository.getMedicationById(medicationId)
            medication?.let {
                val updatedMedication = it.copy(lowStockReminderDays = _uiState.value.selectedDays)
                medicationRepository.updateMedication(updatedMedication)
            }
        }
    }

    fun onDisable() {
        viewModelScope.launch {
            val medication = medicationRepository.getMedicationById(medicationId)
            medication?.let {
                val updatedMedication = it.copy(lowStockReminderDays = null)
                medicationRepository.updateMedication(updatedMedication)
            }
        }
    }
}