package com.d4viddf.medicationreminder.data.model

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_types")
data class MedicationType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @StringRes val nameResId: Int,
    val imageUrl: String?
)
