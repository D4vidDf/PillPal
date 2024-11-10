package com.d4viddf.medicationreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val typeId: Int?,
    val color: Int,
    val dosage: String?,          // Dosage information, e.g., "500 mg"
    val packageSize: Int,         // Number of doses in the package
    val remainingDoses: Int,      // Number of doses left in the package
    val startDate: String?,       // Start date of taking medication (optional)
    val endDate: String?,          // End date if the medication is not chronic (optional)
    val reminderTime: String? // Nullable in case the reminder time is not set
)
