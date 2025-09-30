package com.d4viddf.medicationreminder.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_dosages",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicationId"])]
)
data class MedicationDosage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val dosage: String,
    val startDate: String,
    val endDate: String? = null // Nullable to indicate the active dosage
)