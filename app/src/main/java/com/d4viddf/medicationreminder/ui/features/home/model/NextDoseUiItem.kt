package com.d4viddf.medicationreminder.ui.features.home.model


data class NextDoseUiItem(
    val reminderId: Int,
    val medicationId: Int,
    val medicationName: String,
    val medicationDosage: String,
    val medicationColorName: String,
    val medicationImageUrl: String?, // Using imageUrl from MedicationType
    val rawReminderTime: String,
    val formattedReminderTime: String,
)
