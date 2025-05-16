package com.d4viddf.medicationreminder.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.d4viddf.medicationreminder.MainActivity
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver
import java.util.concurrent.TimeUnit

object NotificationHelper {

    const val REMINDER_CHANNEL_ID = "medication_reminder_channel"
    const val REMINDER_CHANNEL_NAME = "Medication Reminders"
    const val REMINDER_CHANNEL_DESCRIPTION = "Notifications for medication intake reminders"
    const val ACTION_MARK_AS_TAKEN = "com.d4viddf.medicationreminder.ACTION_MARK_AS_TAKEN"
    private const val TAG = "NotificationHelper"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = REMINDER_CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showReminderNotification(
        context: Context,
        reminderDbId: Int,
        medicationName: String,
        medicationDosage: String,
        isIntervalType: Boolean,
        nextDoseTimeMillisForHelper: Long?, // Timestamp de la siguiente dosis DESPUÉS de esta actual
        actualReminderTimeMillis: Long      // Timestamp de ESTE recordatorio (inicio del intervalo actual)
    ) {
        Log.d(
            TAG,
            "showReminderNotification for ID: $reminderDbId, Name: $medicationName, Interval: $isIntervalType, NextDoseHelper: $nextDoseTimeMillisForHelper, ActualTime: $actualReminderTimeMillis"
        )

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_tap_reminder_id", reminderDbId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, reminderDbId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markAsActionIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_MARK_AS_TAKEN
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderDbId)
        }
        val markAsTakenPendingIntent = PendingIntent.getBroadcast(
            context, reminderDbId + 1000, markAsActionIntent, // requestCode único
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = "Time for your $medicationName!"
        var notificationText = "Take $medicationDosage."

        val notificationBuilder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // **USA UN ICONO DE NOTIFICACIÓN ADECUADO**
            .setContentTitle(notificationTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setWhen(actualReminderTimeMillis)
            .setShowWhen(true)
            .addAction(R.drawable.ic_check_circle, "Mark as Taken", markAsTakenPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)


        // Lógica para la notificación de progreso del intervalo actual
        if (isIntervalType && nextDoseTimeMillisForHelper != null && nextDoseTimeMillisForHelper > actualReminderTimeMillis) {
            val currentTimeMillis = System.currentTimeMillis()
            val totalIntervalDurationMillis = nextDoseTimeMillisForHelper - actualReminderTimeMillis
            var elapsedTimeInIntervalMillis = currentTimeMillis - actualReminderTimeMillis

            // Asegurarse de que el tiempo transcurrido no sea negativo ni mayor que la duración total
            if (elapsedTimeInIntervalMillis < 0) elapsedTimeInIntervalMillis = 0
            if (elapsedTimeInIntervalMillis > totalIntervalDurationMillis) elapsedTimeInIntervalMillis = totalIntervalDurationMillis

            if (totalIntervalDurationMillis > 0) {
                // Usar setProgress(max, progress, false) de NotificationCompat
                // max será la duración total del intervalo.
                // progress será el tiempo transcurrido dentro de ese intervalo.
                notificationBuilder.setProgress(totalIntervalDurationMillis.toInt(), elapsedTimeInIntervalMillis.toInt(), false)
                notificationBuilder.setOngoing(true) // Hacerla "sticky" mientras el intervalo está activo

                Log.d(TAG, "Interval Progress: Max=${totalIntervalDurationMillis.toInt()}, Progress=${elapsedTimeInIntervalMillis.toInt()}")

                // Adicionalmente, podemos añadir texto sobre el tiempo restante del intervalo actual
                val remainingTimeInIntervalMinutes = (nextDoseTimeMillisForHelper - currentTimeMillis) / TimeUnit.MINUTES.toMillis(1)
                if (remainingTimeInIntervalMinutes > 0) {
                    notificationText += " This dose valid for approx. $remainingTimeInIntervalMinutes more min."
                } else if (currentTimeMillis < nextDoseTimeMillisForHelper) {
                    // Si quedan segundos pero no minutos completos
                    notificationText += " This dose window ending soon."
                }
            }
        } else if (isIntervalType) {
            // Si es de intervalo pero no hay nextDoseTimeForHelper, no podemos mostrar progreso de duración.
            Log.d(TAG, "Interval type, but no nextDoseTimeForHelper to calculate progress.")
        }

        notificationBuilder.setContentText(notificationText)
        notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))


        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Notification for $reminderDbId not shown.")
                    return
                }
            }
            try {
                Log.i(TAG, "Showing notification for reminder ID: $reminderDbId with text: \"$notificationText\"")
                notify(reminderDbId, notificationBuilder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException showing notification for $reminderDbId: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Generic Exception showing notification for $reminderDbId: ${e.message}", e)
            }
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
        Log.d(TAG, "Cancelled UI notification for ID: $notificationId")
    }
}