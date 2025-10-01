package com.d4viddf.medicationreminder.ui.features.medication.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.CimaMedicationDetail
import com.d4viddf.medicationreminder.data.repository.MedicationInfoRepository
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationInfoViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val medicationInfoRepository: MedicationInfoRepository
) : ViewModel() {

    private val _medicationInfo = MutableStateFlow<CimaMedicationDetail?>(null)
    val medicationInfo: StateFlow<CimaMedicationDetail?> = _medicationInfo.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val TAG = "MedicationInfoVM"

    fun loadMedicationInfo(medicationId: Int) {
        Log.d(TAG, "loadMedicationInfo called for medicationId: $medicationId")
        _isLoading.value = true
        _error.value = null
        _medicationInfo.value = null // Clear previous info

        viewModelScope.launch {
            try {
                val medication = medicationRepository.getMedicationById(medicationId)
                if (medication != null) {
                    Log.d(TAG, "Medication found: ${medication.name}, nregistro: ${medication.nregistro}")
                    if (!medication.nregistro.isNullOrBlank()) {
                        // Ensure nregistro is not null before passing
                        val nregistroValue = medication.nregistro
                        val cimaDetails = medicationInfoRepository.getMedicationDetailsByNRegistro(nregistroValue)
                        if (cimaDetails != null) {
                            _medicationInfo.value = cimaDetails
                            Log.d(TAG, "CIMA details loaded successfully for nregistro: $nregistroValue")
                        } else {
                            _error.value = "Could not retrieve details from CIMA for nregistro: $nregistroValue."
                            Log.w(TAG, "CIMA details were null for nregistro: $nregistroValue")
                        }
                    } else {
                        _error.value = "Medication registration number (nregistro) not found for ${medication.name}."
                        Log.w(TAG, "nregistro is null or blank for medicationId: $medicationId")
                    }
                } else {
                    _error.value = "Medication with ID $medicationId not found."
                    Log.w(TAG, "Medication not found for medicationId: $medicationId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading medication info for ID $medicationId", e)
                _error.value = "An unexpected error occurred while fetching medication information."
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Finished loading medication info for ID $medicationId. isLoading: false")
            }
        }
    }
}