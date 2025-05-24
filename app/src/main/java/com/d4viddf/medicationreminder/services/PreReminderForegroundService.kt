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
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver
// import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker // Not directly used for constant
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

        internal const val PRE_REMINDER_NOTIFICATION_ID_OFFSET = 2000000
        private const val TAG = "PreReminderService"
        private const val MARK_AS_TAKEN_REQUEST_CODE_OFFSET = 3000
        private const val TOTAL_PRE_REMINDER_DURATION_MINUTES = 60L // As per requirement

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

    // @RequiresApi(Build.VERSION_CODES.BAKLAVA) // Removed as we use NotificationCompat
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

        // Use the new unified buildNotification method
        val initialNotification = buildNotification(initialTimeRemainingMillis)
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

    private fun buildNotification(timeRemainingMillis: Long): Notification {
        val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)
        val elapsedTimeMinutes = (TOTAL_PRE_REMINDER_DURATION_MINUTES - minutesRemaining).coerceIn(0L, TOTAL_PRE_REMINDER_DURATION_MINUTES)

        val notificationTapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_tap_prereminder_id", currentReminderId) // Keep distinct extra key
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val tapPendingIntent = PendingIntent.getActivity(this, getNotificationId(currentReminderId) + 1, notificationTapIntent, pendingIntentFlags)

        val formattedTakeTime = timeFormatter.format(Date(actualTakeTimeMillis))
        val title = "$medicationNameForNotification a las $formattedTakeTime"

        val textContent = when {
            minutesRemaining > 1 -> "$minutesRemaining minutos restantes"
            minutesRemaining == 1L -> "En 1 minuto"
            else -> "¡Es casi la hora!"
        }

        val builder = NotificationCompat.Builder(this, NotificationHelper.PRE_REMINDER_CHANNEL_ID) // Use PRE_REMINDER_CHANNEL_ID
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(title)
            .setContentText(textContent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Use PRIORITY_DEFAULT
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(tapPendingIntent)
            .setShowWhen(true)
            .setWhen(actualTakeTimeMillis) // Timestamp is the actual medication time
            .setProgress(TOTAL_PRE_REMINDER_DURATION_MINUTES.toInt(), elapsedTimeMinutes.toInt(), false)

        // Add "Mark as Taken" action if within 5 minutes
        if (minutesRemaining <= 5) {
            val markAsTakenPendingIntent = createMarkAsTakenPendingIntent(currentReminderId)
            builder.addAction(R.drawable.ic_check_circle, "Tomada", markAsTakenPendingIntent)
        }

        Log.d(TAG, "Built PreReminder Notification: Title='$title', Text='$textContent', Progress=${elapsedTimeMinutes}/${TOTAL_PRE_REMINDER_DURATION_MINUTES}")
        return builder.build()
    }

    private fun updateNotification(timeRemainingMillis: Long) {
        if (currentReminderId != -1) {
            val notification = buildNotification(timeRemainingMillis) // Use unified method
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