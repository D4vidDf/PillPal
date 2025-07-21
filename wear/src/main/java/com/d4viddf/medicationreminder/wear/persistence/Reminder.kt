package com.d4viddf.medicationreminder.wear.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: Int,
    val isTaken: Boolean,
    val takenAt: String?,
    val reminderTime: String,
    val medicationName: String
)
