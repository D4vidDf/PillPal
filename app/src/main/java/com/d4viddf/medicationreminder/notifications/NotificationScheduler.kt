package com.d4viddf.medicationreminder.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver
import com.d4viddf.medicationreminder.services.PreReminderForegroundService
import com.d4viddf.medicationreminder.utils.FileLogger
import com.d4viddf.medicationreminder.utils.constants.IntentActionConstants
import com.d4viddf.medicationreminder.utils.constants.IntentExtraConstants
import com.d4viddf.medicationreminder.utils.constants.NotificationConstants
import com.d4viddf.medicationreminder.utils.constants.WorkerConstants
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class NotificationScheduler @Inject constructor() {

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
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (actualScheduledTimeMillis < System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = IntentActionConstants.ACTION_SHOW_REMINDER
            putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, reminder.id)
            putExtra(IntentExtraConstants.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(IntentExtraConstants.EXTRA_MEDICATION_DOSAGE, medicationDosage)
            putExtra(IntentExtraConstants.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, actualScheduledTimeMillis)
            putExtra(IntentExtraConstants.EXTRA_IS_INTERVAL, isIntervalType)
            nextDoseTimeForHelperMillis?.let {
                putExtra(IntentExtraConstants.EXTRA_NEXT_DOSE_TIME_MILLIS, it)
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, reminder.id, intent, pendingIntentFlags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                } else {
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis - TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(2), pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling MAIN alarm for reminder ID ${reminder.id}", e)
        }
    }

    fun schedulePreReminderServiceTrigger(
        context: Context,
        reminder: MedicationReminder,
        actualMainReminderTimeMillis: Long,
        medicationName: String,
        medicationColor: String?,
        medicationForm: String?
    ) {
        val preReminderTimeMillis = actualMainReminderTimeMillis - TimeUnit.MINUTES.toMillis(WorkerConstants.PRE_REMINDER_OFFSET_MINUTES)

        if (preReminderTimeMillis < System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PreReminderForegroundService::class.java).apply {
            // Service intents don't need an action, but it can be useful for debugging
            action = "START_PRE_REMINDER_SERVICE"
            putExtra(IntentExtraConstants.EXTRA_SERVICE_REMINDER_ID, reminder.id)
            putExtra(IntentExtraConstants.EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS, actualMainReminderTimeMillis)
            putExtra(IntentExtraConstants.EXTRA_SERVICE_MEDICATION_NAME, medicationName)
            putExtra(IntentExtraConstants.EXTRA_MEDICATION_COLOR, medicationColor)
            putExtra(IntentExtraConstants.EXTRA_MEDICATION_FORM, medicationForm)
        }

        val preReminderRequestCode = reminder.id + NotificationConstants.PRE_REMINDER_NOTIFICATION_ID_OFFSET

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, preReminderRequestCode, intent, pendingIntentFlags)
        } else {
            // Fallback for older versions, though the service should handle startForeground itself.
            PendingIntent.getService(context, preReminderRequestCode, intent, pendingIntentFlags)
        }

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
            Log.i(TAG, "PRE-REMINDER alarm set via $setMethodUsed for original reminderId ${reminder.id} at ${formatMillisToDateTimeString(preReminderTimeMillis)} (request_code: $preReminderRequestCode)")
        } catch (e: Exception) {
             Log.e(TAG, "Error scheduling PRE-REMINDER alarm for original reminderId ${reminder.id} (request_code: $preReminderRequestCode)", e)
        }
    }

    private fun cancelMainReminderAlarm(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = IntentActionConstants.ACTION_SHOW_REMINDER
            putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, reminderId, intent, pendingIntentFlags)

        if (pendingIntent != null) {
            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling MAIN alarm for reminder ID $reminderId", e)
            }
        }
    }

    private fun cancelPreReminderServiceAlarm(context: Context, reminderId: Int) {
        val preReminderRequestCode = reminderId + NotificationConstants.PRE_REMINDER_NOTIFICATION_ID_OFFSET
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Intent must match the one used to create the alarm
        val intent = Intent(context, PreReminderForegroundService::class.java).apply {
             action = "START_PRE_REMINDER_SERVICE"
             putExtra(IntentExtraConstants.EXTRA_SERVICE_REMINDER_ID, reminderId)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, preReminderRequestCode, intent, pendingIntentFlags)
        } else {
            PendingIntent.getService(context, preReminderRequestCode, intent, pendingIntentFlags)
        }

        if (pendingIntent != null) {
            try{
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                 Log.i(TAG, "Cancelled PRE-REMINDER alarm for original reminderId $reminderId (request_code: $preReminderRequestCode)")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling PRE-REMINDER alarm for original reminder ID $reminderId (request_code: $preReminderRequestCode)", e)
            }
        }
    }

    fun cancelAllAlarmsForReminder(context: Context, reminderId: Int) {
        cancelMainReminderAlarm(context, reminderId)
        cancelPreReminderServiceAlarm(context, reminderId)

        NotificationHelper.cancelNotification(context, reminderId)
        NotificationHelper.cancelNotification(context, PreReminderForegroundService.getNotificationId(reminderId))

        val stopServiceIntent = Intent(context, PreReminderForegroundService::class.java).apply {
            action = IntentActionConstants.ACTION_STOP_PRE_REMINDER
            putExtra(IntentExtraConstants.EXTRA_SERVICE_REMINDER_ID, reminderId)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startService(stopServiceIntent)
            } else {
                context.startService(stopServiceIntent)
            }
        } catch (e: Exception) {
             Log.e(TAG, "Error trying to send stop intent to PreReminderForegroundService for reminderId $reminderId: ${e.message}", e)
        }
    }

    fun scheduleStockReminder(
        context: Context,
        medicationId: Int,
        medicationName: String,
        reminderType: String,
        days: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = IntentActionConstants.ACTION_SHOW_STOCK_REMINDER
            putExtra(IntentExtraConstants.EXTRA_MEDICATION_ID, medicationId)
            putExtra(IntentExtraConstants.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(IntentExtraConstants.EXTRA_STOCK_REMINDER_TYPE, reminderType)
            putExtra(IntentExtraConstants.EXTRA_STOCK_REMINDER_DAYS, days)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        val pendingIntent = PendingIntent.getBroadcast(context, medicationId, intent, pendingIntentFlags)

        if (pendingIntent != null && reminderType == "low") {
            // Low stock reminder already scheduled
            return
        }

        val newPendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val newPendingIntent = PendingIntent.getBroadcast(context, medicationId, intent, newPendingIntentFlags)

        if (reminderType == "low") {
            val triggerAtMillis = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, newPendingIntent)
        } else {
            if (days > 0) {
                val triggerAtMillis = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
                val intervalMillis = TimeUnit.DAYS.toMillis(days.toLong())
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis, newPendingIntent)
            }
        }
    }
}