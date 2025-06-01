package com.d4viddf.medicationreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.MedicationScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    suspend fun getActiveScheduleForMedication(medicationId: Int): MedicationSchedule? {
        // Esta llamada al repositorio debe ser suspend o devolver un Flow que se colecta una vez.
        // Si scheduleRepository.getSchedulesForMedication devuelve Flow<List<MedicationSchedule>>, entonces:
        return withContext(Dispatchers.IO) { // Ejecutar en IO si es una operación de BD directa
            scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()?.firstOrNull()
        }
    }
}
