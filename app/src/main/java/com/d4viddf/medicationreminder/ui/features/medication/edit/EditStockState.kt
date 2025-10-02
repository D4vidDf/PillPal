package com.d4viddf.medicationreminder.ui.features.medication.edit

data class EditStockState(
    val isLoading: Boolean = true,
    val medicationName: String = "",
    val remainingStock: Int = 0,
    val medicationUnit: String = "doses" // e.g., pills, inhalations, etc.
)