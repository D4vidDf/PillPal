package com.d4viddf.medicationreminder.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import com.d4viddf.medicationreminder.MainActivity
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.notifications.NotificationHelper // Para ACTION_MARK_AS_TAKEN
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver // Para el intent de la acción
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PreReminderForegroundService : Service() {

    companion object {
        const val ACTION_STOP_PRE_REMINDER = "com.d4viddf.medicationreminder.ACTION_STOP_PRE_REMINDER"
        const val EXTRA_SERVICE_REMINDER_ID = "extra_service_reminder_id"
        const val EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS = "extra_service_actual_scheduled_time_millis"
        const val EXTRA_SERVICE_MEDICATION_NAME = "extra_service_medication_name"
        // EXTRA_SERVICE_MEDICATION_DOSAGE no se está usando actualmente en este servicio, pero podría pasarse si es necesario.

        internal const val PRE_REMINDER_NOTIFICATION_ID_OFFSET = 2000000
        private const val TAG = "PreReminderService"
        private const val MARK_AS_TAKEN_REQUEST_CODE_OFFSET = 3000 // Para diferenciar de otros PendingIntents

        fun getNotificationId(reminderId: Int) = reminderId + PRE_REMINDER_NOTIFICATION_ID_OFFSET
    }

    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentReminderId: Int = -1
    private var medicationNameForNotification: String = "Medication"
    private var actualTakeTimeMillis: Long = -1L
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())


    private val updateNotificationRunnable = object : Runnable {
        override fun run() {
            if (actualTakeTimeMillis <= 0 || currentReminderId == -1) {
                Log.w(TAG, "Invalid state (time or ID), stopping updates. actualTakeTimeMillis=$actualTakeTimeMillis, currentReminderId=$currentReminderId")
                stopSelfService()
                return
            }

            val currentTime = System.currentTimeMillis()
            val timeRemainingMillis = actualTakeTimeMillis - currentTime

            if (timeRemainingMillis <= TimeUnit.SECONDS.toMillis(10)) {
                Log.i(TAG, "Scheduled take time reached or very close. Stopping PreReminderService for reminderId: $currentReminderId")
                stopSelfService()
            } else {
                updateNotification(timeRemainingMillis)
                if (currentReminderId != -1) {
                    handler.postDelayed(this, TimeUnit.MINUTES.toMillis(1))
                    Log.d(TAG, "PreReminderService updated notification for reminderId: $currentReminderId. Time remaining: ${TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis)} min")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Log.d(TAG, "PreReminderForegroundService onCreate")
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA) // Mantenida por startForeground y buildStyledNotification
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received with action: ${intent?.action}")

        val reminderIdFromIntent = intent?.getIntExtra(EXTRA_SERVICE_REMINDER_ID, -1) ?: -1

        if (intent?.action == ACTION_STOP_PRE_REMINDER) {
            Log.d(TAG, "Received ACTION_STOP_PRE_REMINDER for reminderId: $reminderIdFromIntent. Current service processing reminderId: $currentReminderId")
            if (currentReminderId == reminderIdFromIntent || reminderIdFromIntent == -1) {
                stopSelfService()
            }
            return START_NOT_STICKY
        }

        val takeTimeFromIntent = intent?.getLongExtra(EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS, -1L) ?: -1L
        val medNameFromIntent = intent?.getStringExtra(EXTRA_SERVICE_MEDICATION_NAME) ?: "Medication"

        if (reminderIdFromIntent == -1 || takeTimeFromIntent == -1L) {
            Log.e(TAG, "Invalid data for starting service: reminderId=$reminderIdFromIntent, takeTime=$takeTimeFromIntent. Stopping.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (currentReminderId != -1 && currentReminderId != reminderIdFromIntent) {
            Log.w(TAG, "New pre-reminder request for $reminderIdFromIntent while $currentReminderId is active. Stopping old, starting new.")
            handler.removeCallbacks(updateNotificationRunnable)
        }

        currentReminderId = reminderIdFromIntent
        actualTakeTimeMillis = takeTimeFromIntent
        medicationNameForNotification = medNameFromIntent

        Log.i(TAG, "Starting/Updating PreReminderForegroundService for reminderId: $currentReminderId, medication: $medicationNameForNotification, actualTakeTime: $actualTakeTimeMillis (${timeFormatter.format(Date(actualTakeTimeMillis))})")

        val initialTimeRemainingMillis = actualTakeTimeMillis - System.currentTimeMillis()
        if (initialTimeRemainingMillis <= TimeUnit.SECONDS.toMillis(10)) {
            Log.w(TAG, "Pre-reminder start requested for $currentReminderId, but actual take time is in the past or too close. Not starting foreground service.")
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            buildStyledNotification(initialTimeRemainingMillis)
        } else {
            buildCompatNotification(initialTimeRemainingMillis)
        }
        startForeground(getNotificationId(currentReminderId), initialNotification)

        handler.removeCallbacks(updateNotificationRunnable)
        handler.post(updateNotificationRunnable)

        return START_STICKY
    }

    private fun createMarkAsTakenPendingIntent(reminderId: Int): PendingIntent {
        val markAsActionIntent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
            action = NotificationHelper.ACTION_MARK_AS_TAKEN // Usar la misma acción que NotificationHelper
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
            // Podrías pasar EXTRA_SERVICE_MEDICATION_NAME si el receiver lo necesitara para algo específico
            // al marcar como tomada desde el pre-recordatorio, pero usualmente solo el ID es suficiente.
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        // Usar un requestCode diferente para el PendingIntent de "Marcar como Tomada" del servicio
        return PendingIntent.getBroadcast(this, reminderId + MARK_AS_TAKEN_REQUEST_CODE_OFFSET, markAsActionIntent, flags)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildStyledNotification(timeRemainingMillis: Long): Notification {
        val totalPreReminderDurationMinutes = ReminderSchedulingWorker.PRE_REMINDER_OFFSET_MINUTES
        val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)
        val elapsedMinutesInPrePeriod = (totalPreReminderDurationMinutes - minutesRemaining)
            .coerceIn(0L, totalPreReminderDurationMinutes)

        val notificationTapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_tap_prereminder_id", currentReminderId)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val tapPendingIntent = PendingIntent.getActivity(this, getNotificationId(currentReminderId) + 1, notificationTapIntent, pendingIntentFlags)

        val formattedTakeTime = timeFormatter.format(Date(actualTakeTimeMillis))
        val title = "$medicationNameForNotification a las $formattedTakeTime"

        val dynamicText = when {
            minutesRemaining > 55 -> "Recordatorio en aprox. 1 hora"
            minutesRemaining > 5 -> "Aprox. $minutesRemaining minutos restantes"
            minutesRemaining > 1 -> "$minutesRemaining minutos restantes"
            minutesRemaining == 1L -> "En 1 minuto"
            else -> "¡Es casi la hora!"
        }

        val platformBuilder = Notification.Builder(this, NotificationHelper.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(title)
            .setContentText(dynamicText)
            .setOngoing(true)
            .setContentIntent(tapPendingIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(actualTakeTimeMillis)

        // --- Notification.ProgressStyle (Baklava+) ---
        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(false)
        val segments = mutableListOf<Notification.ProgressStyle.Segment>()
        val segmentLength = totalPreReminderDurationMinutes / 4.0f
        segments.add(Notification.ProgressStyle.Segment(segmentLength.toInt()).setColor("#A5D6A7".toColorInt()))
        segments.add(Notification.ProgressStyle.Segment(segmentLength.toInt()).setColor("#FFF59D".toColorInt()))
        segments.add(Notification.ProgressStyle.Segment(segmentLength.toInt()).setColor("#FFCC80".toColorInt()))
        segments.add(Notification.ProgressStyle.Segment(segmentLength.toInt()).setColor("#EF9A9A".toColorInt()))
        progressStyle.setProgressSegments(segments)

        try {
            val trackerIconRes = R.drawable.tracker_dot // Asegúrate que tracker_dot es un VECTOR drawable
            val trackerIcon = Icon.createWithResource(this, trackerIconRes)
            progressStyle.setProgressTrackerIcon(trackerIcon)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting progress tracker icon (is tracker_dot.xml a vector?): ${e.message}")
        }
        platformBuilder.setStyle(progressStyle)
        platformBuilder.setProgress(totalPreReminderDurationMinutes.toInt(), elapsedMinutesInPrePeriod.toInt(), false)

        // Añadir acción "Marcar como Tomada" si quedan 5 minutos o menos
        if (minutesRemaining <= 5) {
            val markAsTakenPendingIntent = createMarkAsTakenPendingIntent(currentReminderId)
            platformBuilder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_check_circle), // Reemplaza con tu icono
                    "Tomada",
                    markAsTakenPendingIntent
                ).build()
            )
        }

        Log.d(TAG, "Built PreReminder (Baklava+) with ProgressStyle: Title='$title', Text='$dynamicText', Progress=${elapsedMinutesInPrePeriod}/${totalPreReminderDurationMinutes}")
        return platformBuilder.build()
    }

    private fun buildCompatNotification(timeRemainingMillis: Long): Notification {
        // No usar totalPreReminderDurationMinutes o elapsedMinutesInPrePeriod si no hay barra de progreso
        val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)

        val notificationTapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_tap_prereminder_id", currentReminderId)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val tapPendingIntent = PendingIntent.getActivity(this, getNotificationId(currentReminderId) + 1, notificationTapIntent, pendingIntentFlags)

        val formattedTakeTime = timeFormatter.format(Date(actualTakeTimeMillis))
        val title = "$medicationNameForNotification a las $formattedTakeTime"

        val text = when {
            minutesRemaining > 55 -> "Recordatorio en aprox. 1 hora"
            minutesRemaining > 5 -> "Aprox. $minutesRemaining minutos restantes"
            minutesRemaining > 1 -> "$minutesRemaining minutos restantes"
            minutesRemaining == 1L -> "En 1 minuto"
            else -> "¡Es casi la hora!"
        }

        val compatBuilder = NotificationCompat.Builder(this, NotificationHelper.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true) // Puede que quieras hacerlo false si no hay progreso y es solo un aviso
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(tapPendingIntent)
            .setShowWhen(true)
            .setWhen(actualTakeTimeMillis)
        // No llamar a .setProgress() para que sea una notificación "normal"

        // Añadir acción "Marcar como Tomada" si quedan 5 minutos o menos
        if (minutesRemaining <= 5) {
            val markAsTakenPendingIntent = createMarkAsTakenPendingIntent(currentReminderId)
            compatBuilder.addAction(R.drawable.ic_check_circle, "Tomada", markAsTakenPendingIntent)
        }

        Log.d(TAG, "Built PreReminder (Compat) normal notification: Title='$title', Text='$text'")
        return compatBuilder.build()
    }

    private fun updateNotification(timeRemainingMillis: Long) {
        if (currentReminderId != -1) {
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                buildStyledNotification(timeRemainingMillis)
            } else {
                buildCompatNotification(timeRemainingMillis)
            }
            try {
                notificationManager.notify(getNotificationId(currentReminderId), notification)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException updating notification for $currentReminderId: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Generic Exception updating notification for $currentReminderId: ${e.message}", e)
            }
        }
    }

    private fun stopSelfService() {
        Log.i(TAG, "Stopping PreReminderForegroundService for reminderId: $currentReminderId")
        handler.removeCallbacks(updateNotificationRunnable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        currentReminderId = -1
        actualTakeTimeMillis = -1L
        medicationNameForNotification = "Medication"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateNotificationRunnable)
        Log.d(TAG, "PreReminderForegroundService onDestroy for (last known) reminderId: $currentReminderId")
    }
}