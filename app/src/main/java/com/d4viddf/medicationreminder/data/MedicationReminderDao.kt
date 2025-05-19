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

    @Query("SELECT * FROM medication_reminder WHERE id = :id")
    suspend fun getReminderById(id: Int): MedicationReminder?

    @Query("SELECT * FROM medication_reminder WHERE medicationId = :medicationId")
    fun getRemindersForMedication(medicationId: Int): Flow<List<MedicationReminder>>

    @Query("SELECT * FROM medication_reminder WHERE isTaken = :isTaken")
    fun getRemindersByStatus(isTaken: Boolean): Flow<List<MedicationReminder>>

    @Query("SELECT * FROM medication_reminder WHERE medicationId = :medicationId AND reminderTime > :currentTimeIso AND isTaken = 0")
    fun getFutureRemindersForMedication(medicationId: Int, currentTimeIso: String): Flow<List<MedicationReminder>>

    @Query("DELETE FROM medication_reminder WHERE medicationId = :medicationId AND isTaken = 0 AND reminderTime > :currentTimeIso")
    suspend fun deleteFutureUntakenRemindersForMedication(medicationId: Int, currentTimeIso: String)

    // NUEVO: Para obtener recordatorios futuros no tomados para una medicación
    @Query("SELECT * FROM medication_reminder WHERE medicationId = :medicationId AND isTaken = 0 AND reminderTime > :currentTimeIsoString")
    fun getFutureUntakenRemindersForMedication(medicationId: Int, currentTimeIsoString: String): Flow<List<MedicationReminder>>

    @Query("DELETE FROM medication_reminder WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Int)
}