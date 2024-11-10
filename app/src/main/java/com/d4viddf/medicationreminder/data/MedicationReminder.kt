package com.d4viddf.medicationreminder.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_reminder",
    foreignKeys = [
        ForeignKey(entity = Medication::class, parentColumns = ["id"], childColumns = ["medicationId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MedicationSchedule::class, parentColumns = ["id"], childColumns = ["medicationScheduleId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val medicationScheduleId: Int,
    val reminderTime: String,
    val isTaken: Boolean = false,
    val takenAt: String?,          // Timestamp for when the dose was taken
    val notificationId: Int?       // ID for managing Android notifications
)