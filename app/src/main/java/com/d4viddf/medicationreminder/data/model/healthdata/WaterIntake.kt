package com.d4viddf.medicationreminder.data.model.healthdata

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "water_intake_records")
data class WaterIntake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Instant,
    val volumeMilliliters: Double,
    val type: String? = null,
    // Metadata fields
    val sourceApp: String? = null,
    val device: String? = null
)