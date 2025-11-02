package com.d4viddf.medicationreminder.ui.features.medication.edit

data class LowStockReminderState(
    val isLoading: Boolean = true,
    val medicationName: String = "",
    val runsOutInDays: Int? = null,
    val selectedDays: Int = 1 // Default to 1 day
)
