package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.ui.navigation.MEDICATION_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmptyStockReminderViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmptyStockReminderState())
    val uiState: StateFlow<EmptyStockReminderState> = _uiState.asStateFlow()

    private val medicationId: Int = savedStateHandle.get<Int>(MEDICATION_ID_ARG)!!

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val medication = medicationRepository.getMedicationById(medicationId)
            if (medication != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        medicationName = medication.name,
                        selectedDays = medication.emptyStockReminderDays ?: it.selectedDays
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onDaysChanged(days: Int) {
        _uiState.update { it.copy(selectedDays = days) }
    }

    fun onSave() {
        viewModelScope.launch {
            val medication = medicationRepository.getMedicationById(medicationId)
            medication?.let {
                val updatedMedication = it.copy(emptyStockReminderDays = _uiState.value.selectedDays)
                medicationRepository.updateMedication(updatedMedication)
            }
        }
    }

    fun onDisable() {
        viewModelScope.launch {
            val medication = medicationRepository.getMedicationById(medicationId)
            medication?.let {
                val updatedMedication = it.copy(emptyStockReminderDays = null)
                medicationRepository.updateMedication(updatedMedication)
            }
        }
    }
}
