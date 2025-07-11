package com.d4viddf.medicationreminder.wear.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationSyncDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationSyncEntity(medication: MedicationSyncEntity)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleDetailSyncEntities(schedules: List<ScheduleDetailSyncEntity>)

    @Transaction
    @Query("DELETE FROM medications_sync")
    suspend fun clearAllMedications()

    @Transaction
    @Query("DELETE FROM schedule_details_sync")
    suspend fun clearAllSchedules()

    @Transaction
    suspend fun clearAndInsertSyncData(medications: List<MedicationSyncEntity>, schedules: List<ScheduleDetailSyncEntity>) {
        clearAllMedications()
        // clearAllSchedules() // This might be redundant if CASCADE DELETE works on medications_sync table for its related schedules.
        medications.forEach { insertMedicationSyncEntity(it) }
        if (schedules.isNotEmpty()) {
             insertScheduleDetailSyncEntities(schedules)
        }
    }

    @Transaction
    @Query("SELECT * FROM medications_sync")
    fun getAllMedicationsWithSchedules(): Flow<List<MedicationWithSchedulesPojo>>

    @Transaction
    @Query("SELECT * FROM medications_sync WHERE medicationId = :medicationId")
    fun getMedicationWithSchedulesById(medicationId: Int): Flow<MedicationWithSchedulesPojo?>
}
