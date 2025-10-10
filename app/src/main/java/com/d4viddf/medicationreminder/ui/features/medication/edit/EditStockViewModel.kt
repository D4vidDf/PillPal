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
class EditStockViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val medicationTypeRepository: MedicationTypeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditStockUiState())
    val uiState: StateFlow<EditStockUiState> = _uiState.asStateFlow()

    private val medicationId: Int = savedStateHandle.get<Int>(MEDICATION_ID_ARG)!!

    init {
        loadMedicationData()
    }

    fun loadMedicationData() {
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
                        medicationId = medication.id,
                        medicationName = medication.name,
                        remainingStock = medication.remainingDoses,
                        medicationUnit = medicationType?.name?.lowercase() ?: "doses"
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}