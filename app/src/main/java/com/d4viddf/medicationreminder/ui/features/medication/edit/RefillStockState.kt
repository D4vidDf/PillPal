package com.d4viddf.medicationreminder.ui.features.medication.edit

import com.d4viddf.medicationreminder.data.model.MedicationForm

data class RefillStockState(
    val isLoading: Boolean = true,
    val currentStock: Int = 0,
    val amountToAdd: String = "0",
    val medicationUnit: MedicationForm? = null
)