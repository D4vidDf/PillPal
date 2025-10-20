package com.d4viddf.medicationreminder.ui.features.medication.edit

import com.d4viddf.medicationreminder.data.model.MedicationForm
import com.d4viddf.medicationreminder.ui.theme.MedicationColor

data class EditFormState(
    val isLoading: Boolean = true,
    val medicationForm: MedicationForm? = null,
    val medicationColor: MedicationColor? = null
)