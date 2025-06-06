package com.d4viddf.medicationreminder.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver
import com.d4viddf.medicationreminder.services.PreReminderForegroundService
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor() {

    companion object {
        private const val TAG = "NotificationScheduler"
        // private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME // Not used here
    }

    fun scheduleNotification(
        context: Context,
        reminder: MedicationReminder,
        medicationName: String,
        medicationDosage: String,
        isIntervalType: Boolean,
        nextDoseTimeForHelperMillis: Long?,
        actualScheduledTimeMillis: Long
    ) {
        Log.d(TAG, "Attempting to get AlarmManager instance for scheduleNotification.")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d(TAG, "Successfully got AlarmManager instance for scheduleNotification: $alarmManager")
        val reminderTimeForLog = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(actualScheduledTimeMillis), ZoneId.systemDefault())

        Log.i(TAG, "Attempting to schedule MAIN reminder for ID: ${reminder.id} ($medicationName) at $reminderTimeForLog")

        if (actualScheduledTimeMillis < System.currentTimeMillis()) {
            Log.w(TAG, "Skipping past MAIN reminder for ${medicationName} (ID: ${reminder.id}) - Scheduled: $reminderTimeForLog")
            // Optionally, you might still want to schedule the pre-reminder if its time is in the future,
            // but current logic in ReminderSchedulingWorker likely prevents this earlier.
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_DOSAGE, medicationDosage)
            putExtra(ReminderBroadcastReceiver.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, actualScheduledTimeMillis)
            putExtra(ReminderBroadcastReceiver.EXTRA_IS_INTERVAL, isIntervalType)
            nextDoseTimeForHelperMillis?.let {
                putExtra(ReminderBroadcastReceiver.EXTRA_NEXT_DOSE_TIME_MILLIS, it)
            }
        }

        Log.v(TAG, "MAIN Intent for scheduling: ${intent}, Extras: ${intent.extras?.let { bundle -> bundle.keySet().joinToString { key -> "$key=${bundle.get(key)}" } } ?: "null"})")
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        // Using reminder.id as the request code for the main reminder's PendingIntent
        val pendingIntent = PendingIntent.getBroadcast(context, reminder.id, intent, pendingIntentFlags)
        Log.d(TAG, "MAIN PendingIntent created for reminder ID: ${reminder.id} with request code ${reminder.id} and action: ${intent.action}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                    Log.i(TAG, "Scheduled exact MAIN alarm for ${medicationName} (ID: ${reminder.id}) at $reminderTimeForLog")
                } else {
                    Log.w(TAG, "Cannot schedule exact MAIN alarms. Scheduling inexact for reminder ID: ${reminder.id}.")
                    // Fallback to inexact or setWindow if user disabled exact alarms
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis - TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(2), pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                Log.i(TAG, "Scheduled exact MAIN alarm (legacy) for ${medicationName} (ID: ${reminder.id}) at $reminderTimeForLog")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling MAIN alarm for reminder ID ${reminder.id}", e)
        }
    }

    fun schedulePreReminderServiceTrigger(
        context: Context,
        reminder: MedicationReminder,
        actualMainReminderTimeMillis: Long,
        medicationName: String
    ) {
        val preReminderTimeMillis = actualMainReminderTimeMillis - TimeUnit.MINUTES.toMillis(ReminderSchedulingWorker.PRE_REMINDER_OFFSET_MINUTES)
        val preReminderTimeForLog = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(preReminderTimeMillis), ZoneId.systemDefault())

        if (preReminderTimeMillis < System.currentTimeMillis()) {
            Log.d(TAG, "Pre-reminder time for reminderId ${reminder.id} ($preReminderTimeForLog) is in the past. Skipping pre-reminder scheduling.")
            return
        }

        Log.i(TAG, "Attempting to schedule PRE-REMINDER service trigger for main reminder ID: ${reminder.id} ($medicationName) to trigger at $preReminderTimeForLog")


        Log.d(TAG, "Attempting to get AlarmManager instance for schedulePreReminderServiceTrigger.")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d(TAG, "Successfully got AlarmManager instance for schedulePreReminderServiceTrigger: $alarmManager")
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_PRE_REMINDER_SERVICE
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, actualMainReminderTimeMillis)
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_NAME, medicationName)
        }

        Log.v(TAG, "PRE-REMINDER Intent for scheduling: ${intent}, Extras: ${intent.extras?.let { bundle -> bundle.keySet().joinToString { key -> "$key=${bundle.get(key)}" } } ?: "null"})")
        val preReminderRequestCode = reminder.id + PreReminderForegroundService.PRE_REMINDER_NOTIFICATION_ID_OFFSET
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, preReminderRequestCode, intent, pendingIntentFlags)
        Log.d(TAG, "PRE-REMINDER PendingIntent created for original reminder ID: ${reminder.id} (request Code: $preReminderRequestCode) with action: ${intent.action}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preReminderTimeMillis, pendingIntent)
            } else {
                // For older versions or if exact alarms are denied, setExact might be delayed.
                // Consider setWindow for more flexibility if exactness isn't critical here.
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, preReminderTimeMillis, pendingIntent)
            }
            Log.i(TAG, "Scheduled Pre-Reminder Service trigger for original reminderId ${reminder.id} at $preReminderTimeForLog (request Code: $preReminderRequestCode)")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling pre-reminder service trigger for original reminderId ${reminder.id}", e)
        }
    }

    private fun cancelMainReminderAlarm(context: Context, reminderId: Int) {
        Log.d(TAG, "Attempting to get AlarmManager instance for cancelMainReminderAlarm.")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d(TAG, "Successfully got AlarmManager instance for cancelMainReminderAlarm: $alarmManager")
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
            // Important: The intent used to create the PendingIntent must match the one used to cancel it.
            // If extras were added when creating, they might be needed here too, but usually action and request code are key.
        }
        Log.v(TAG, "MAIN Intent for cancellation: ${intent}")
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        // Using reminderId as the request code, matching how it was created in scheduleNotification
        val pendingIntent = PendingIntent.getBroadcast(context, reminderId, intent, pendingIntentFlags)

        if (pendingIntent != null) {
            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel() // Also cancel the PendingIntent itself
                Log.d(TAG, "Cancelled MAIN AlarmManager alarm for reminder ID: $reminderId (request code: $reminderId)")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling MAIN alarm for reminder ID $reminderId", e)
            }
        } else {
            Log.w(TAG, "No MAIN AlarmManager alarm found to cancel for reminder ID: $reminderId (request code: $reminderId). Intent used for lookup: $intent")
        }
    }

    private fun cancelPreReminderServiceAlarm(context: Context, reminderId: Int) {
        Log.d(TAG, "Attempting to get AlarmManager instance for cancelPreReminderServiceAlarm.")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d(TAG, "Successfully got AlarmManager instance for cancelPreReminderServiceAlarm: $alarmManager")
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_PRE_REMINDER_SERVICE
        }
        Log.v(TAG, "PRE-REMINDER Intent for cancellation: ${intent}")
        val preReminderRequestCode = reminderId + PreReminderForegroundService.PRE_REMINDER_NOTIFICATION_ID_OFFSET
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, preReminderRequestCode, intent, pendingIntentFlags)

        if (pendingIntent != null) {
            try{
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.i(TAG, "Cancelled Pre-Reminder Service alarm for original reminderId $reminderId (request Code: $preReminderRequestCode)")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling PRE-REMINDER alarm for original reminder ID $reminderId (request code: $preReminderRequestCode)", e)
            }
        } else {
            Log.w(TAG, "No Pre-Reminder Service alarm found to cancel for original reminderId $reminderId (request Code: $preReminderRequestCode). Intent used for lookup: $intent")
        }
    }

    fun cancelAllAlarmsForReminder(context: Context, reminderId: Int) {
        Log.d(TAG, "cancelAllAlarmsForReminder called for reminderId: $reminderId")
        cancelMainReminderAlarm(context, reminderId)
        cancelPreReminderServiceAlarm(context, reminderId)

        NotificationHelper.cancelNotification(context, reminderId)
        NotificationHelper.cancelNotification(context, PreReminderForegroundService.getNotificationId(reminderId))

        val stopServiceIntent = Intent(context, PreReminderForegroundService::class.java).apply {
            action = PreReminderForegroundService.ACTION_STOP_PRE_REMINDER
            putExtra(PreReminderForegroundService.EXTRA_SERVICE_REMINDER_ID, reminderId)
        }
        try {
            // startService might throw IllegalStateException if app is in background on Android 12+
            // and doesn't have background start privileges.
            // However, this is an intent to stop an already running foreground service,
            // which is generally permissible.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // context.startForegroundService(stopServiceIntent) // Could also be used
                context.startService(stopServiceIntent)
            } else {
                context.startService(stopServiceIntent)
            }
            Log.d(TAG, "Sent stop intent to PreReminderForegroundService for reminderId: $reminderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error trying to send stop intent to PreReminderForegroundService for reminderId $reminderId: ${e.message}")
        }
    }
}