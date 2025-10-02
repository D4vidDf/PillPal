package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.repository.MedicationDosageRepository
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationScheduleRepository
import com.d4viddf.medicationreminder.data.repository.MedicationTypeRepository
import com.d4viddf.medicationreminder.ui.navigation.MEDICATION_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class EditMedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val medicationTypeRepository: MedicationTypeRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val dosageRepository: MedicationDosageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditMedicationState())
    val uiState: StateFlow<EditMedicationState> = _uiState.asStateFlow()

    private val medicationId: Int = savedStateHandle.get<Int>(MEDICATION_ID_ARG)!!
    private var initialMedicationState: Medication? = null

    init {
        loadMedicationData()
    }

    fun loadMedicationData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val medication = medicationRepository.getMedicationById(medicationId)
            initialMedicationState = medication // Store initial state
            if (medication != null) {
                val medicationType = medication.typeId?.let {
                    medicationTypeRepository.getMedicationTypeById(it)
                }
                val schedule = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()?.firstOrNull()
                val activeDosage = dosageRepository.getActiveDosage(medicationId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        medication = medication,
                        medicationType = medicationType,
                        schedule = schedule,
                        dose = activeDosage
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun checkForUnsavedChanges() {
        val hasChanges = _uiState.value.medication != initialMedicationState
        _uiState.update { it.copy(hasUnsavedChanges = hasChanges) }
    }

    // --- Dialog Control ---
    fun onArchiveClicked() = _uiState.update { it.copy(showArchiveDialog = true) }
    fun onDismissArchiveDialog() = _uiState.update { it.copy(showArchiveDialog = false) }

    fun onDeleteClicked() = _uiState.update { it.copy(showDeleteDialog = true) }
    fun onDismissDeleteDialog() = _uiState.update { it.copy(showDeleteDialog = false) }

    fun onEndTreatmentClicked() = _uiState.update { it.copy(showEndTreatmentDialog = true) }
    fun onDismissEndTreatmentDialog() = _uiState.update { it.copy(showEndTreatmentDialog = false) }

    fun onBackClicked(navigateBack: () -> Unit) {
        checkForUnsavedChanges()
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showSaveDialog = true) }
        } else {
            navigateBack()
        }
    }

    fun onDismissSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = false) }
    }

    // --- Actions ---
    fun confirmArchive() {
        viewModelScope.launch {
            _uiState.value.medication?.let {
                medicationRepository.updateMedication(it.copy(isArchived = true))
            }
            onDismissArchiveDialog()
        }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _uiState.value.medication?.let {
                medicationRepository.deleteMedication(it)
            }
            onDismissDeleteDialog()
        }
    }

    fun confirmEndTreatment(endDate: LocalDate) {
        viewModelScope.launch {
            _uiState.value.medication?.let {
                val formattedDate = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val updatedMedication = it.copy(endDate = formattedDate)
                medicationRepository.updateMedication(updatedMedication)
                _uiState.update { state -> state.copy(medication = updatedMedication) }
                checkForUnsavedChanges()
            }
            onDismissEndTreatmentDialog()
        }
    }

    fun toggleSuspend() {
        viewModelScope.launch {
            _uiState.value.medication?.let {
                val updatedMedication = it.copy(isSuspended = !it.isSuspended)
                medicationRepository.updateMedication(updatedMedication)
                _uiState.update { state -> state.copy(medication = updatedMedication) }
                checkForUnsavedChanges()
            }
        }
    }

    fun confirmSaveChanges() {
        viewModelScope.launch {
            _uiState.value.medication?.let {
                medicationRepository.updateMedication(it)
            }
            onDismissSaveDialog()
        }
    }
}