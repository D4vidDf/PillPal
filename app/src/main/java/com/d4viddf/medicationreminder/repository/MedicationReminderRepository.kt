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

    suspend fun updateReminder(reminder: MedicationReminder) {
        medicationReminderDao.updateReminder(reminder)
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "MedicationReminder", entityId = reminder.id, syncStatus = SyncStatus.PENDING)
        )
    }

    suspend fun markReminderAsTaken(reminderId: Int, takenAt: String) {
        val reminder = medicationReminderDao.getReminderById(reminderId)
        reminder?.let {
            val updatedReminder = it.copy(isTaken = true, takenAt = takenAt)
            updateReminder(updatedReminder)
        } ?: run {
            Log.e("MedRecReminderRepo", "markReminderAsTaken: Reminder not found with ID: $reminderId")
        }
    }
    suspend fun getReminderById(id: Int): MedicationReminder? {
        return medicationReminderDao.getReminderById(id)
    }

    fun getFutureRemindersForMedication(medicationId: Int, currentTimeIso: String): Flow<List<MedicationReminder>> {
        return medicationReminderDao.getFutureRemindersForMedication(medicationId, currentTimeIso)
    }

    suspend fun deleteReminder(reminder: MedicationReminder) { // Asegúrate de que este método existe
        medicationReminderDao.deleteReminder(reminder)
        // Considera si necesitas un firebaseSyncDao.insertSyncRecord para la eliminación
    }
}