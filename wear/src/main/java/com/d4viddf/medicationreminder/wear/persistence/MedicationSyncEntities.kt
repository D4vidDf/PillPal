package com.d4viddf.medicationreminder.wear.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
// Removed: import com.d4viddf.medicationreminder.wear.data.MedicationScheduleDetailSyncItem
// This import is not directly needed here as the fields are basic types or handled by TypeConverters

@Entity(tableName = "medications_sync")
data class MedicationSyncEntity(
    @PrimaryKey val medicationId: Int,
    val name: String,
    val dosage: String?,
    val color: String?,
    val typeName: String?,
    val typeIconUrl: String?,
    val startDate: String?,
    val endDate: String?
)

@Entity(
    tableName = "schedule_details_sync",
    primaryKeys = ["medicationId", "scheduleId"],
    foreignKeys = [
        ForeignKey(
            entity = MedicationSyncEntity::class,
            parentColumns = ["medicationId"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScheduleDetailSyncEntity(
    val medicationId: Int,
    val scheduleId: Long,
    val scheduleType: String,
    val specificTimesJson: String?,
    val intervalHours: Int?,
    val intervalMinutes: Int?,
    val intervalStartTime: String?,
    val intervalEndTime: String?,
    val dailyRepetitionDaysJson: String?
)

data class MedicationWithSchedulesPojo(
    @Embedded val medication: MedicationSyncEntity,
    @Relation(
        parentColumn = "medicationId",
        entityColumn = "medicationId"
    )
    val schedules: List<ScheduleDetailSyncEntity>
)

@Entity(tableName = "reminder_states")
data class ReminderStateEntity(
    @PrimaryKey val reminderInstanceId: String, // Unique ID for each reminder slot (e.g., medId_scheduleId_timestampEpochSeconds)
    val medicationId: Int, // Foreign key to MedicationSyncEntity might be useful for cleanup
    val scheduleId: Long,  // Foreign key to ScheduleDetailSyncEntity might be useful
    val reminderTimeKey: String, // E.g., "HH:mm" or a specific timestamp part to identify the slot for non-synced reminders
    var isTaken: Boolean,
    var takenAt: String? // ISO 8601 timestamp string when taken
)
