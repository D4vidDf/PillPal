package com.d4viddf.medicationreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.MedicationScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationScheduleViewModel @Inject constructor(
    private val scheduleRepository: MedicationScheduleRepository
) : ViewModel() {

    fun insertSchedule(schedule: MedicationSchedule) {
        viewModelScope.launch {
            scheduleRepository.insertSchedule(schedule)
        }
    }

    fun updateSchedule(schedule: MedicationSchedule) {
        viewModelScope.launch {
            scheduleRepository.updateSchedule(schedule)
        }
    }

    fun deleteSchedule(schedule: MedicationSchedule) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(schedule)
        }
    }
}
