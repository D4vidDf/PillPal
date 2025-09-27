package com.d4viddf.medicationreminder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_dosages")
data class MedicationDosage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val dosage: String,
    val startDate: String,
    val endDate: String? = null // Nullable to indicate the active dosage
)