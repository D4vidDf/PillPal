package com.d4viddf.medicationreminder.notifications

import android.Manifest
import android.app.Application // For application context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.d4viddf.medicationreminder.MainActivity // Assuming this is your main entry point
import com.d4viddf.medicationreminder.R // For app icon
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver // For notification actions

object NotificationHelper {

    const val REMINDER_CHANNEL_ID = "medication_reminder_channel"
    const val REMINDER_CHANNEL_NAME = "Medication Reminders"
    const val REMINDER_CHANNEL_DESCRIPTION = "Notifications for medication intake reminders"
    const val ACTION_MARK_AS_TAKEN = "com.d4viddf.medicationreminder.ACTION_MARK_AS_TAKEN"
    const val ACTION_SNOOZE_REMINDER = "com.d4viddf.medicationreminder.ACTION_SNOOZE_REMINDER" // Example


    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Reminder Channel
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Use HIGH for reminders to make them pop up
            ).apply {
                description = REMINDER_CHANNEL_DESCRIPTION
                // Optionally configure lights, vibration, sound etc.
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500) // Vibrate pattern
                // setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)

            // You can create other channels here if needed (e.g., for lower priority info)
        }
    }

    fun showReminderNotification(
        context: Context,
        reminderDbId: Int,
        medicationName: String,
        medicationDosage: String,
        isIntervalType: Boolean = false,
        nextDoseTimeMillis: Long? = null,
        actualReminderTime: Long // The exact time this reminder is for (in millis)
    ) {
        // Intent to open MainActivity when notification is tapped
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_tap_reminder_id", reminderDbId) // Identify which reminder was tapped
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderDbId, // Unique request code for content tap
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Mark as Taken" action
        val markAsActionIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_MARK_AS_TAKEN
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderDbId)
        }
        // Use a unique request code for each action on each reminder
        val markAsTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderDbId + 1, // Offset for uniqueness
            markAsActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Example "Snooze" action
        val snoozeActionIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderDbId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderDbId + 2, // Further offset for uniqueness
            snoozeActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = "Time for your $medicationName!"
        val notificationText = "Take $medicationDosage."

        val notificationBuilder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // **IMPORTANT: Use a proper small icon**
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen (respects user settings)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true) // Dismiss notification when tapped
            .setWhen(actualReminderTime) // Show the actual time of the event
            .setShowWhen(true)
            .addAction(R.drawable.ic_check_circle, "Mark as Taken", markAsTakenPendingIntent) // Ensure ic_check_circle exists
            // .addAction(R.drawable.ic_snooze, "Snooze 10 min", snoozePendingIntent) // Ensure ic_snooze exists
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // Helps system prioritize
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Use default sound, vibrate, light unless channel overrides

        // Android 16 (Baklava - API 36) Progress-Centric Notification
        // The official Build.VERSION_CODES constant for Baklava will be 'VANILLA_ICE_CREAM'
        // For now, we'll use a placeholder for API 36.
        val ANDROID_16_BAKLAVA_API_LEVEL = 36 // Use Build.VERSION_CODES.VANILLA_ICE_CREAM when available

        if (isIntervalType && nextDoseTimeMillis != null && nextDoseTimeMillis > System.currentTimeMillis() && Build.VERSION.SDK_INT >= ANDROID_16_BAKLAVA_API_LEVEL) {
            // The API is: .setProgress(long endTimeMillis, long durationMillis, boolean indeterminate)
            // endTimeMillis: The time at which the progress completes (next dose time).
            // durationMillis: The total duration over which this progress occurs.
            // This needs the *start time* of the current dose window to calculate duration correctly.
            // For simplicity, let's assume 'actualReminderTime' is the start of this dose's effective period.
            val durationOfThisDoseWindow = nextDoseTimeMillis - actualReminderTime
            if (durationOfThisDoseWindow > 0) {
                // This requires Notification.Builder, not NotificationCompat.Builder directly for setProgress with new API
                // For now, we adapt. The actual implementation might need platform Notification.Builder
                // Or NotificationCompat might update to support this.
                // As of now, NotificationCompat.setProgress(max, progress, indeterminate) is the standard.
                // We can simulate by setting a custom subtext.
                val minutesRemaining = (nextDoseTimeMillis - System.currentTimeMillis()) / (1000 * 60)
                notificationBuilder.setSubText("Next dose in $minutesRemaining min (approx)")
                // TODO: Replace with actual setProgress(endTime, duration, false) when API is stable & supported by Compat
                // builder.setProgress(max, progress, false) can be used to show current progress out of total duration.
                // For example, if total interval is 60 mins, and 10 mins passed:
                // builder.setProgress(60, 10, false);
            }
        }


        with(NotificationManagerCompat.from(context)) {
            // Permission check for Android 13 (API 33) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    // Log, or ideally, the app should have already requested this permission.
                    // For a background service/receiver, you can't request permission here.
                    // The user must grant it beforehand.
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Notification for $reminderDbId not shown.")
                    return
                }
            }
            try {
                notify(reminderDbId, notificationBuilder.build())
                Log.d(TAG, "Notification shown for reminder ID: $reminderDbId")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException showing notification for $reminderDbId: ${e.message}")
            }
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
        Log.d(TAG, "Cancelled UI notification for ID: $notificationId")
    }
}