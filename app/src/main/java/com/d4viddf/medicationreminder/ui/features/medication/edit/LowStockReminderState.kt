package com.d4viddf.medicationreminder.ui.features.medication.edit

data class LowStockReminderState(
    val isLoading: Boolean = true,
    val medicationName: String = "",
    val runsOutInDays: Int? = null,
    val selectedDays: Int = 7 // Default to 7 days
)