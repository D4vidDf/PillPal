package com.d4viddf.medicationreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MedicationSchedule)

    @Update
    suspend fun updateSchedule(schedule: MedicationSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: MedicationSchedule)

    @Query("SELECT * FROM medication_schedule WHERE medicationId = :medicationId")
    fun getSchedulesForMedication(medicationId: Int): Flow<List<MedicationSchedule>>
}
