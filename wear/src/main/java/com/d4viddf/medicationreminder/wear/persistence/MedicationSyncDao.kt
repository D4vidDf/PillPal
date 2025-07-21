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

    // Reminder State Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReminderState(reminderState: ReminderStateEntity)

    @Query("SELECT * FROM reminder_states WHERE reminderInstanceId = :reminderInstanceId")
    suspend fun getReminderState(reminderInstanceId: String): ReminderStateEntity?

    @Query("SELECT * FROM reminder_states")
    fun getAllReminderStates(): Flow<List<ReminderStateEntity>>

    @Query("DELETE FROM reminder_states")
    suspend fun clearAllReminderStates()

    // Update clearAndInsertSyncData to also clear reminder states
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationInfoSyncEntity(medicationInfo: MedicationInfoSyncEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationTypeSyncEntity(medicationType: MedicationTypeSyncEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationReminderSyncEntities(reminders: List<MedicationReminderSyncEntity>)

    @Query("DELETE FROM medication_info_sync")
    suspend fun clearAllMedicationInfo()

    @Query("DELETE FROM medication_types_sync")
    suspend fun clearAllMedicationTypes()

    @Query("DELETE FROM medication_reminders_sync")
    suspend fun clearAllMedicationReminders()

    @Transaction
    suspend fun clearAndInsertFullSyncData(
        medications: List<MedicationSyncEntity>,
        schedules: List<ScheduleDetailSyncEntity>,
        medicationInfos: List<MedicationInfoSyncEntity>,
        medicationTypes: List<MedicationTypeSyncEntity>,
        reminders: List<MedicationReminderSyncEntity>
    ) {
        clearAllMedications()
        clearAllSchedules()
        clearAllReminderStates()
        clearAllMedicationInfo()
        clearAllMedicationTypes()
        clearAllMedicationReminders()

        medications.forEach { insertMedicationSyncEntity(it) }
        if (schedules.isNotEmpty()) {
            insertScheduleDetailSyncEntities(schedules)
        }
        medicationInfos.forEach { insertMedicationInfoSyncEntity(it) }
        medicationTypes.forEach { insertMedicationTypeSyncEntity(it) }
        if (reminders.isNotEmpty()) {
            insertMedicationReminderSyncEntities(reminders)
        }
    }
}
