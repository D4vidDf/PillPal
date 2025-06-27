package com.d4viddf.medicationreminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.d4viddf.medicationreminder.workers.WorkerScheduler // Added for WorkerScheduler
// Commented out unused WorkManager imports, Data, etc. as WorkerScheduler handles this.
// import androidx.work.Data
// import androidx.work.ExistingWorkPolicy
// import androidx.work.OneTimeWorkRequestBuilder
// import androidx.work.WorkManager
// import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker

class AppUpdateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AppUpdateReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Application package updated. Scheduling immediate reminder refresh.")
            // Use WorkerScheduler to enqueue the work.
            // The context passed to onReceive is valid for this call.
            WorkerScheduler.scheduleRemindersImmediate(context.applicationContext)
        }
    }
}
