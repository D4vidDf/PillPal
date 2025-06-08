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
    val daysOfWeek: String?,      // Comma-separated Ints (1-7 for Mon-Sun) for "Once a day" / "Weekly"
    val specificTimes: String?,   // Comma-separated LocalTime strings for "Once a day" (single) or "Multiple times a day" (list)
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
            val times = specificTimes?.split(",")?.joinToString { java.time.LocalTime.parse(it).format(timeFormatter) } ?: "N/A"
            "Daily at $times"
        }
        ScheduleType.WEEKLY -> {
            val days = daysOfWeek?.split(",")?.mapNotNull {
                when (it.trim()) {
                    "1" -> "Mon"
                    "2" -> "Tue"
                    "3" -> "Wed"
                    "4" -> "Thu"
                    "5" -> "Fri"
                    "6" -> "Sat"
                    "7" -> "Sun"
                    else -> null
                }
            }?.joinToString() ?: "N/A"
            val times = specificTimes?.split(",")?.joinToString { java.time.LocalTime.parse(it).format(timeFormatter) } ?: "N/A"
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
        ScheduleType.CUSTOM_ALARMS -> { // Assuming CUSTOM_ALARMS is similar to DAILY/WEEKLY in terms of specificTimes
            val times = specificTimes?.split(",")?.joinToString { java.time.LocalTime.parse(it).format(timeFormatter) } ?: "N/A"
            "Custom alarms at $times"
        }
    }
}
