package com.d4viddf.medicationreminder.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_schedule",
    foreignKeys = [ForeignKey(entity = Medication::class, parentColumns = ["id"], childColumns = ["medicationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["medicationId"])] // AÑADE ESTO
)
data class MedicationSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val scheduleType: ScheduleType,
    val intervalHours: Int?,
    val intervalMinutes: Int?,
    val daysOfWeek: String?,      // Comma-separated Ints (1-7 for Mon-Sun) for "Once a day" / "Weekly"
    val specificTimes: String?,   // Comma-separated LocalTime strings for "Once a day" (single) or "Multiple times a day" (list)
    val intervalStartTime: String?, // ISO LocalTime string for "Interval"
    val intervalEndTime: String?    // ISO LocalTime string for "Interval"
)

enum class ScheduleType {
    DAILY, WEEKLY, INTERVAL, AS_NEEDED, CUSTOM_ALARMS
}
