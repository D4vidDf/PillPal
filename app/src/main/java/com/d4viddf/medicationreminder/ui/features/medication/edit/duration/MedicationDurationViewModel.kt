package com.d4viddf.medicationreminder.ui.features.medication.edit.duration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MedicationDurationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val medicationId: Int = savedStateHandle.get<Int>("medicationId") ?: 0

    private val _uiState = MutableStateFlow(MedicationDurationState())
    val uiState: StateFlow<MedicationDurationState> = _uiState

    init {
        viewModelScope.launch {
            medicationRepository.getMedicationByIdFlow(medicationId).collectLatest { medication ->
                medication?.let {
                    _uiState.value = MedicationDurationState(
                        startDate = it.startDate?.let { LocalDate.parse(it) },
                        endDate = it.endDate?.let { LocalDate.parse(it) }
                    )
                }
            }
        }
    }

    fun onStartDateSelected(date: LocalDate) {
        viewModelScope.launch {
            val medication = medicationRepository.getMedicationById(medicationId)
            medication?.let {
                val updatedMedication = it.copy(startDate = date.toString())
                medicationRepository.updateMedication(updatedMedication)
            }
        }
    }

    fun onEndDateSelected(date: LocalDate?) {
        viewModelScope.launch {
            val medication = medicationRepository.getMedicationById(medicationId)
            medication?.let {
                val updatedMedication = it.copy(endDate = date?.toString())
                medicationRepository.updateMedication(updatedMedication)
            }
        }
    }

    fun onShowStartDatePicker() {
        _uiState.value = _uiState.value.copy(showStartDatePicker = true)
    }

    fun onDismissStartDatePicker() {
        _uiState.value = _uiState.value.copy(showStartDatePicker = false)
    }

    fun onShowEndDatePicker() {
        _uiState.value = _uiState.value.copy(showEndDatePicker = true, showEndDateConfirmationDialog = false)
    }

    fun onDismissEndDatePicker() {
        _uiState.value = _uiState.value.copy(showEndDatePicker = false)
    }

    fun onShowEndDateConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showEndDateConfirmationDialog = true)
    }

    fun onDismissEndDateConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showEndDateConfirmationDialog = false)
    }
}