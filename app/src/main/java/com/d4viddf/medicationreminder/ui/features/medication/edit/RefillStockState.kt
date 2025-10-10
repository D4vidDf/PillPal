package com.d4viddf.medicationreminder.ui.features.medication.edit

data class RefillStockState(
    val isLoading: Boolean = true,
    val currentStock: Int = 0,
    val amountToAdd: String = "0",
    val medicationUnit: String = "doses"
)