package com.d4viddf.medicationreminder.data

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
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "MedicationReminder", entityId = newId.toInt(), syncStatus = SyncStatus.PENDING)
        )
        return newId // RETURN THE ID
    }

    suspend fun updateReminder(reminder: MedicationReminder) {
        medicationReminderDao.updateReminder(reminder)
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(entityName = "MedicationReminder", entityId = reminder.id, syncStatus = SyncStatus.PENDING)
        )
    }

    suspend fun markReminderAsTaken(reminderId: Int, takenAt: String) {
        // Retrieve the reminder by ID
        val reminders = medicationReminderDao.getRemindersForMedication(reminderId)
        reminders.collect { list ->
            val reminder = list.firstOrNull { it.id == reminderId }
            reminder?.let {
                val updatedReminder = it.copy(isTaken = true, takenAt = takenAt)
                updateReminder(updatedReminder)
            }
        }
    }
}
