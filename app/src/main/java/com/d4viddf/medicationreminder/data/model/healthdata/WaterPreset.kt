package com.d4viddf.medicationreminder.data.model.healthdata

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_presets")
data class WaterPreset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double
)
