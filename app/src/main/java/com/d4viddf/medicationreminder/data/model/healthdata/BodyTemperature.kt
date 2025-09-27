package com.d4viddf.medicationreminder.data.model.healthdata

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "body_temperature_records")
data class BodyTemperature(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Instant,
    val temperatureCelsius: Double,
    // Metadata fields based on Health Connect spec
    val measurementLocation: Int? = null, // e.g., LOCATION_ARMPIT, LOCATION_EAR
    val sourceApp: String? = null, // e.g., "com.d4viddf.medicationreminder"
    val device: String? = null // e.g., "Generic Thermometer"
) {
    companion object {
        const val LOCATION_UNKNOWN = 0
        const val LOCATION_ARMPIT = 1
        const val LOCATION_EAR = 2
        const val LOCATION_FINGER = 3
        const val LOCATION_FOREHEAD = 4
        const val LOCATION_MOUTH = 5
        const val LOCATION_RECTUM = 6
        const val LOCATION_TEMPORAL_ARTERY = 7
        const val LOCATION_TOE = 8
        const val LOCATION_WRIST = 9
    }
}