package com.d4viddf.medicationreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminder): Long

    @Update
    suspend fun updateReminder(reminder: MedicationReminder)

    @Delete
    suspend fun deleteReminder(reminder: MedicationReminder)

    @Query("SELECT * FROM medication_reminder WHERE medicationId = :medicationId")
    fun getRemindersForMedication(medicationId: Int): Flow<List<MedicationReminder>>

    @Query("SELECT * FROM medication_reminder WHERE isTaken = :isTaken")
    fun getRemindersByStatus(isTaken: Boolean): Flow<List<MedicationReminder>>
}
