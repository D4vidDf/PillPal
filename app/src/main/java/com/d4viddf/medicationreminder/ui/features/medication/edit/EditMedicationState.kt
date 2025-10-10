package com.d4viddf.medicationreminder.ui.features.medication.edit

import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationType

import com.d4viddf.medicationreminder.data.model.MedicationDosage
import com.d4viddf.medicationreminder.data.model.MedicationSchedule

data class EditMedicationState(
    val isLoading: Boolean = true,
    val medication: Medication? = null,
    val medicationType: MedicationType? = null,
    val schedule: MedicationSchedule? = null,
    val dose: MedicationDosage? = null,
    val showArchiveDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showEndTreatmentDialog: Boolean = false,
    val showSaveDialog: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val showColorPicker: Boolean = false
)