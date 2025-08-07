package com.d4viddf.medicationreminder.data.model.healthdata

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "weight_records")
data class Weight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Instant,
    val weightKilograms: Double,
    // Metadata fields
    val sourceApp: String? = null,
    val device: String? = null
)