package com.d4viddf.medicationreminder.utils

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat // Added import
import androidx.core.content.ContextCompat

object PermissionUtils {

    private const val TAG_PERMISSION_UTILS = "PermissionUtils"

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String> // For Post Notifications
    private lateinit var requestFullScreenIntentLauncher: ActivityResultLauncher<Intent>
    private lateinit var recordAudioPermissionLauncher: ActivityResultLauncher<String> // For Record Audio

    fun init(activity: ComponentActivity) {
        requestPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Log.i(TAG_PERMISSION_UTILS, "POST_NOTIFICATIONS permission granted.")
                } else {
                    Log.w(TAG_PERMISSION_UTILS, "POST_NOTIFICATIONS permission denied.")
                }
            }

        requestFullScreenIntentLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+ for canUseFullScreenIntent
                val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.canUseFullScreenIntent()) {
                    Log.i(TAG_PERMISSION_UTILS, "USE_FULL_SCREEN_INTENT permission is now granted after returning from settings.")
                } else {
                    Log.w(TAG_PERMISSION_UTILS, "USE_FULL_SCREEN_INTENT permission is still NOT granted after returning from settings.")
                }
            }
        }

        recordAudioPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Log.i(TAG_PERMISSION_UTILS, "RECORD_AUDIO permission granted.")
                } else {
                    Log.w(TAG_PERMISSION_UTILS, "RECORD_AUDIO permission denied.")
                }
            }
    }

    fun requestPostNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i(TAG_PERMISSION_UTILS, "POST_NOTIFICATIONS permission already granted.")
                }
                activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.i(TAG_PERMISSION_UTILS, "Showing rationale for POST_NOTIFICATIONS permission.")
                    if (::requestPermissionLauncher.isInitialized) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Log.e(TAG_PERMISSION_UTILS, "requestPermissionLauncher not initialized for POST_NOTIFICATIONS rationale.")
                    }
                }
                else -> {
                    Log.i(TAG_PERMISSION_UTILS, "Requesting POST_NOTIFICATIONS permission.")
                    if (::requestPermissionLauncher.isInitialized) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Log.e(TAG_PERMISSION_UTILS, "requestPermissionLauncher not initialized for POST_NOTIFICATIONS request.")
                    }
                }
            }
        }
    }

    fun requestRecordAudioPermission(activity: Activity, onAlreadyGranted: () -> Unit, onRationaleNeeded: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG_PERMISSION_UTILS, "RECORD_AUDIO permission already granted.")
                onAlreadyGranted()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO) -> {
                Log.i(TAG_PERMISSION_UTILS, "Showing rationale for RECORD_AUDIO permission.")
                onRationaleNeeded()
            }
            else -> {
                Log.i(TAG_PERMISSION_UTILS, "Requesting RECORD_AUDIO permission.")
                if (::recordAudioPermissionLauncher.isInitialized) {
                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    Log.e(TAG_PERMISSION_UTILS, "recordAudioPermissionLauncher not initialized.")
                }
            }
        }
    }

    fun checkAndRequestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG_PERMISSION_UTILS, "SCHEDULE_EXACT_ALARM permission not granted. Requesting...")
                Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    // Add URI to redirect to specific app settings if desired, though not strictly necessary for this action
                    // data = Uri.parse("package:${context.packageName}")
                }.also {
                    try {
                        context.startActivity(it)
                    } catch (e: Exception) {
                        Log.e(TAG_PERMISSION_UTILS, "Could not open ACTION_REQUEST_SCHEDULE_EXACT_ALARM settings", e)
                    }
                }
            } else {
                Log.d(TAG_PERMISSION_UTILS, "SCHEDULE_EXACT_ALARM permission already granted.")
            }
        }
    }

    fun checkAndRequestFullScreenIntentPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // NotificationManager.canUseFullScreenIntent() is API 34
            val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.canUseFullScreenIntent()) {
                Log.w(TAG_PERMISSION_UTILS, "App cannot use full-screen intents. Sending user to settings.")
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                try {
                    requestFullScreenIntentLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e(TAG_PERMISSION_UTILS, "Could not open ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT settings", e)
                }
            } else {
                Log.i(TAG_PERMISSION_UTILS, "App can use full-screen intents.")
            }
        } else {
            Log.d(TAG_PERMISSION_UTILS, "Full-screen intent permission check not applicable for API < 34 via NotificationManager.")
        }
    }
}
