package com.d4viddf.medicationreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminder): Long

    @Update
    suspend fun updateReminder(reminder: MedicationReminder): Int // Changed to return Int

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

    @Query("SELECT * FROM medication_reminder WHERE medicationId = :medicationId AND reminderTime BETWEEN :startTime AND :endTime")
    suspend fun getRemindersForMedicationInWindow(medicationId: Int, startTime: String, endTime: String): List<MedicationReminder>

    @Query("DELETE FROM medication_reminder WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Int)

    @Query("SELECT * FROM medication_reminder WHERE medicationId = :medicationId AND isTaken = 1 ORDER BY takenAt DESC LIMIT 1")
    suspend fun getMostRecentTakenReminder(medicationId: Int): MedicationReminder?

    @Query("""
        SELECT * FROM medication_reminder
        WHERE reminderTime BETWEEN :startOfDayString AND :endOfDayString
        ORDER BY reminderTime ASC
    """)
    fun getRemindersForDay(startOfDayString: String, endOfDayString: String): Flow<List<MedicationReminder>>
}