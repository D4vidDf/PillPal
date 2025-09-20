package com.d4viddf.medicationreminder.data.model.healthdata

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "heart_rate")
data class HeartRate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Instant,
    val beatsPerMinute: Long,
    val sourceApp: String? = null
)
