package com.d4viddf.medicationreminder.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

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
    val daysOfWeek: List<DayOfWeek>?,      // List of DayOfWeek enums for "Once a day" / "Weekly"
    val specificTimes: List<LocalTime>?,   // List of LocalTime objects
    val intervalStartTime: String?, // ISO LocalTime string for "Interval"
    val intervalEndTime: String?    // ISO LocalTime string for "Interval"
)

enum class ScheduleType {
    DAILY, WEEKLY, INTERVAL, AS_NEEDED, CUSTOM_ALARMS
}

// Helper function to format schedule details for display
fun MedicationSchedule.getFormattedSchedule(): String {
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return when (scheduleType) {
        ScheduleType.DAILY -> {
            val times = specificTimes?.joinToString { it.format(timeFormatter) } ?: "N/A"
            "Daily at $times"
        }
        ScheduleType.WEEKLY -> {
            val days = daysOfWeek?.joinToString { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) } ?: "N/A"
            val times = specificTimes?.joinToString { it.format(timeFormatter) } ?: "N/A"
            "Weekly on $days at $times"
        }
        ScheduleType.INTERVAL -> {
            val start = intervalStartTime?.let { LocalTime.parse(it).format(timeFormatter) } ?: "N/A"
            val end = intervalEndTime?.let { LocalTime.parse(it).format(timeFormatter) } ?: "N/A"
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
