package com.d4viddf.medicationreminder.notifications

import android.Manifest
import android.app.Notification
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
            Log.d(TAG, "Notification channel $REMINDER_CHANNEL_ID created.")
        }
    }

    fun showReminderNotification(
        context: Context,
        reminderDbId: Int,
        medicationName: String,
        medicationDosage: String,
        isIntervalType: Boolean,
        nextDoseTimeMillisForHelper: Long?,
        actualReminderTimeMillis: Long
    ) {
        Log.i(
            TAG,
            "showReminderNotification called for ID: $reminderDbId, Name: $medicationName, Interval: $isIntervalType, NextDoseHelper: $nextDoseTimeMillisForHelper, ActualTime: $actualReminderTimeMillis"
        )

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_tap_reminder_id", reminderDbId)
        }
        val contentPendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, reminderDbId, contentIntent, contentPendingIntentFlags
        )

        val markAsActionIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_MARK_AS_TAKEN
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderDbId)
        }
        val markAsTakenPendingIntent = PendingIntent.getBroadcast(
            context, reminderDbId + 1000, markAsActionIntent, contentPendingIntentFlags
        )

        val notificationTitle = "Time for your $medicationName!"
        var notificationText = "Take $medicationDosage."

        // Usamos NotificationCompat.Builder para la mayor parte de la compatibilidad
        val notificationCompatBuilder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(notificationTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setWhen(actualReminderTimeMillis)
            .setShowWhen(true)
            .addAction(R.drawable.ic_check_circle, "Mark as Taken", markAsTakenPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)


        // Lógica de progreso para intervalos, intentando la API de Android 16 (API 36) si es posible.
        // La "actividad" que se está rastreando es la ventana de dosificación actual.
        if (isIntervalType && nextDoseTimeMillisForHelper != null && nextDoseTimeMillisForHelper > actualReminderTimeMillis) {
            val startTimeMillis = actualReminderTimeMillis // Inicio de la ventana de dosificación
            val endTimeMillis = nextDoseTimeMillisForHelper  // Fin de la ventana de dosificación
            val durationMillis = endTimeMillis - startTimeMillis

            if (durationMillis > 0) {
                notificationCompatBuilder.setOngoing(true) // Hacerla persistente durante el intervalo

                // Si es Android 12 (API 31) o superior, podemos usar setProgress con cuenta atrás de forma más efectiva
                // o la nueva API si estamos en Vanilla Ice Cream (API 36)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
                    // La API específica Notification.Builder.setProgress(endTime, duration, ongoing)
                    // es para API 36 (Vanilla Ice Cream).
                    // Por ahora, usaremos NotificationCompat.setProgress de forma estándar para
                    // mostrar el progreso actual dentro de la ventana.
                    val currentTimeMillis = System.currentTimeMillis()
                    var elapsedTimeInIntervalMillis = currentTimeMillis - startTimeMillis
                    if (elapsedTimeInIntervalMillis < 0) elapsedTimeInIntervalMillis = 0
                    if (elapsedTimeInIntervalMillis > durationMillis) elapsedTimeInIntervalMillis = durationMillis

                    notificationCompatBuilder.setProgress(durationMillis.toInt(), elapsedTimeInIntervalMillis.toInt(), false)
                    Log.d(TAG, "Interval Progress (Compat): Max=${durationMillis.toInt()}, Progress=${elapsedTimeInIntervalMillis.toInt()}")

                    val remainingTimeInIntervalMinutes = (endTimeMillis - currentTimeMillis) / TimeUnit.MINUTES.toMillis(1)
                    if (remainingTimeInIntervalMinutes > 0) {
                        notificationText += " This dose valid for approx. $remainingTimeInIntervalMinutes more min."
                    } else if (currentTimeMillis < endTimeMillis) {
                        notificationText += " This dose window ending soon."
                    }
                } else {
                    // Para versiones anteriores a S, el setProgress podría no ser tan visual para duraciones.
                    // Podemos simplemente añadir el texto.
                    val timeUntilNextDoseMinutes = (endTimeMillis - System.currentTimeMillis()) / TimeUnit.MINUTES.toMillis(1)
                    if (timeUntilNextDoseMinutes > 0) {
                        notificationText += " Next dose in approx. $timeUntilNextDoseMinutes min."
                    }
                }
            }
        }

        notificationCompatBuilder.setContentText(notificationText)
        notificationCompatBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))

        // Mostrar la notificación
        showNotificationInternal(context, reminderDbId, notificationCompatBuilder.build())
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