package com.d4viddf.medicationreminder.ui.features.today_schedules

import com.d4viddf.medicationreminder.data.MedicationReminder

// Wrapper class for items in the "Today's Schedule" list
data class TodayScheduleUiItem(
    val reminder: MedicationReminder, // Keep the original reminder for actions like 'markAsTaken'
    val medicationName: String,
    val medicationDosage: String,
    val medicationColorName: String,
    val medicationIconUrl: String?, // URL for the medication type icon, or null
    val medicationTypeName: String?, // Name of the medication type, if available
    val formattedReminderTime: String
)