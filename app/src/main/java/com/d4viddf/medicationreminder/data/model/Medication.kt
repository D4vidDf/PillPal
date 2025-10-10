package com.d4viddf.medicationreminder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val typeId: Int?,
    val color: String,
    val packageSize: Int,         // Number of doses in the package
    val remainingDoses: Int,      // Number of doses left in the package
    val saveRemainingFraction: Boolean = false,
    val startDate: String?,       // Start date of taking medication (optional)
    val endDate: String?,          // End date if the medication is not chronic (optional)
    val reminderTime: String?, // Nullable in case the reminder time is not set
    val registrationDate: String? = null, // New field
    val nregistro: String? = null, // CIMA registration number
    val lowStockThreshold: Int? = null,
    val lowStockReminderDays: Int? = null,
    val isArchived: Boolean = false,
    val isSuspended: Boolean = false
)
