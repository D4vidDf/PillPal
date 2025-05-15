package com.d4viddf.medicationreminder

import android.Manifest // Import Manifest
import android.content.pm.PackageManager // Import PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts // Import ActivityResultContracts
import androidx.core.content.ContextCompat // Import ContextCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import com.d4viddf.medicationreminder.ui.MedicationReminderApp
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ActivityResultLauncher for the permission request
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MainActivity", "POST_NOTIFICATIONS permission granted.")
                // You can now proceed with operations that require this permission
                // (e.g., if you deferred some notification scheduling)
            } else {
                Log.w("MainActivity", "POST_NOTIFICATIONS permission denied.")
                // Explain to the user why the permission is important for reminders.
                // Optionally, guide them to app settings if they deny permanently.
                // You could show a Snackbar or Dialog here.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Create Notification Channels (moved before setContent for early setup)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createNotificationChannels(this)
            Log.d("MainActivity", "Notification channels created.")
        }

        // 2. Request Notification Permission (API 33+)
        requestNotificationPermission()

        // 3. Set up UI
        setContent {
            MedicationReminderApp()
        }

    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is API 33
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i("MainActivity", "POST_NOTIFICATIONS permission already granted.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // This block is important if the user denied the permission previously.
                    // You should show a UI explaining why you need the permission
                    // and then invoke the launcher again.
                    Log.i("MainActivity", "Showing rationale for POST_NOTIFICATIONS permission.")
                    // Example: Show a dialog explaining the need for notifications for reminders.
                    // After the user interacts with the dialog, you might call:
                    // requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    // For simplicity now, just re-requesting, but a rationale UI is better UX.
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Directly request the permission for the first time or if no rationale needed.
                    Log.i("MainActivity", "Requesting POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    // calculateDelayUntilMidnight() is no longer needed here if logic is inline in setupDaily...
}