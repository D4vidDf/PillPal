package com.d4viddf.medicationreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications

    init {
        getAllMedications()
    }

    private fun getAllMedications() {
        viewModelScope.launch {
            medicationRepository.getAllMedications().collect {
                _medications.value = it
            }
        }
    }

    suspend fun insertMedication(medication: Medication): Int {
        // Run in the IO thread to avoid blocking the main thread
        return withContext(Dispatchers.IO) {
            medicationRepository.insertMedication(medication)
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.updateMedication(medication)
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.deleteMedication(medication)
        }
    }

    suspend fun getMedicationById(medicationId: Int): Medication? {
        return medicationRepository.getMedicationById(medicationId)
    }
}

