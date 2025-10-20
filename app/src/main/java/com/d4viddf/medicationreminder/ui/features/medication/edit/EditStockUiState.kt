package com.d4viddf.medicationreminder.ui.features.medication.edit

import com.d4viddf.medicationreminder.data.model.MedicationForm

data class EditStockUiState(
    val isLoading: Boolean = true,
    val medicationId: Int = 0,
    val medicationName: String = "",
    val remainingStock: Int = 0,
    val medicationUnit: MedicationForm? = null // e.g., pills, inhalations, etc.
)