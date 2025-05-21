package com.d4viddf.medicationreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationRepository
import com.d4viddf.medicationreminder.ui.components.ProgressDetails // Asegúrate que esta importación esté
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _medicationProgressDetails = MutableStateFlow<ProgressDetails?>(null)
    val medicationProgressDetails: StateFlow<ProgressDetails?> = _medicationProgressDetails.asStateFlow()

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
        return withContext(Dispatchers.IO) {
            medicationRepository.insertMedication(medication)
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
            medicationRepository.updateMedication(medication)
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch(Dispatchers.IO) {
            medicationRepository.deleteMedication(medication)
        }
    }

    suspend fun getMedicationById(medicationId: Int): Medication? {
        return withContext(Dispatchers.IO) {
            medicationRepository.getMedicationById(medicationId)
        }
    }

    fun calculateAndSetProgressDetails(medication: Medication?) {
        if (medication == null) {
            _medicationProgressDetails.value = null
            return
        }

        if (medication.packageSize > 0) {
            val totalFromPackage = medication.packageSize
            val remaining = medication.remainingDoses.coerceAtLeast(0).coerceAtMost(totalFromPackage)
            val taken = totalFromPackage - remaining // Dosis tomadas

            val progressFraction = if (totalFromPackage > 0) {
                taken.toFloat() / totalFromPackage.toFloat()
            } else {
                0f
            }

            _medicationProgressDetails.value = ProgressDetails(
                taken = taken,
                remaining = remaining,
                totalFromPackage = totalFromPackage,
                progressFraction = progressFraction.coerceIn(0f, 1f),
                // Cambiado el formato a "Restantes / Total del Paquete"
                displayText = "$taken / $totalFromPackage"
            )
        } else {
            _medicationProgressDetails.value = ProgressDetails(
                taken = 0,
                // Si no hay packageSize, remainingDoses podría no tener un contexto claro de "total".
                // Podrías mostrar medication.remainingDoses si ese campo se sigue actualizando.
                // Por simplicidad y para evitar confusión, si no hay paquete, mostramos N/A.
                remaining = 0,
                totalFromPackage = 0,
                progressFraction = 0f,
                displayText = "N/A"
            )
        }
    }
}