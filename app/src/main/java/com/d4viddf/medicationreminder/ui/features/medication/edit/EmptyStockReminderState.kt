package com.d4viddf.medicationreminder.ui.features.medication.edit

data class EmptyStockReminderState(
    val isLoading: Boolean = true,
    val medicationName: String = "",
    val selectedDays: Int = 2
)
