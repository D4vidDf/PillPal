package com.d4viddf.medicationreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "firebase_sync")
data class FirebaseSync(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityName: String,             // e.g., "Medication", "MedicationSchedule"
    val entityId: Int,                  // ID of the entity that changed
    val syncStatus: SyncStatus          // ENUM: PENDING, SYNCED
)

enum class SyncStatus {
    PENDING, SYNCED
}
