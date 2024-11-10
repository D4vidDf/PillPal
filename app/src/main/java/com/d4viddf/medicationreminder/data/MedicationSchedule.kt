package com.d4viddf.medicationreminder.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_schedule",
    foreignKeys = [ForeignKey(entity = Medication::class, parentColumns = ["id"], childColumns = ["medicationId"], onDelete = ForeignKey.CASCADE)]
)
data class MedicationSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val scheduleType: ScheduleType,        // ENUM or String - e.g., "Daily", "Weekly", "As Needed"
    val intervalHours: Int?,               // Interval in hours for countdown schedules
    val intervalMinutes: Int?,
    val daysOfWeek: String?,               // JSON array or comma-separated values for days of week, e.g., ["Monday", "Wednesday"]
    val specificTimes: String?             // JSON array for specific alarm times, e.g., ["08:00", "14:00"]
)

enum class ScheduleType {
    DAILY, WEEKLY, INTERVAL, AS_NEEDED, CUSTOM_ALARMS
}
