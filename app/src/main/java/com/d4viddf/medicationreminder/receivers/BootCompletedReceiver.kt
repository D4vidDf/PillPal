// (e.g., in com.d4viddf.medicationreminder.receivers)
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
import dagger.hilt.android.AndroidEntryPoint

// Hilt cannot directly inject into BroadcastReceivers declared in the Manifest
// unless they are for specific Hilt extensions (like HiltWorker).
// For a simple BootCompletedReceiver, you usually don't need Hilt injection directly in it.
// If you did, you'd need a more complex setup or have it trigger a Hilt service/worker.
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device boot completed. Enqueueing reminder rescheduling work.")

            // Enqueue the ReminderSchedulingWorker to refresh/reschedule all reminders
            val workManager = WorkManager.getInstance(context.applicationContext)
            val data = Data.Builder()
                .putBoolean(ReminderSchedulingWorker.KEY_IS_DAILY_REFRESH, true) // Use the same flag as daily refresh
                // Or add a new flag: .putBoolean("is_boot_reschedule", true)
                .build()

            val rescheduleWorkRequest = OneTimeWorkRequestBuilder<ReminderSchedulingWorker>()
                .setInputData(data)
                .addTag("BootRescheduleRemindersWorker")
                .build()

            // Enqueue as unique work to prevent multiple executions if boot signal is somehow received multiple times
            workManager.enqueueUniqueWork(
                "BootRescheduleReminders",
                ExistingWorkPolicy.REPLACE, // Replace any pending boot reschedule
                rescheduleWorkRequest
            )
        }
    }
}