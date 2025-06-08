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
import com.d4viddf.medicationreminder.utils.FileLogger // Import FileLogger
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
        val initialLog = "scheduleNotification called with: reminder.id=${reminder.id}, medicationName='$medicationName', medicationDosage='$medicationDosage', isIntervalType=$isIntervalType, nextDoseTimeForHelperMillis=${formatMillisToDateTimeString(nextDoseTimeForHelperMillis)} (${nextDoseTimeForHelperMillis ?: "null"}), actualScheduledTimeMillis=${formatMillisToDateTimeString(actualScheduledTimeMillis)} ($actualScheduledTimeMillis)"
        Log.d(TAG, initialLog)
        FileLogger.log(TAG, initialLog)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val attemptScheduleLog = "Attempting to schedule MAIN reminder for ID: ${reminder.id} ('$medicationName') at ${formatMillisToDateTimeString(actualScheduledTimeMillis)}"
        Log.i(TAG, attemptScheduleLog)
        FileLogger.log(TAG, attemptScheduleLog)

        if (actualScheduledTimeMillis < System.currentTimeMillis()) {
            val skipLog = "Skipping past MAIN reminder for '$medicationName' (ID: ${reminder.id}) - Scheduled: ${formatMillisToDateTimeString(actualScheduledTimeMillis)}"
            Log.w(TAG, skipLog)
            FileLogger.log(TAG, skipLog)
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
        val intentCreatedLog = "MAIN Intent created: action=${intent.action}, extras=[$intentExtrasString]"
        Log.d(TAG, intentCreatedLog)
        FileLogger.log(TAG, intentCreatedLog)

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, reminder.id, intent, pendingIntentFlags)
        val piCreatedLog = "MAIN PendingIntent created: request_code=${reminder.id}"
        Log.d(TAG, piCreatedLog)
        FileLogger.log(TAG, piCreatedLog)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                    val setExactLog = "MAIN alarm set via setExactAndAllowWhileIdle for ID ${reminder.id} at ${formatMillisToDateTimeString(actualScheduledTimeMillis)}"
                    Log.i(TAG, setExactLog)
                    FileLogger.log(TAG, setExactLog)
                } else {
                    val cannotScheduleLog = "Cannot schedule exact MAIN alarms for ID ${reminder.id}. Scheduling inexact using setWindow."
                    Log.w(TAG, cannotScheduleLog)
                    FileLogger.log(TAG, cannotScheduleLog)
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis - TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(2), pendingIntent)
                    val setWindowLog = "MAIN alarm set via setWindow for ID ${reminder.id} around ${formatMillisToDateTimeString(actualScheduledTimeMillis)}"
                    Log.i(TAG, setWindowLog)
                    FileLogger.log(TAG, setWindowLog)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                val setExactLegacyLog = "MAIN alarm set via setExactAndAllowWhileIdle (legacy) for ID ${reminder.id} at ${formatMillisToDateTimeString(actualScheduledTimeMillis)}"
                Log.i(TAG, setExactLegacyLog)
                FileLogger.log(TAG, setExactLegacyLog)
            }
        } catch (e: Exception) {
            val errorLog = "Error scheduling MAIN alarm for reminder ID ${reminder.id}"
            Log.e(TAG, errorLog, e)
            FileLogger.log(TAG, errorLog, e)
        }
    }

    fun schedulePreReminderServiceTrigger(
        context: Context,
        reminder: MedicationReminder,
        actualMainReminderTimeMillis: Long,
        medicationName: String
    ) {
        val preReminderTimeMillis = actualMainReminderTimeMillis - TimeUnit.MINUTES.toMillis(ReminderSchedulingWorker.PRE_REMINDER_OFFSET_MINUTES)
        val initialPreLog = "schedulePreReminderServiceTrigger called with: reminder.id=${reminder.id}, actualMainReminderTimeMillis=${formatMillisToDateTimeString(actualMainReminderTimeMillis)} ($actualMainReminderTimeMillis), medicationName='$medicationName'. Calculated preReminderTimeMillis=${formatMillisToDateTimeString(preReminderTimeMillis)} ($preReminderTimeMillis)"
        Log.d(TAG, initialPreLog)
        FileLogger.log(TAG, initialPreLog)

        if (preReminderTimeMillis < System.currentTimeMillis()) {
            val skipPreLog = "Pre-reminder time for reminderId ${reminder.id} (${formatMillisToDateTimeString(preReminderTimeMillis)}) is in the past. Skipping pre-reminder scheduling."
            Log.d(TAG, skipPreLog)
            FileLogger.log(TAG, skipPreLog)
            return
        }

        val attemptSchedulePreLog = "Attempting to schedule PRE-REMINDER service trigger for main reminder ID: ${reminder.id} ('$medicationName') to trigger at ${formatMillisToDateTimeString(preReminderTimeMillis)}"
        Log.i(TAG, attemptSchedulePreLog)
        FileLogger.log(TAG, attemptSchedulePreLog)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_PRE_REMINDER_SERVICE
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, actualMainReminderTimeMillis)
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_NAME, medicationName)
        }
        val intentExtrasPreString = intent.extras?.let { bundle -> bundle.keySet().joinToString { key -> "$key=${bundle.get(key)}" } } ?: "null"
        val intentPreCreatedLog = "PRE-REMINDER Intent created: action=${intent.action}, extras=[$intentExtrasPreString]"
        Log.d(TAG, intentPreCreatedLog)
        FileLogger.log(TAG, intentPreCreatedLog)

        val preReminderRequestCode = reminder.id + PreReminderForegroundService.PRE_REMINDER_NOTIFICATION_ID_OFFSET
        val preReqCodeLog = "PRE-REMINDER preReminderRequestCode: $preReminderRequestCode"
        Log.d(TAG, preReqCodeLog)
        FileLogger.log(TAG, preReqCodeLog)

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, preReminderRequestCode, intent, pendingIntentFlags)
        val piPreCreatedLog = "PRE-REMINDER PendingIntent created for original reminder ID: ${reminder.id} (request_code: $preReminderRequestCode)"
        Log.d(TAG, piPreCreatedLog)
        FileLogger.log(TAG, piPreCreatedLog)

        try {
            var setMethodUsed = "setExact"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preReminderTimeMillis, pendingIntent)
                setMethodUsed = "setExactAndAllowWhileIdle"
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                 alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preReminderTimeMillis, pendingIntent)
                 setMethodUsed = "setExactAndAllowWhileIdle (fallback for S+ no permission or pre-S)"
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, preReminderTimeMillis, pendingIntent)
            }
            val setPreAlarmLog = "PRE-REMINDER alarm set via $setMethodUsed for original reminderId ${reminder.id} at ${formatMillisToDateTimeString(preReminderTimeMillis)} (request_code: $preReminderRequestCode)"
            Log.i(TAG, setPreAlarmLog)
            FileLogger.log(TAG, setPreAlarmLog)
        } catch (e: Exception) {
            val errorPreLog = "Error scheduling PRE-REMINDER alarm for original reminderId ${reminder.id} (request_code: $preReminderRequestCode)"
            Log.e(TAG, errorPreLog, e)
            FileLogger.log(TAG, errorPreLog, e)
        }
    }

    private fun cancelMainReminderAlarm(context: Context, reminderId: Int) {
        val cancelMainLog = "cancelMainReminderAlarm called for reminderId: $reminderId"
        Log.d(TAG, cancelMainLog)
        FileLogger.log(TAG, cancelMainLog)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, reminderId, intent, pendingIntentFlags)

        if (pendingIntent != null) {
            val piFoundLog = "MAIN PendingIntent found for reminderId: $reminderId. Attempting cancellation."
            Log.d(TAG, piFoundLog)
            FileLogger.log(TAG, piFoundLog)
            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                val cancelledLog = "Cancelled MAIN alarm for reminder ID: $reminderId (request_code: $reminderId)"
                Log.i(TAG, cancelledLog)
                FileLogger.log(TAG, cancelledLog)
            } catch (e: Exception) {
                val errorCancelLog = "Error cancelling MAIN alarm for reminder ID $reminderId"
                Log.e(TAG, errorCancelLog, e)
                FileLogger.log(TAG, errorCancelLog, e)
            }
        } else {
            val noPiLog = "No MAIN PendingIntent found to cancel for reminder ID: $reminderId (request_code: $reminderId). Intent used for lookup: action=${intent.action}, extras=[${ReminderBroadcastReceiver.EXTRA_REMINDER_ID}=${reminderId}]. Alarm might have already fired or was not set."
            Log.w(TAG, noPiLog)
            FileLogger.log(TAG, noPiLog)
        }
    }

    private fun cancelPreReminderServiceAlarm(context: Context, reminderId: Int) {
        val preReminderRequestCode = reminderId + PreReminderForegroundService.PRE_REMINDER_NOTIFICATION_ID_OFFSET
        val cancelPreLog = "cancelPreReminderServiceAlarm called for original reminderId: $reminderId (preReminderRequestCode: $preReminderRequestCode)"
        Log.d(TAG, cancelPreLog)
        FileLogger.log(TAG, cancelPreLog)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_PRE_REMINDER_SERVICE
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, preReminderRequestCode, intent, pendingIntentFlags)

        if (pendingIntent != null) {
            val piPreFoundLog = "PRE-REMINDER PendingIntent found for request_code: $preReminderRequestCode. Attempting cancellation."
            Log.d(TAG, piPreFoundLog)
            FileLogger.log(TAG, piPreFoundLog)
            try{
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                val cancelledPreLog = "Cancelled PRE-REMINDER alarm for original reminderId $reminderId (request_code: $preReminderRequestCode)"
                Log.i(TAG, cancelledPreLog)
                FileLogger.log(TAG, cancelledPreLog)
            } catch (e: Exception) {
                val errorCancelPreLog = "Error cancelling PRE-REMINDER alarm for original reminder ID $reminderId (request_code: $preReminderRequestCode)"
                Log.e(TAG, errorCancelPreLog, e)
                FileLogger.log(TAG, errorCancelPreLog, e)
            }
        } else {
            val noPiPreLog = "No PRE-REMINDER PendingIntent found to cancel for original reminderId $reminderId (request_code: $preReminderRequestCode). Intent used for lookup: action=${intent.action}, extras=[${ReminderBroadcastReceiver.EXTRA_REMINDER_ID}=${reminderId}]. Alarm might have already fired or was not set."
            Log.w(TAG, noPiPreLog)
            FileLogger.log(TAG, noPiPreLog)
        }
    }

    fun cancelAllAlarmsForReminder(context: Context, reminderId: Int) {
        val cancelAllLog = "cancelAllAlarmsForReminder called for reminderId: $reminderId"
        Log.i(TAG, cancelAllLog)
        FileLogger.log(TAG, cancelAllLog)
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
            val stopIntentLog = "Sent stop intent to PreReminderForegroundService for reminderId: $reminderId"
            Log.d(TAG, stopIntentLog)
            FileLogger.log(TAG, stopIntentLog)
        } catch (e: Exception) {
            val errorStopIntentLog = "Error trying to send stop intent to PreReminderForegroundService for reminderId $reminderId: ${e.message}"
            Log.e(TAG, errorStopIntentLog, e)
            FileLogger.log(TAG, errorStopIntentLog, e)
        }
    }
}