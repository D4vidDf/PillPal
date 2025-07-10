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
        clearAllMedications() // schedules will be cleared by CASCADE if medicationId is the only PK part
        // If schedule_details_sync has a composite PK that doesn't solely rely on medicationId for cascade, clear it explicitly.
        // Given primaryKeys = ["medicationId", "scheduleId"], cascading from medications_sync should work.
        // clearAllSchedules() // Might not be needed if cascade works as expected. For safety, can be kept.
        medications.forEach { insertMedicationSyncEntity(it) }
        if (schedules.isNotEmpty()) { // Room doesn't like inserting empty lists for varargs/collections
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
