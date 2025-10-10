package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.ui.navigation.MEDICATION_ID_ARG
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditFormViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditFormState())
    val uiState: StateFlow<EditFormState> = _uiState.asStateFlow()

    private val medicationId: Int = savedStateHandle.get<Int>(MEDICATION_ID_ARG)!!
    private val colorName: String? = savedStateHandle.get<String>("colorName")

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val medication = medicationRepository.getMedicationById(medicationId)
            medication?.let {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        medicationTypeId = it.typeId,
                        medicationColor = colorName?.let { MedicationColor.valueOf(it) }
                    )
                }
            }
        }
    }

    fun onTypeSelected(typeId: Int) {
        _uiState.update { it.copy(medicationTypeId = typeId) }
    }

    fun onSave() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val medication = medicationRepository.getMedicationById(medicationId)
            medication?.let {
                val updatedMedication = it.copy(
                    typeId = currentState.medicationTypeId
                )
                medicationRepository.updateMedication(updatedMedication)
            }
        }
    }
}