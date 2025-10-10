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
class StockReminderViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockReminderState())
    val uiState: StateFlow<StockReminderState> = _uiState.asStateFlow()

    val medicationId: Int = savedStateHandle.get<Int>(MEDICATION_ID_ARG)!!

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val medication = medicationRepository.getMedicationById(medicationId)
            medication?.let {
                _uiState.update { currentState ->
                    currentState.copy(
                        lowStockReminderValue = it.lowStockReminderDays?.toString() ?: "None"
                    )
                }
            }
        }
    }

    fun saveLowStockReminder(days: Int?) {
        viewModelScope.launch {
            val medication = medicationRepository.getMedicationById(medicationId)
            medication?.let {
                val updatedMedication = it.copy(lowStockReminderDays = days)
                medicationRepository.updateMedication(updatedMedication)
                _uiState.update { currentState ->
                    currentState.copy(
                        lowStockReminderValue = days?.toString() ?: "None"
                    )
                }
            }
        }
    }
}