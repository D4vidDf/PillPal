package com.d4viddf.medicationreminder.data

import java.time.LocalTime

data class TodayScheduleItem(
    val id: String, // Unique ID for the reminder instance
    val medicationName: String,
    val time: LocalTime, // Or formatted string if preferred for display
    val isPast: Boolean,
    var isTaken: Boolean,
    val underlyingReminderId: Long // Or appropriate ID type for DB operations, using Long as a placeholder
)
