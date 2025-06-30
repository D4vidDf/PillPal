package com.d4viddf.medicationreminder.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationReminderRepository @Inject constructor(
    private val medicationReminderDao: MedicationReminderDao,
    private val firebaseSyncDao: FirebaseSyncDao
) {

    fun getRemindersForMedication(medicationId: Int): Flow<List<MedicationReminder>> =
        medicationReminderDao.getRemindersForMedication(medicationId)

    suspend fun insertReminder(reminder: MedicationReminder): Long {
        val newId = medicationReminderDao.insertReminder(reminder)
        val entityIdForSync = if (reminder.id == 0) newId.toInt() else reminder.id
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "MedicationReminder", entityId = entityIdForSync, syncStatus = SyncStatus.PENDING)
        )
        return newId
    }

    suspend fun updateReminder(reminder: MedicationReminder): Int { // Return Int
        Log.d("MedRecReminderRepo", "Attempting to update reminder in DAO. ID: ${reminder.id}, isTaken: ${reminder.isTaken}, takenAt: ${reminder.takenAt}")
        val rowsUpdated = medicationReminderDao.updateReminder(reminder)
        Log.i("MedRecReminderRepo", "Reminder DAO update executed. Rows updated: $rowsUpdated for reminder ID: ${reminder.id}")
        if (rowsUpdated > 0) {
            firebaseSyncDao.insertSyncRecord(
                FirebaseSync(entityName = "MedicationReminder", entityId = reminder.id, syncStatus = SyncStatus.PENDING)
            )
        } else {
            Log.e("MedRecReminderRepo", "CRITICAL: Update reminder ID ${reminder.id} affected 0 rows in DB!")
        }
        return rowsUpdated // Return the count
    }

    suspend fun deleteReminder(reminder: MedicationReminder) {
        medicationReminderDao.deleteReminder(reminder)
        firebaseSyncDao.insertSyncRecord(FirebaseSync(entityName = "MedicationReminder", entityId = reminder.id, syncStatus = SyncStatus.PENDING)) // O similar
    }

    suspend fun markReminderAsTaken(reminderId: Int, takenAt: String): Boolean { // Return Boolean for success
        val reminder = medicationReminderDao.getReminderById(reminderId)
        var success = false
        reminder?.let {
            val updatedReminder = it.copy(isTaken = true, takenAt = takenAt)
            val rowsUpdated = updateReminder(updatedReminder) // Call the modified updateReminder
            if (rowsUpdated > 0) {
                Log.i("MedRecReminderRepo", "Successfully marked reminder ID $reminderId as taken. Rows updated: $rowsUpdated")
                success = true
            } else {
                Log.e("MedRecReminderRepo", "Failed to mark reminder ID $reminderId as taken (0 rows updated in DB).")
            }
        } ?: run {
            Log.e("MedRecReminderRepo", "markReminderAsTaken: Reminder not found with ID: $reminderId")
        }
        return success
    }
    suspend fun getReminderById(id: Int): MedicationReminder? {
        return medicationReminderDao.getReminderById(id)
    }

    fun getFutureRemindersForMedication(medicationId: Int, currentTimeIso: String): Flow<List<MedicationReminder>> {
        return medicationReminderDao.getFutureRemindersForMedication(medicationId, currentTimeIso)
    }

    suspend fun deleteReminderById(reminderId: Int) { // NUEVO
        medicationReminderDao.deleteReminderById(reminderId)

    }

    fun getFutureUntakenRemindersForMedication(medicationId: Int, currentTimeIsoString: String): Flow<List<MedicationReminder>> { //NUEVO
        return medicationReminderDao.getFutureUntakenRemindersForMedication(medicationId, currentTimeIsoString)
    }

    suspend fun getRemindersForMedicationInWindow(medicationId: Int, startTime: String, endTime: String): List<MedicationReminder> {
        return medicationReminderDao.getRemindersForMedicationInWindow(medicationId, startTime, endTime)
    }

    suspend fun getMostRecentTakenReminder(medicationId: Int): MedicationReminder? {
        return medicationReminderDao.getMostRecentTakenReminder(medicationId)
    }

    fun getRemindersForDay(startOfDayString: String, endOfDayString: String): Flow<List<MedicationReminder>> {
        return medicationReminderDao.getRemindersForDay(startOfDayString, endOfDayString)
    }
}