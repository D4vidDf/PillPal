package com.d4viddf.medicationreminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.d4viddf.medicationreminder.workers.WorkerScheduler // Added for WorkerScheduler
// Hilt cannot directly inject into BroadcastReceivers declared in the Manifest
// unless they are for specific Hilt extensions (like HiltWorker).
// For a simple BootCompletedReceiver, you usually don't need Hilt injection directly in it.
// If you did, you'd need a more complex setup or have it trigger a Hilt service/worker.
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return // context can be nullable, ensure it's handled.
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device boot completed. Scheduling global reminder refresh.")
            // Use WorkerScheduler to enqueue the work.
            // The context passed to onReceive is valid for this call.
            WorkerScheduler.scheduleRemindersGlobalRefresh(context.applicationContext)
        }
    }
}