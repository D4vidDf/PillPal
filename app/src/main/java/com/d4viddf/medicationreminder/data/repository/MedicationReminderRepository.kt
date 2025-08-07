package com.d4viddf.medicationreminder.data

import android.content.Context
import android.util.Log
import com.d4viddf.medicationreminder.data.model.FirebaseSync
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.data.model.SyncStatus
import com.d4viddf.medicationreminder.data.source.local.FirebaseSyncDao
import com.d4viddf.medicationreminder.data.source.local.MedicationReminderDao
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MedicationReminderRepository @Inject constructor(
    @ApplicationContext private val context: Context, // Inject context for DataClient
    private val medicationReminderDao: MedicationReminderDao,
    private val firebaseSyncDao: FirebaseSyncDao
) {

    private val dataClient: DataClient by lazy { Wearable.getDataClient(context) }

    fun getRemindersForMedication(medicationId: Int): Flow<List<MedicationReminder>> =
        medicationReminderDao.getRemindersForMedication(medicationId)

    suspend fun insertReminder(reminder: MedicationReminder): Long {
        val newId = medicationReminderDao.insertReminder(reminder)
        val entityIdForSync = if (reminder.id == 0) newId.toInt() else reminder.id
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(
                entityName = "MedicationReminder",
                entityId = entityIdForSync,
                syncStatus = SyncStatus.PENDING
            )
        )
        // Potentially sync new reminder to watch if needed, though initial request focuses on updates
        // syncReminderToWatch(reminder.copy(id = newId.toInt())) // Assuming ID is updated in the object
        return newId
    }

    suspend fun updateReminder(reminder: MedicationReminder): Int { // Return Int
        Log.d(TAG, "Attempting to update reminder in DAO. ID: ${reminder.id}, isTaken: ${reminder.isTaken}, takenAt: ${reminder.takenAt}")
        val rowsUpdated = medicationReminderDao.updateReminder(reminder)
        Log.i(TAG, "Reminder DAO update executed. Rows updated: $rowsUpdated for reminder ID: ${reminder.id}")
        if (rowsUpdated > 0) {
            firebaseSyncDao.insertSyncRecord(
                FirebaseSync(
                    entityName = "MedicationReminder",
                    entityId = reminder.id,
                    syncStatus = SyncStatus.PENDING
                )
            )
            syncReminderToWatch(reminder) // Sync change to watch
        } else {
            Log.e(TAG, "CRITICAL: Update reminder ID ${reminder.id} affected 0 rows in DB!")
        }
        return rowsUpdated // Return the count
    }

    suspend fun deleteReminder(reminder: MedicationReminder) {
        medicationReminderDao.deleteReminder(reminder)
        firebaseSyncDao.insertSyncRecord(
            FirebaseSync(
                entityName = "MedicationReminder",
                entityId = reminder.id,
                syncStatus = SyncStatus.PENDING
            )
        ) // O similar
        // Potentially sync deletion to watch if needed
        // syncDeletionToWatch(reminder.id, "MedicationReminder")
    }

    suspend fun markReminderAsTaken(reminderId: Int, takenAt: String?): Boolean { // Return Boolean for success
        val reminder = medicationReminderDao.getReminderById(reminderId)
        var success = false
        reminder?.let {
            val updatedReminder = it.copy(isTaken = true, takenAt = takenAt)
            // updateReminder will handle DAO update and syncing to watch
            val rowsUpdated = updateReminder(updatedReminder)
            if (rowsUpdated > 0) {
                Log.i(TAG, "Successfully marked reminder ID $reminderId as taken. Rows updated: $rowsUpdated")
                success = true
            } else {
                Log.e(TAG, "Failed to mark reminder ID $reminderId as taken (0 rows updated in DB).")
            }
        } ?: run {
            Log.e(TAG, "markReminderAsTaken: Reminder not found with ID: $reminderId")
        }
        return success
    }

    private suspend fun syncReminderToWatch(reminder: MedicationReminder) {
        try {
            val putDataMapReq = PutDataMapRequest.create("$REMINDER_PATH_PREFIX/${reminder.id}")
            putDataMapReq.dataMap.putInt(KEY_REMINDER_ID, reminder.id)
            putDataMapReq.dataMap.putBoolean(KEY_REMINDER_IS_TAKEN, reminder.isTaken)
            reminder.takenAt?.let { putDataMapReq.dataMap.putString(KEY_REMINDER_TAKEN_AT, it) }
            // medicationName would require fetching from Medication table via medicationId, skipping for now
            // putDataMapReq.dataMap.putString(KEY_REMINDER_MEDICATION_NAME, reminder.medicationName)
            putDataMapReq.dataMap.putString(KEY_REMINDER_SCHEDULED_TIME, reminder.reminderTime)
            // Add other relevant fields

            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq)
            Log.i(TAG, "Reminder ${reminder.id} data synced to watch.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync reminder ${reminder.id} to watch: ${e.message}", e)
        }
    }

    // Example for syncing all reminders (could be called on app start or after a full sync)
    suspend fun syncAllRemindersToWatch(reminders: List<MedicationReminder>) {
        reminders.forEach { syncReminderToWatch(it) }
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

    /**
     * Gets all reminders scheduled for a given day.
     * @param startOfDayString The ISO-8601 string for the beginning of the day (e.g., "2025-08-07T00:00:00").
     * @param endOfDayString The ISO-8601 string for the end of the day (e.g., "2025-08-07T23:59:59.999").
     */
    open fun getRemindersForDay(startOfDayString: String, endOfDayString: String): Flow<List<MedicationReminder>> {
        return medicationReminderDao.getRemindersForDay(startOfDayString, endOfDayString)
    }

    /**
     * Gets all reminders from before the current time that have not been marked as taken.
     * @param currentTimeIsoString The ISO-8601 string for the current time.
     */
    fun getMissedReminders(currentTimeIsoString: String): Flow<List<MedicationReminder>> {
        return medicationReminderDao.getPastUntakenReminders(currentTimeIsoString)
    }

    fun getNextUpcomingReminder(currentTime: String): Flow<MedicationReminder?> {
        return medicationReminderDao.getNextUpcomingReminder(currentTime)
    }

    companion object {
        private const val TAG = "MedReminderRepo"
        private const val REMINDER_PATH_PREFIX = "/reminder"
        private const val KEY_REMINDER_ID = "reminder_id"
        private const val KEY_REMINDER_IS_TAKEN = "reminder_is_taken"
        private const val KEY_REMINDER_TAKEN_AT = "reminder_taken_at"
        private const val KEY_REMINDER_MEDICATION_NAME = "reminder_med_name"
        private const val KEY_REMINDER_SCHEDULED_TIME = "reminder_sched_time"
        // Add other keys as needed
    }
}