package com.d4viddf.medicationreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FirebaseSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncRecord(syncRecord: FirebaseSync)

    @Update
    suspend fun updateSyncRecord(syncRecord: FirebaseSync)

    @Delete
    suspend fun deleteSyncRecord(syncRecord: FirebaseSync)

    @Query("SELECT * FROM firebase_sync WHERE syncStatus = :status")
    fun getPendingSyncRecords(status: SyncStatus = SyncStatus.PENDING): Flow<List<FirebaseSync>>
}
