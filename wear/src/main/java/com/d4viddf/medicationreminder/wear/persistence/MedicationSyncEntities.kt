package com.d4viddf.medicationreminder.wear.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.d4viddf.medicationreminder.wear.data.MedicationScheduleDetailSyncItem

@Entity(tableName = "medications_sync")
data class MedicationSyncEntity(
    @PrimaryKey val medicationId: Int,
    val name: String,
    val dosage: String?,
    val color: String?,
    val typeName: String?,
    val typeIconUrl: String?,
    val startDate: String?, // "dd/MM/yyyy"
    val endDate: String?   // "dd/MM/yyyy"
)

@Entity(
    tableName = "schedule_details_sync",
    primaryKeys = ["medicationId", "scheduleId"], // Composite primary key
    foreignKeys = [
        ForeignKey(
            entity = MedicationSyncEntity::class,
            parentColumns = ["medicationId"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE // If medication is deleted, its schedules are also deleted
        )
    ]
)
data class ScheduleDetailSyncEntity(
    val medicationId: Int, // Foreign key to MedicationSyncEntity
    val scheduleId: Long,  // Original schedule ID from phone
    val scheduleType: String,
    // For lists like specificTimes and dailyRepetitionDays, Room needs type converters
    // or store them as joined strings. For simplicity in this step, let's use joined strings.
    val specificTimesJson: String?, // JSON string of List<String>
    val intervalHours: Int?,
    val intervalMinutes: Int?,
    val intervalStartTime: String?, // "HH:mm"
    val intervalEndTime: String?,   // "HH:mm"
    val dailyRepetitionDaysJson: String? // JSON string of List<String>
)

// Data class to hold a medication with its schedules (for querying)
data class MedicationWithSchedulesPojo(
    @Embedded val medication: MedicationSyncEntity,
    @Relation(
        parentColumn = "medicationId",
        entityColumn = "medicationId"
    )
    val schedules: List<ScheduleDetailSyncEntity>
)
