package com.d4viddf.medicationreminder.ui.features.medication.edit.duration

import java.time.LocalDate

data class MedicationDurationState(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val showEndDateConfirmationDialog: Boolean = false
)