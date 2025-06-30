package com.d4viddf.medicationreminder.ui.features.home.model

import androidx.annotation.DrawableRes

data class NextDoseUiItem(
    val reminderId: Int,
    val medicationId: Int,
    val medicationName: String,
    val medicationDosage: String,
    val medicationColorName: String,
    @DrawableRes val medicationTypeIconRes: Int, // Resolved icon resource
    val formattedReminderTime: String, // e.g., "09:00"
    // val rawReminderTime: String, // Optional: if needed for sorting before formatting
)
