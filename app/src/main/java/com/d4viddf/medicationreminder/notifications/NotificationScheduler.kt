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
        private const val TAG = "NotificationSchedLog" // Updated TAG
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        private fun formatMillisToDateTimeString(millis: Long?): String {
            if (millis == null) return "null"
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(dateTimeFormatter)
        }
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
        Log.d(TAG, "scheduleNotification called with: reminder.id=${reminder.id}, medicationName='$medicationName', medicationDosage='$medicationDosage', isIntervalType=$isIntervalType, nextDoseTimeForHelperMillis=${formatMillisToDateTimeString(nextDoseTimeForHelperMillis)} (${nextDoseTimeForHelperMillis ?: "null"}), actualScheduledTimeMillis=${formatMillisToDateTimeString(actualScheduledTimeMillis)} ($actualScheduledTimeMillis)")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // val reminderTimeForLog = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(actualScheduledTimeMillis), ZoneId.systemDefault()) // Replaced by helper

        Log.i(TAG, "Attempting to schedule MAIN reminder for ID: ${reminder.id} ('$medicationName') at ${formatMillisToDateTimeString(actualScheduledTimeMillis)}")

        if (actualScheduledTimeMillis < System.currentTimeMillis()) {
            Log.w(TAG, "Skipping past MAIN reminder for '$medicationName' (ID: ${reminder.id}) - Scheduled: ${formatMillisToDateTimeString(actualScheduledTimeMillis)}")
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
        val intentExtrasString = intent.extras?.let { bundle -> bundle.keySet().joinToString { key -> "$key=${bundle.get(key)}" } } ?: "null"
        Log.d(TAG, "MAIN Intent created: action=${intent.action}, extras=[$intentExtrasString]")

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        // Using reminder.id as the request code for the main reminder's PendingIntent
        val pendingIntent = PendingIntent.getBroadcast(context, reminder.id, intent, pendingIntentFlags)
        Log.d(TAG, "MAIN PendingIntent created: request_code=${reminder.id}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                    Log.i(TAG, "MAIN alarm set via setExactAndAllowWhileIdle for ID ${reminder.id} at ${formatMillisToDateTimeString(actualScheduledTimeMillis)}")
                } else {
                    Log.w(TAG, "Cannot schedule exact MAIN alarms for ID ${reminder.id}. Scheduling inexact using setWindow.")
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis - TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(2), pendingIntent)
                    Log.i(TAG, "MAIN alarm set via setWindow for ID ${reminder.id} around ${formatMillisToDateTimeString(actualScheduledTimeMillis)}")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                Log.i(TAG, "MAIN alarm set via setExactAndAllowWhileIdle (legacy) for ID ${reminder.id} at ${formatMillisToDateTimeString(actualScheduledTimeMillis)}")
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
        Log.d(TAG, "schedulePreReminderServiceTrigger called with: reminder.id=${reminder.id}, actualMainReminderTimeMillis=${formatMillisToDateTimeString(actualMainReminderTimeMillis)} ($actualMainReminderTimeMillis), medicationName='$medicationName'. Calculated preReminderTimeMillis=${formatMillisToDateTimeString(preReminderTimeMillis)} ($preReminderTimeMillis)")

        // val preReminderTimeForLog = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(preReminderTimeMillis), ZoneId.systemDefault()) // Replaced by helper

        if (preReminderTimeMillis < System.currentTimeMillis()) {
            Log.d(TAG, "Pre-reminder time for reminderId ${reminder.id} (${formatMillisToDateTimeString(preReminderTimeMillis)}) is in the past. Skipping pre-reminder scheduling.")
            return
        }

        Log.i(TAG, "Attempting to schedule PRE-REMINDER service trigger for main reminder ID: ${reminder.id} ('$medicationName') to trigger at ${formatMillisToDateTimeString(preReminderTimeMillis)}")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_PRE_REMINDER_SERVICE
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, actualMainReminderTimeMillis)
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_NAME, medicationName)
        }
        val intentExtrasString = intent.extras?.let { bundle -> bundle.keySet().joinToString { key -> "$key=${bundle.get(key)}" } } ?: "null"
        Log.d(TAG, "PRE-REMINDER Intent created: action=${intent.action}, extras=[$intentExtrasString]")

        val preReminderRequestCode = reminder.id + PreReminderForegroundService.PRE_REMINDER_NOTIFICATION_ID_OFFSET
        Log.d(TAG, "PRE-REMINDER preReminderRequestCode: $preReminderRequestCode")

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, preReminderRequestCode, intent, pendingIntentFlags)
        Log.d(TAG, "PRE-REMINDER PendingIntent created for original reminder ID: ${reminder.id} (request_code: $preReminderRequestCode)")

        try {
            var setMethodUsed = "setExact" // Default for older versions or when exact alarms are off
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preReminderTimeMillis, pendingIntent)
                setMethodUsed = "setExactAndAllowWhileIdle"
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // setExactAndAllowWhileIdle is API 23, setExact is API 19
                 alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preReminderTimeMillis, pendingIntent) // Prefer this if available and exact not scheduleable by S+
                 setMethodUsed = "setExactAndAllowWhileIdle (fallback for S+ no permission or pre-S)"
            }
             else { // API < M
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, preReminderTimeMillis, pendingIntent)
            }
            Log.i(TAG, "PRE-REMINDER alarm set via $setMethodUsed for original reminderId ${reminder.id} at ${formatMillisToDateTimeString(preReminderTimeMillis)} (request_code: $preReminderRequestCode)")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling PRE-REMINDER alarm for original reminderId ${reminder.id} (request_code: $preReminderRequestCode)", e)
        }
    }

    private fun cancelMainReminderAlarm(context: Context, reminderId: Int) {
        Log.d(TAG, "cancelMainReminderAlarm called for reminderId: $reminderId")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId) // Added EXTRA_REMINDER_ID
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, reminderId, intent, pendingIntentFlags)

        if (pendingIntent != null) {
            Log.d(TAG, "MAIN PendingIntent found for reminderId: $reminderId. Attempting cancellation.")
            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.i(TAG, "Cancelled MAIN alarm for reminder ID: $reminderId (request_code: $reminderId)")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling MAIN alarm for reminder ID $reminderId", e)
            }
        } else {
            Log.w(TAG, "No MAIN PendingIntent found to cancel for reminder ID: $reminderId (request_code: $reminderId). Intent used for lookup: action=${intent.action}, extras=[${ReminderBroadcastReceiver.EXTRA_REMINDER_ID}=${reminderId}]. Alarm might have already fired or was not set.")
        }
    }

    private fun cancelPreReminderServiceAlarm(context: Context, reminderId: Int) {
        val preReminderRequestCode = reminderId + PreReminderForegroundService.PRE_REMINDER_NOTIFICATION_ID_OFFSET
        Log.d(TAG, "cancelPreReminderServiceAlarm called for original reminderId: $reminderId (preReminderRequestCode: $preReminderRequestCode)")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_PRE_REMINDER_SERVICE
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId) // Added EXTRA_REMINDER_ID
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, preReminderRequestCode, intent, pendingIntentFlags)

        if (pendingIntent != null) {
            Log.d(TAG, "PRE-REMINDER PendingIntent found for request_code: $preReminderRequestCode. Attempting cancellation.")
            try{
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.i(TAG, "Cancelled PRE-REMINDER alarm for original reminderId $reminderId (request_code: $preReminderRequestCode)")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling PRE-REMINDER alarm for original reminder ID $reminderId (request_code: $preReminderRequestCode)", e)
            }
        } else {
            Log.w(TAG, "No PRE-REMINDER PendingIntent found to cancel for original reminderId $reminderId (request_code: $preReminderRequestCode). Intent used for lookup: action=${intent.action}, extras=[${ReminderBroadcastReceiver.EXTRA_REMINDER_ID}=${reminderId}]. Alarm might have already fired or was not set.")
        }
    }

    fun cancelAllAlarmsForReminder(context: Context, reminderId: Int) {
        Log.i(TAG, "cancelAllAlarmsForReminder called for reminderId: $reminderId")
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