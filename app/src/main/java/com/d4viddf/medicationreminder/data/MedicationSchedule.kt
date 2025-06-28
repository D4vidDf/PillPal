package com.d4viddf.medicationreminder.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_schedule",
    foreignKeys = [ForeignKey(entity = Medication::class, parentColumns = ["id"], childColumns = ["medicationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["medicationId"])]
)
data class MedicationSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val scheduleType: ScheduleType,
    val intervalHours: Int?,
    val intervalMinutes: Int?,
    val daysOfWeek: List<java.time.DayOfWeek>?,      // List of DayOfWeek enums for "Once a day" / "Weekly"
    val specificTimes: List<java.time.LocalTime>?,   // List of LocalTime objects
    val intervalStartTime: String?, // ISO LocalTime string for "Interval"
    val intervalEndTime: String?    // ISO LocalTime string for "Interval"
)

enum class ScheduleType {
    DAILY, WEEKLY, INTERVAL, AS_NEEDED, CUSTOM_ALARMS
}

// Helper function to format schedule details for display
fun MedicationSchedule.getFormattedSchedule(): String {
    val timeFormatter = java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
    return when (scheduleType) {
        ScheduleType.DAILY -> {
            val times = specificTimes?.joinToString { it.format(timeFormatter) } ?: "N/A"
            "Daily at $times"
        }
        ScheduleType.WEEKLY -> {
            val days = daysOfWeek?.joinToString { it.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) } ?: "N/A"
            val times = specificTimes?.joinToString { it.format(timeFormatter) } ?: "N/A"
            "Weekly on $days at $times"
        }
        ScheduleType.INTERVAL -> {
            val start = intervalStartTime?.let { java.time.LocalTime.parse(it).format(timeFormatter) } ?: "N/A"
            val end = intervalEndTime?.let { java.time.LocalTime.parse(it).format(timeFormatter) } ?: "N/A"
            val hours = intervalHours ?: 0
            val minutes = intervalMinutes ?: 0
            "Every ${hours}h ${minutes}m from $start to $end"
        }
        ScheduleType.AS_NEEDED -> "As needed"
        ScheduleType.CUSTOM_ALARMS -> {
            val times = specificTimes?.joinToString { it.format(timeFormatter) } ?: "N/A"
            "Custom alarms at $times"
        }
    }
}
