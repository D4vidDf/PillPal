package com.d4viddf.medicationreminder.workers

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

object WorkerScheduler {
    fun scheduleRemindersImmediate(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ReminderSchedulingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // Ensures it runs as soon as possible
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "ImmediateReminderScheduling",
            ExistingWorkPolicy.REPLACE, // Replace any pending immediate request with the latest one
            workRequest
        )
    }
}
