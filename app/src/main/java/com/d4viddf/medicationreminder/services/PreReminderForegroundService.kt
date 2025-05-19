package com.d4viddf.medicationreminder.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.d4viddf.medicationreminder.MainActivity
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import java.util.concurrent.TimeUnit
import androidx.core.graphics.toColorInt

class PreReminderForegroundService : Service() {

    companion object {
        const val ACTION_STOP_PRE_REMINDER = "com.d4viddf.medicationreminder.ACTION_STOP_PRE_REMINDER"
        const val EXTRA_SERVICE_REMINDER_ID = "extra_service_reminder_id"
        const val EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS = "extra_service_actual_scheduled_time_millis"
        const val EXTRA_SERVICE_MEDICATION_NAME = "extra_service_medication_name"

        internal const val PRE_REMINDER_NOTIFICATION_ID_OFFSET = 2000000
        private const val TAG = "PreReminderService"

        fun getNotificationId(reminderId: Int) = reminderId + PRE_REMINDER_NOTIFICATION_ID_OFFSET
    }

    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentReminderId: Int = -1
    private var medicationNameForNotification: String = "Medication"
    private var actualTakeTimeMillis: Long = -1L

    private val updateNotificationRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        override fun run() {
            if (actualTakeTimeMillis <= 0 || currentReminderId == -1) {
                Log.w(TAG, "Invalid state (time or ID), stopping updates. actualTakeTimeMillis=$actualTakeTimeMillis, currentReminderId=$currentReminderId")
                stopSelfService()
                return
            }

            val currentTime = System.currentTimeMillis()
            val timeRemainingMillis = actualTakeTimeMillis - currentTime

            if (timeRemainingMillis <= TimeUnit.SECONDS.toMillis(10)) { // Margen pequeño
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

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
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

        Log.i(TAG, "Starting/Updating PreReminderForegroundService for reminderId: $currentReminderId, medication: $medicationNameForNotification, actualTakeTime: $actualTakeTimeMillis")

        val initialTimeRemainingMillis = actualTakeTimeMillis - System.currentTimeMillis()
        if (initialTimeRemainingMillis <= TimeUnit.SECONDS.toMillis(10)) {
            Log.w(TAG, "Pre-reminder start requested for $currentReminderId, but actual take time is in the past or too close. Not starting foreground service.")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(getNotificationId(currentReminderId), buildStyledNotification(initialTimeRemainingMillis))

        handler.removeCallbacks(updateNotificationRunnable)
        handler.post(updateNotificationRunnable)

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildStyledNotification(timeRemainingMillis: Long): Notification {
        val totalPreReminderDurationMinutes = ReminderSchedulingWorker.PRE_REMINDER_OFFSET_MINUTES
        val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)
        val elapsedMinutesInPrePeriod = (totalPreReminderDurationMinutes - minutesRemaining)
            .coerceIn(0L, totalPreReminderDurationMinutes)

        val notificationTapIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("notification_tap_prereminder_id", currentReminderId)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val tapPendingIntent = PendingIntent.getActivity(this, getNotificationId(currentReminderId), notificationTapIntent, pendingIntentFlags)

        val title = medicationNameForNotification
        val dynamicText = when {
            minutesRemaining > 55 -> "Reminder in about an hour"
            minutesRemaining > 45 -> "Reminder in ~${(minutesRemaining / 5) * 5} minutes" // Aproximar a 5 min
            minutesRemaining > 30 -> "Reminder in ~${(minutesRemaining / 5) * 5} minutes"
            minutesRemaining > 15 -> "Approx. $minutesRemaining minutes left"
            minutesRemaining > 5 -> "Coming up: $minutesRemaining min!"
            minutesRemaining > 0 -> "Very soon: $minutesRemaining min!"
            else -> "It's about time!"
        }


        val platformBuilder = Notification.Builder(this, NotificationHelper.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(dynamicText)
            .setContentText(dynamicText)
            .setOngoing(true)
            .setContentIntent(tapPendingIntent)
            .setOnlyAlertOnce(true)

        // --- Notification.ProgressStyle ---
        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(false)
            .setProgress(elapsedMinutesInPrePeriod.toInt())
        val segments = mutableListOf<Notification.ProgressStyle.Segment>()
        segments.add(Notification.ProgressStyle.Segment(15).setColor("#A5D6A7".toColorInt())) // Verde pálido (60-46 min)
        segments.add(Notification.ProgressStyle.Segment(15).setColor("#FFF59D".toColorInt())) // Amarillo pálido (45-31 min)
        segments.add(Notification.ProgressStyle.Segment(15).setColor("#FFCC80".toColorInt())) // Naranja pálido (30-16 min)
        segments.add(Notification.ProgressStyle.Segment(15).setColor("#EF9A9A".toColorInt())) // Rojo pálido (15-0 min)
        progressStyle.setProgressSegments(segments)

        try {
            val trackerIconRes = R.drawable.tracker_dot
            val trackerIcon = Icon.createWithResource(this, trackerIconRes)
            progressStyle.setProgressTrackerIcon(trackerIcon)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting progress tracker icon: ${e.message}")
        }

        platformBuilder.setStyle(progressStyle)
        platformBuilder.setProgress(totalPreReminderDurationMinutes.toInt(), elapsedMinutesInPrePeriod.toInt(), false)

        Log.d(TAG, "Built PreReminder (API S+) with ProgressStyle: Title='$title', Text='$dynamicText', Progress=${elapsedMinutesInPrePeriod}/${totalPreReminderDurationMinutes}")
        return platformBuilder.build()
    }

    private fun buildCompatNotification(timeRemainingMillis: Long): Notification {
        val totalPreReminderDurationMinutes = ReminderSchedulingWorker.PRE_REMINDER_OFFSET_MINUTES
        val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)
        val elapsedMinutesInPrePeriod = (totalPreReminderDurationMinutes - minutesRemaining)
            .coerceIn(0L, totalPreReminderDurationMinutes)

        val notificationTapIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val tapPendingIntent = PendingIntent.getActivity(this, getNotificationId(currentReminderId), notificationTapIntent, pendingIntentFlags)


        val title = "$medicationNameForNotification Reminder"
        val text = if (minutesRemaining > 0) "Coming up in approx. $minutesRemaining min" else "It's about time!"

        val compatBuilder = NotificationCompat.Builder(this, NotificationHelper.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(tapPendingIntent)
            .setProgress(totalPreReminderDurationMinutes.toInt(), elapsedMinutesInPrePeriod.toInt(), false)

        Log.d(TAG, "Built PreReminder (Compat) with setProgress: Title='$title', Text='$text', Progress=${elapsedMinutesInPrePeriod}/${totalPreReminderDurationMinutes}")
        return compatBuilder.build()
    }


    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun updateNotification(timeRemainingMillis: Long) {
        if (currentReminderId != -1) {
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                buildStyledNotification(timeRemainingMillis)
            } else {
                buildCompatNotification(timeRemainingMillis)
            }
            notificationManager.notify(getNotificationId(currentReminderId), notification)
        }
    }

    private fun stopSelfService() {
        Log.i(TAG, "Stopping PreReminderForegroundService for reminderId: $currentReminderId")
        handler.removeCallbacks(updateNotificationRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
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