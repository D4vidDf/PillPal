package com.d4viddf.medicationreminder.data

import com.d4viddf.medicationreminder.data.model.FirebaseSync
import com.d4viddf.medicationreminder.data.source.local.FirebaseSyncDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncRepository @Inject constructor(
    private val firebaseSyncDao: FirebaseSyncDao
) {

    fun getPendingSyncRecords(): Flow<List<FirebaseSync>> =
        firebaseSyncDao.getPendingSyncRecords()

    suspend fun updateSyncRecord(syncRecord: FirebaseSync) {
        firebaseSyncDao.updateSyncRecord(syncRecord)
    }

    suspend fun insertSyncRecord(syncRecord: FirebaseSync) {
        firebaseSyncDao.insertSyncRecord(syncRecord)
    }
}
