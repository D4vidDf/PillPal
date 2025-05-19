package com.d4viddf.medicationreminder.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver
import com.d4viddf.medicationreminder.services.PreReminderForegroundService // Importa el servicio
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker // Para la constante
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
        private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        // El offset ya está en ReminderSchedulingWorker, pero puede ser útil aquí también si es necesario
        // const val PRE_REMINDER_OFFSET_MINUTES = 60L
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
        val reminderTimeForLog = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(actualScheduledTimeMillis), ZoneId.systemDefault())

        Log.i(TAG, "Attempting to schedule MAIN reminder for ID: ${reminder.id} ($medicationName) at $reminderTimeForLog")

        if (actualScheduledTimeMillis < System.currentTimeMillis()) {
            Log.w(TAG, "Skipping past MAIN reminder for ${medicationName} (ID: ${reminder.id}) - Scheduled: $reminderTimeForLog")
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

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, reminder.id, intent, pendingIntentFlags)
        Log.d(TAG, "MAIN PendingIntent created for reminder ID: ${reminder.id} with action: ${intent.action}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                    Log.i(TAG, "Scheduled exact MAIN alarm for ${medicationName} (ID: ${reminder.id}) at $reminderTimeForLog")
                } else {
                    Log.w(TAG, "Cannot schedule exact MAIN alarms. Scheduling inexact for reminder ID: ${reminder.id}.")
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis - TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(2), pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                Log.i(TAG, "Scheduled exact MAIN alarm (legacy) for ${medicationName} (ID: ${reminder.id}) at $reminderTimeForLog")
            }
        } catch (e: Exception) { // Captura más general para robustez
            Log.e(TAG, "Error scheduling MAIN alarm for reminder ID ${reminder.id}", e)
            // No relanzar aquí podría ser mejor para que el worker no falle por esto si es un error aislado
        }
    }

    fun schedulePreReminderServiceTrigger(
        context: Context,
        reminder: MedicationReminder, // El recordatorio principal
        actualMainReminderTimeMillis: Long, // La hora de la toma principal
        medicationName: String
    ) {
        val preReminderTimeMillis = actualMainReminderTimeMillis - TimeUnit.MINUTES.toMillis(ReminderSchedulingWorker.PRE_REMINDER_OFFSET_MINUTES)
        val preReminderTimeForLog = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(preReminderTimeMillis), ZoneId.systemDefault())

        if (preReminderTimeMillis < System.currentTimeMillis()) {
            Log.d(TAG, "Pre-reminder time for reminderId ${reminder.id} ($preReminderTimeForLog) is in the past. Skipping.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_PRE_REMINDER_SERVICE
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id) // ID del recordatorio original
            putExtra(ReminderBroadcastReceiver.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, actualMainReminderTimeMillis) // Hora de la toma principal
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_NAME, medicationName)
        }

        val preReminderRequestCode = reminder.id + PreReminderForegroundService.PRE_REMINDER_NOTIFICATION_ID_OFFSET // Asegura un requestCode único
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, preReminderRequestCode, intent, pendingIntentFlags)
        Log.d(TAG, "PRE-REMINDER PendingIntent created for original reminder ID: ${reminder.id} (requestCode: $preReminderRequestCode) with action: ${intent.action}")


        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preReminderTimeMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, preReminderTimeMillis, pendingIntent) // setExact para <S o si no puede exactas
            }
            Log.i(TAG, "Scheduled Pre-Reminder Service trigger for original reminderId ${reminder.id} at $preReminderTimeForLog (requestCode: $preReminderRequestCode)")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling pre-reminder service trigger for original reminderId ${reminder.id}", e)
        }
    }

    // Cancela la alarma principal del recordatorio
    private fun cancelMainReminderAlarm(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
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
                Log.d(TAG, "Cancelled MAIN AlarmManager alarm for reminder ID: $reminderId")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling MAIN alarm for reminder ID $reminderId", e)
            }
        } else {
            Log.w(TAG, "No MAIN AlarmManager alarm found to cancel for reminder ID: $reminderId")
        }
    }

    // Cancela la alarma del pre-recordatorio
    private fun cancelPreReminderServiceAlarm(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_PRE_REMINDER_SERVICE
        }
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
                Log.i(TAG, "Cancelled Pre-Reminder Service alarm for original reminderId $reminderId (requestCode: $preReminderRequestCode)")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling PRE-REMINDER alarm for original reminder ID $reminderId", e)
            }
        } else {
            Log.w(TAG, "No Pre-Reminder Service alarm found to cancel for original reminderId $reminderId (requestCode: $preReminderRequestCode)")
        }
    }

    // Cancela todas las alarmas asociadas y la notificación UI
    fun cancelAllAlarmsForReminder(context: Context, reminderId: Int) {
        Log.d(TAG, "cancelAllAlarmsForReminder called for reminderId: $reminderId")
        cancelMainReminderAlarm(context, reminderId)
        cancelPreReminderServiceAlarm(context, reminderId)

        NotificationHelper.cancelNotification(context, reminderId) // Cancela notificación principal
        NotificationHelper.cancelNotification(context, PreReminderForegroundService.getNotificationId(reminderId)) // Cancela notificación del servicio

        // Detener el servicio en primer plano si está corriendo para este reminderId
        val stopServiceIntent = Intent(context, PreReminderForegroundService::class.java).apply {
            action = PreReminderForegroundService.ACTION_STOP_PRE_REMINDER
            putExtra(PreReminderForegroundService.EXTRA_SERVICE_REMINDER_ID, reminderId)
        }
        try {
            context.startService(stopServiceIntent)
            Log.d(TAG, "Sent stop intent to PreReminderForegroundService for reminderId: $reminderId")
        } catch (e: Exception) {
            // Si el servicio no se puede iniciar (ej. app en segundo plano en Android 12+ sin permisos), esto podría fallar.
            // El servicio debería detenerse solo de todas formas cuando llegue su hora o si su notificación se cancela.
            Log.e(TAG, "Error trying to send stop intent to PreReminderForegroundService for reminderId $reminderId: ${e.message}")
        }
    }
}