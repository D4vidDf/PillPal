package com.d4viddf.medicationreminder.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao,
    private val firebaseSyncDao: FirebaseSyncDao
) {

    fun getAllMedications(): Flow<List<Medication>> = medicationDao.getAllMedications()

    suspend fun insertMedication(medication: Medication): Int {
        val newId = medicationDao.insertMedication(medication).toInt()
        // Record sync request for Firebase
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "Medication", entityId = newId, syncStatus = SyncStatus.PENDING)
        )
        return newId
    }

    suspend fun updateMedication(medication: Medication) {
        medicationDao.updateMedication(medication)
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "Medication", entityId = medication.id, syncStatus = SyncStatus.PENDING)
        )
    }

    suspend fun deleteMedication(medication: Medication) {
        medicationDao.deleteMedication(medication)
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "Medication", entityId = medication.id, syncStatus = SyncStatus.PENDING)
        )
    }

    suspend fun updateDoseCount(medicationId: Int, remainingDoses: Int) {
        val medication = medicationDao.getMedicationById(medicationId)
        if (medication != null) {
            val updatedMedication = medication.copy(remainingDoses = remainingDoses)
            updateMedication(updatedMedication)
        }
    }

    suspend fun getMedicationById(medicationId: Int): Medication? {
        return medicationDao.getMedicationById(medicationId)
    }
}

