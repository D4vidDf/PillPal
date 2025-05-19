package com.d4viddf.medicationreminder

import android.Manifest // Import Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager // Import PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts // Import ActivityResultContracts
import androidx.core.content.ContextCompat // Import ContextCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import com.d4viddf.medicationreminder.ui.MedicationReminderApp
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import com.d4viddf.medicationreminder.workers.TestSimpleWorker
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
        requestPostNotificationPermission()
        checkAndRequestExactAlarmPermission()

        val testWorkRequest = OneTimeWorkRequestBuilder<TestSimpleWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(testWorkRequest)
        Log.d("MainActivity", "Enqueued TestSimpleWorker")

        // 3. Set up UI
        setContent {
            MedicationReminderApp()
        }

    }

    private fun requestPostNotificationPermission() {
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
    private fun checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // A partir de Android 12 (API 31)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("MainActivity", "SCHEDULE_EXACT_ALARM permission not granted. Requesting...")
                // Opcional: Muestra un diálogo al usuario explicando por qué necesitas este permiso.
                Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    // Opcional: puedes añadir tu URI de paquete para que el usuario sea llevado
                    // directamente a la configuración de tu app si es posible.
                    // data = Uri.parse("package:$packageName")
                }.also {
                    try {
                        startActivity(it)
                        // No hay un callback directo para este intent, el usuario debe concederlo manualmente.
                        // Podrías verificar de nuevo en onResume() de la actividad.
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Could not open ACTION_REQUEST_SCHEDULE_EXACT_ALARM settings", e)
                        // Informar al usuario que las alarmas podrían no ser precisas.
                    }
                }
            } else {
                Log.d("MainActivity", "SCHEDULE_EXACT_ALARM permission already granted.")
            }
        }
    }
}