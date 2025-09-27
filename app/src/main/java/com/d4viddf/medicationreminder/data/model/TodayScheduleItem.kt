package com.d4viddf.medicationreminder.data.model

import java.time.LocalTime

data class TodayScheduleItem(
    val id: String, // Unique ID for the reminder instance
    val medicationName: String,
    val time: LocalTime, // Or formatted string if preferred for display
    val isPast: Boolean,
    var isTaken: Boolean,
    val underlyingReminderId: Long, // ID of the MedicationReminder DB record, if one exists
    val medicationScheduleId: Int,   // ID of the MedicationSchedule that generated this item
    val takenAt: String? // Timestamp for when the dose was actually taken
)