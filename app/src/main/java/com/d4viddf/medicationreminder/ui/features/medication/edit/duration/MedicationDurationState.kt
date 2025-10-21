package com.d4viddf.medicationreminder.ui.features.medication.edit.duration

data class MedicationDurationState(
    val startDate: String = "",
    val endDate: String = "",
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false
)