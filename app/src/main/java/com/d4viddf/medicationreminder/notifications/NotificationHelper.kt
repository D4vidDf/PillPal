package com.d4viddf.medicationreminder.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes // Added import
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.d4viddf.medicationreminder.MainActivity
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.common.IntentActionConstants
import com.d4viddf.medicationreminder.common.IntentExtraConstants
import com.d4viddf.medicationreminder.common.NotificationConstants
import com.d4viddf.medicationreminder.receivers.SnoozeBroadcastReceiver // Ensure this import is present
// import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver // Now using IntentExtraConstants
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

object NotificationHelper {

    // REMINDER_CHANNEL_NAME and REMINDER_CHANNEL_DESCRIPTION removed
    // PRE_REMINDER_CHANNEL_NAME and PRE_REMINDER_CHANNEL_DESCRIPTION removed

    private const val TAG = "NotificationHelper"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) // Changed to TYPE_ALARM

            val reminderChannelHigh = NotificationChannel(
                NotificationConstants.REMINDER_CHANNEL_ID,
                context.getString(R.string.notification_channel_reminder_name), // Use new string resource
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_reminder_description) // Use new string resource
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(defaultSoundUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())

            }

            val preReminderChannelDefault = NotificationChannel(
                NotificationConstants.PRE_REMINDER_CHANNEL_ID,
                context.getString(R.string.notification_channel_prereminder_name), // Use new string resource
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_prereminder_description) // Use new string resource
                enableLights(false) // Typically pre-reminders are less intrusive
                enableVibration(false) // No vibration for pre-reminders or very subtle
                // setSound(null, null) // Explicitly no sound for pre-reminders by default
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannelHigh)
            Log.d(TAG, "Notification channel ${NotificationConstants.REMINDER_CHANNEL_ID} created with HIGH importance and sound.")
            notificationManager.createNotificationChannel(preReminderChannelDefault)
            Log.d(TAG, "Notification channel ${NotificationConstants.PRE_REMINDER_CHANNEL_ID} created with DEFAULT importance.")
        }
    }

    private fun formatTimeRemaining(context: Context, millis: Long): String {
        if (millis <= 0) return "" // Or "now" or "ended"
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("$hours hr") // Consider R.string.hours_abbreviation
        if (minutes > 0 || hours == 0L) parts.add("$minutes min") // Consider R.string.minutes_abbreviation

        return if (parts.isEmpty()) context.getString(R.string.less_than_a_minute) else parts.joinToString(" ")
    }


    fun showReminderNotification(
        context: Context,
        reminderDbId: Int,
        medicationName: String,
        medicationDosage: String,
        isIntervalType: Boolean,
        nextDoseTimeMillisForHelper: Long?,
        actualReminderTimeMillis: Long,
        notificationSoundUriString: String?,
        medicationColorHex: String?, // New parameter
        medicationTypeName: String?,  // New parameter
        capabilityClient: CapabilityClient // Added capabilityClient
    ) {
        Log.i(
            TAG,
            "showReminderNotification called for ID: $reminderDbId, Name: $medicationName, Interval: $isIntervalType, NextDoseHelper: $nextDoseTimeMillisForHelper, ActualTime: $actualReminderTimeMillis, SoundURI: $notificationSoundUriString, Color: $medicationColorHex, Type: $medicationTypeName"
        )

        // Full-screen activity intent (used for both full-screen pop-up and content tap)
        val fullScreenActivityIntent = Intent(context, com.d4viddf.medicationreminder.ui.features.notifications.activity.FullScreenNotificationActivity::class.java).apply {
            putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, reminderDbId)
            putExtra(IntentExtraConstants.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(IntentExtraConstants.EXTRA_MEDICATION_DOSAGE, medicationDosage)
            putExtra(NotificationConstants.EXTRA_MED_COLOR_HEX, medicationColorHex)
            putExtra(NotificationConstants.EXTRA_MED_TYPE_NAME, medicationTypeName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Standard flags
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Content PendingIntent (for tapping the notification in the shade)
        // Now uses the same intent as the full-screen pop-up
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderDbId, // Standard request code for content PI
            fullScreenActivityIntent, // Use the fullScreenActivityIntent here
            pendingIntentFlags
        )

        // Full-screen PendingIntent (for the heads-up notification)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            reminderDbId + 2000, // Unique request code for full-screen PI
            fullScreenActivityIntent, // Also uses fullScreenActivityIntent
            pendingIntentFlags // Re-use flags
        )

        val markAsActionIntent = Intent(context, com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver::class.java).apply { // FQDN for ReminderBroadcastReceiver
            action = IntentActionConstants.ACTION_MARK_AS_TAKEN // Ensure this action is set
            putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, reminderDbId)
            putExtra(IntentExtraConstants.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(IntentExtraConstants.EXTRA_MEDICATION_DOSAGE, medicationDosage)
            putExtra(NotificationConstants.EXTRA_MED_COLOR_HEX, medicationColorHex)
            putExtra(NotificationConstants.EXTRA_MED_TYPE_NAME, medicationTypeName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Standard flags
        }
        // Unique request code for this pending intent
        val markAsTakenRequestCode = reminderDbId + 1000
        val markAsTakenPendingIntent = PendingIntent.getBroadcast(
            context, markAsTakenRequestCode, markAsActionIntent, pendingIntentFlags // Corrected: Was contentPendingIntentFlags
        )

        // Snooze Action Intent
        val snoozeIntent = Intent(context, SnoozeBroadcastReceiver::class.java).apply {
            action = IntentActionConstants.ACTION_SNOOZE_REMINDER
            putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, reminderDbId)
        }
        // Unique request code for snooze pending intent
        val snoozeRequestCode = reminderDbId + 3000 // Ensure this is unique
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, snoozeRequestCode, snoozeIntent, pendingIntentFlags // Corrected: Was contentPendingIntentFlags
        )

        val notificationTitle = context.getString(R.string.notification_title_time_for, medicationName)
        var notificationText = context.getString(R.string.notification_text_take_dosage, medicationDosage)

        // val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) // Old line, to be removed or replaced

        val notificationCompatBuilder = NotificationCompat.Builder(context, NotificationConstants.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(notificationTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent) // For tap on notification body
            .setFullScreenIntent(fullScreenPendingIntent, true) // For the pop-up
            .setAutoCancel(true)
            .setWhen(actualReminderTimeMillis)
            .setShowWhen(true)
            .addAction(R.drawable.ic_check, context.getString(R.string.notification_action_mark_as_taken), markAsTakenPendingIntent)
            .addAction(R.drawable.ic_snooze, context.getString(R.string.notification_action_snooze), snoozePendingIntent) // Added Snooze Action
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .extend(NotificationCompat.WearableExtender()
                .addAction(NotificationCompat.Action.Builder(
                    R.drawable.ic_check, // Use the same icon as the phone action
                    context.getString(R.string.notification_action_mark_as_taken),
                    markAsTakenPendingIntent
                ).build())
                // Add snooze action for Wear OS if it's available
                .addAction(NotificationCompat.Action.Builder(
                    R.drawable.ic_snooze, // Use the same icon as the phone action
                    context.getString(R.string.notification_action_snooze),
                    snoozePendingIntent
                ).build())
            )
        // Sound will be set below based on notificationSoundUriString

        if (!notificationSoundUriString.isNullOrEmpty()) {
            try {
                val customSoundUri = notificationSoundUriString.toUri()
                notificationCompatBuilder.setSound(customSoundUri)
                Log.d(TAG, "Using custom notification sound: $customSoundUri")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse custom sound URI: $notificationSoundUriString. Falling back to default ALARM sound.", e)
                val defaultAlarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                notificationCompatBuilder.setSound(defaultAlarmSound)
            }
        } else {
            // User selected "Default" or no sound is set, use system default ALARM sound.
            val defaultAlarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            notificationCompatBuilder.setSound(defaultAlarmSound)
            Log.d(TAG, "Using default ALARM sound: $defaultAlarmSound")
        }

        if (isIntervalType && nextDoseTimeMillisForHelper != null && nextDoseTimeMillisForHelper > actualReminderTimeMillis) {
            val startTimeMillis = actualReminderTimeMillis
            val endTimeMillis = nextDoseTimeMillisForHelper
            val durationMillis = endTimeMillis - startTimeMillis

            if (durationMillis > 0) {
                notificationCompatBuilder.setOngoing(true) // Make it persistent during the interval

                // For interval notifications, we usually show time *until next dose* or validity.
                // Progress bar here might be confusing if the "progress" is just time passing until next.
                // Let's focus on clear text.
                val timeRemainingForDoseValidityMillis = endTimeMillis - System.currentTimeMillis()
                if (timeRemainingForDoseValidityMillis > 0) {
                    val formattedTimeRemaining = formatTimeRemaining(context, timeRemainingForDoseValidityMillis)
                    notificationText = context.getString(R.string.notification_text_interval_dose_valid, medicationDosage, formattedTimeRemaining)
                } else {
                    notificationText = context.getString(R.string.notification_text_interval_dose_window_ended, medicationDosage)
                }
            }
        } else if (!isIntervalType) {
            // For non-interval, ensure no progress bar is shown from previous states if builder was reused (though it's not here)
            // notificationCompatBuilder.setProgress(0, 0, false) // Not strictly needed as it's only added for interval
        }


        notificationCompatBuilder.setContentText(notificationText)
        notificationCompatBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))

    }

    private fun showNotificationInternal(context: Context, id: Int, notification: Notification) {
        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Notification for $id not shown.")
                    return
                }
            }
            try {
                Log.i(TAG, "Showing final notification for ID: $id")
                notify(id, notification)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException showing notification for $id: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Generic Exception showing notification for $id: ${e.message}", e)
            }
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
        Log.d(TAG, "Cancelled UI notification for ID: $notificationId")
    }
}