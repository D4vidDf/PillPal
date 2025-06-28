package com.d4viddf.medicationreminder.workers

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

object WorkerScheduler {

    private const val GLOBAL_REFRESH_WORK_NAME = "GlobalImmediateReminderScheduling"
    private const val MEDICATION_SPECIFIC_WORK_NAME_PREFIX = "MedicationSpecificScheduling_"

    /**
     * Schedules an immediate global refresh of reminders for ALL medications.
     * Typically used on app boot, app update, or when a global re-evaluation is needed.
     */
    fun scheduleRemindersGlobalRefresh(context: Context) {
        val inputData = Data.Builder()
            .putBoolean(ReminderSchedulingWorker.KEY_IS_DAILY_REFRESH, true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderSchedulingWorker>()
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            GLOBAL_REFRESH_WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace any pending global refresh
            workRequest
        )
    }

    /**
     * Schedules an immediate refresh of reminders for a SPECIFIC medication.
     * Typically used after adding, editing, deleting a medication, or marking a dose as taken.
     */
    fun scheduleRemindersForMedication(context: Context, medicationId: Int) {
        if (medicationId == -1 || medicationId == 0) { // Basic validation for medicationId
            // Potentially log an error or fall back to global refresh if appropriate,
            // or rely on caller to provide valid ID. For now, just log and return if invalid.
            android.util.Log.e("WorkerScheduler", "scheduleRemindersForMedication called with invalid medicationId: $medicationId")
            return
        }

        val inputData = Data.Builder()
            .putInt(ReminderSchedulingWorker.KEY_MEDICATION_ID, medicationId)
            // KEY_IS_DAILY_REFRESH will be false by default, which is correct for specific med scheduling
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderSchedulingWorker>()
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            // Add a tag that could be used for cancellation if needed, though unique work name is primary
            .addTag("${ReminderSchedulingWorker.WORK_NAME_PREFIX}${medicationId}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "$MEDICATION_SPECIFIC_WORK_NAME_PREFIX$medicationId", // Unique name per medication
            ExistingWorkPolicy.REPLACE, // Replace any pending specific refresh for this medication
            workRequest
        )
    }
}
