package com.d4viddf.medicationreminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker

class AppUpdateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AppUpdateReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Application package updated. Enqueueing reminder rescheduling work.")

            val workManager = WorkManager.getInstance(context.applicationContext)
            // We can reuse the KEY_IS_DAILY_REFRESH flag from ReminderSchedulingWorker
            // as the logic for rescheduling all reminders is what we need here.
            val data = Data.Builder()
                .putBoolean(ReminderSchedulingWorker.KEY_IS_DAILY_REFRESH, true)
                .build()

            val rescheduleWorkRequest = OneTimeWorkRequestBuilder<ReminderSchedulingWorker>()
                .setInputData(data)
                .addTag("AppUpdateRescheduleRemindersWorker") // Unique tag for this worker
                .build()

            // Enqueue as unique work to prevent multiple executions if the broadcast is somehow received multiple times
            // or if it overlaps with a boot-triggered reschedule.
            // REPLACE policy will replace any pending work with the same unique name.
            workManager.enqueueUniqueWork(
                "AppUpdateRescheduleReminders", // Unique work name
                ExistingWorkPolicy.REPLACE,
                rescheduleWorkRequest
            )
        }
    }
}
