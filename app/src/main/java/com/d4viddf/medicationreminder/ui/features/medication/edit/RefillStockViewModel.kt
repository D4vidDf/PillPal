package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationTypeRepository
import com.d4viddf.medicationreminder.ui.navigation.MEDICATION_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RefillStockViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val medicationTypeRepository: MedicationTypeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RefillStockState())
    val uiState: StateFlow<RefillStockState> = _uiState.asStateFlow()

    private val medicationId: Int = savedStateHandle.get<Int>(MEDICATION_ID_ARG)!!

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val medication = medicationRepository.getMedicationById(medicationId)
            if (medication != null) {
                val medicationType = medication.typeId?.let {
                    medicationTypeRepository.getMedicationTypeById(it)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStock = medication.remainingDoses,
                        medicationUnit = medicationType?.name?.lowercase() ?: "doses"
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onAmountToAddChanged(amount: String) {
        if (amount.all { it.isDigit() }) {
            val newAmount = amount.toIntOrNull()
            if (newAmount != null && newAmount >= 0) {
                _uiState.update { it.copy(amountToAdd = amount) }
            } else if (amount.isEmpty()) {
                _uiState.update { it.copy(amountToAdd = "") }
            }
        }
    }

    fun onSave() {
        viewModelScope.launch {
            val amountToAdd = _uiState.value.amountToAdd.toIntOrNull() ?: 0
            if (amountToAdd > 0) {
                val medication = medicationRepository.getMedicationById(medicationId)
                medication?.let {
                    val newStock = it.remainingDoses + amountToAdd
                    val updatedMedication = it.copy(remainingDoses = newStock)
                    medicationRepository.updateMedication(updatedMedication)
                }
            }
        }
    }
}