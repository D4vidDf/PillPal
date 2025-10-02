package com.d4viddf.medicationreminder.ui.features.medication.edit

import com.d4viddf.medicationreminder.ui.theme.MedicationColor

data class EditFormAndColorState(
    val isLoading: Boolean = true,
    val medicationTypeId: Int? = null,
    val medicationColor: MedicationColor? = null
)