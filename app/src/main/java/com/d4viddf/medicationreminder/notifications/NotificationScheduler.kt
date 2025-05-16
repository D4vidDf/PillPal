package com.d4viddf.medicationreminder.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor() {

    companion object {
        private const val TAG = "NotificationScheduler"
        private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    fun scheduleNotification(
        context: Context,
        reminder: MedicationReminder,
        medicationName: String,
        medicationDosage: String,
        isIntervalType: Boolean,            // PARÁMETRO AÑADIDO
        nextDoseTimeForHelperMillis: Long?, // PARÁMETRO AÑADIDO
        actualScheduledTimeMillis: Long     // PARÁMETRO AÑADIDO (tiempo de ESTA alarma)
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Ya no necesitamos parsear reminder.reminderTime si actualScheduledTimeMillis es el correcto.
        // val reminderDateTime: LocalDateTime = LocalDateTime.parse(reminder.reminderTime, storableDateTimeFormatter)

        Log.i(
            TAG,
            "Attempting to schedule for reminder.id: ${reminder.id} ($medicationName) at ${
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(actualScheduledTimeMillis),
                    ZoneId.systemDefault()
                )
            }"
        )

        if (actualScheduledTimeMillis < System.currentTimeMillis()) {
            Log.w(
                TAG,
                "Skipping past reminder for ${medicationName} (ID: ${reminder.id}) - Scheduled time: ${
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(actualScheduledTimeMillis),
                        ZoneId.systemDefault()
                    )
                }"
            )
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_DOSAGE, medicationDosage)
            putExtra(ReminderBroadcastReceiver.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, actualScheduledTimeMillis) // Tiempo de esta alarma
            putExtra(ReminderBroadcastReceiver.EXTRA_IS_INTERVAL, isIntervalType) // Pasar si es de intervalo
            nextDoseTimeForHelperMillis?.let { // Pasar el tiempo de la siguiente dosis del intervalo (si aplica)
                putExtra(ReminderBroadcastReceiver.EXTRA_NEXT_DOSE_TIME_MILLIS, it)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id, // ID único del recordatorio para el PendingIntent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "PendingIntent created for reminder ID: ${reminder.id} with action: ${intent.action}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                    Log.i(
                        TAG,
                        "Scheduled exact alarm for ${medicationName} (ID: ${reminder.id}) at ${
                            LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(actualScheduledTimeMillis),
                                ZoneId.systemDefault()
                            )
                        }"
                    )
                } else {
                    Log.w(TAG, "Cannot schedule exact alarms. Scheduling inexact for reminder ID: ${reminder.id}.")
                    // Fallback a una ventana si no se pueden programar alarmas exactas
                    alarmManager.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        actualScheduledTimeMillis - (1 * 60 * 1000), // 1 minuto antes
                        2 * 60 * 1000, // Ventana de 2 minutos
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, actualScheduledTimeMillis, pendingIntent)
                Log.i(
                    TAG,
                    "Scheduled exact alarm (legacy) for ${medicationName} (ID: ${reminder.id}) at ${
                        LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(actualScheduledTimeMillis),
                            ZoneId.systemDefault()
                        )
                    }"
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm for reminder ID ${reminder.id}. Check SCHEDULE_EXACT_ALARM permission.", e)
            // Fallback si hay SecurityException (aunque canScheduleExactAlarms debería prevenirlo)
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                actualScheduledTimeMillis - (1 * 60 * 1000),
                2 * 60 * 1000,
                pendingIntent
            )
            Log.w(TAG, "Fell back to inexact alarm for reminder ID: ${reminder.id} due to SecurityException.")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException (Likely Alarm Limit or other issue) for reminder ID ${reminder.id}: ${e.message}", e)
            // Considera relanzar si quieres que el Worker lo maneje como un fallo
            // throw e;
        } catch (e: Exception) {
            Log.e(TAG, "Generic error scheduling alarm for reminder ID ${reminder.id}", e)
            // Considera relanzar
            // throw e;
        }
    }

    fun cancelAlarmAndNotification(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER // Debe coincidir con la acción del PendingIntent original
        }
        // Para cancelar, el PendingIntent debe coincidir exactamente con el que se usó para programar
        // (mismo action, mismo requestCode, mismo tipo de componente, y los flags deben permitir encontrarlo)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId, // El ID del recordatorio usado como requestCode
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel() // También cancela el PendingIntent para que no se pueda reutilizar accidentalmente
                Log.i(TAG, "Cancelled AlarmManager alarm and PendingIntent for reminder ID: $reminderId")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling alarm for reminder ID $reminderId", e)
            }
        } else {
            Log.w(TAG, "No AlarmManager alarm found to cancel for reminder ID: $reminderId (pendingIntent was null). This can happen if it was already cancelled or never set with this exact ID/Intent.")
        }
        NotificationHelper.cancelNotification(context, reminderId) // Cancela la notificación UI si está visible
    }
}