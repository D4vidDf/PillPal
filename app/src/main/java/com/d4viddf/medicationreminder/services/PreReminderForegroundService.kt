package com.d4viddf.medicationreminder.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.d4viddf.medicationreminder.MainActivity
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver // Import ReminderBroadcastReceiver
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
        const val TOTAL_PRE_REMINDER_DURATION_MINUTES = ReminderSchedulingWorker.PRE_REMINDER_OFFSET_MINUTES

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

            if (timeRemainingMillis <= TimeUnit.SECONDS.toMillis(20)) {
                Log.i(TAG, "Scheduled take time reached or very close. Stopping PreReminderService for reminderId: $currentReminderId")
                stopSelfService()
            } else {
                updateNotificationContent(timeRemainingMillis)
                if (currentReminderId != -1) {
                    handler.postDelayed(this, TimeUnit.MINUTES.toMillis(1))
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
            Log.d(TAG, "Received ACTION_STOP_PRE_REMINDER for reminderId: $reminderIdFromIntent. Current: $currentReminderId")
            if (currentReminderId == reminderIdFromIntent || currentReminderId == -1 || reminderIdFromIntent == -1) { // Also stop if -1 is passed, signifying generic stop
                stopSelfService()
            }
            return START_NOT_STICKY
        }

        val takeTimeFromIntent = intent?.getLongExtra(EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS, -1L) ?: -1L
        val medNameFromIntent = intent?.getStringExtra(EXTRA_SERVICE_MEDICATION_NAME) ?: getString(R.string.medications_title)


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

        Log.i(TAG, "Starting/Updating PreReminderService for reminderId: $currentReminderId, med: $medicationNameForNotification, takeTime: $actualTakeTimeMillis")

        val initialTimeRemainingMillis = actualTakeTimeMillis - System.currentTimeMillis()
        if (initialTimeRemainingMillis <= TimeUnit.SECONDS.toMillis(20)) {
            Log.w(TAG, "Pre-reminder for $currentReminderId, but actual take time is too close. Not starting foreground.")
            stopSelf()
            return START_NOT_STICKY
        }

        val notificationToShow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            buildStyledNotification(initialTimeRemainingMillis)
        } else {
            buildCompatNotification(initialTimeRemainingMillis)
        }
        startForeground(getNotificationId(currentReminderId), notificationToShow)

        handler.removeCallbacks(updateNotificationRunnable)
        handler.post(updateNotificationRunnable)

        return START_STICKY
    }

    private fun formatTimeRemaining(millis: Long): String {
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> getString(R.string.time_hr_min, hours, minutes) // Example: "%1$d hr %2$d min"
            hours > 0 -> getString(R.string.time_hr, hours) // Example: "%1$d hr"
            minutes > 0 -> getString(R.string.time_min, minutes) // Example: "%1$d min"
            else -> getString(R.string.less_than_a_minute)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildStyledNotification(timeRemainingMillis: Long): Notification {
        val minutesRemainingOverall = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)
        val elapsedMinutesInPrePeriod = (TOTAL_PRE_REMINDER_DURATION_MINUTES - minutesRemainingOverall)
            .coerceIn(0L, TOTAL_PRE_REMINDER_DURATION_MINUTES)

        val notificationTapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_tap_prereminder_id", currentReminderId)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val tapPendingIntent = PendingIntent.getActivity(this, getNotificationId(currentReminderId) + 1, notificationTapIntent, pendingIntentFlags)

        val titleText = getString(R.string.prereminder_title_medication, medicationNameForNotification)
        val timeRemainingFormatted = formatTimeRemaining(timeRemainingMillis)

        val contentText = when {
            minutesRemainingOverall > 55 -> getString(R.string.prereminder_text_in_about_hour)
            minutesRemainingOverall > 1 -> getString(R.string.prereminder_text_approx_minutes_left_plural, minutesRemainingOverall)
            minutesRemainingOverall == 1L -> getString(R.string.prereminder_text_approx_minutes_left_singular, minutesRemainingOverall)
            else -> getString(R.string.prereminder_text_about_time)
        }

        val builder = Notification.Builder(this, NotificationHelper.PRE_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColorized(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis().plus(50 * 60* 100))
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setProgress(TOTAL_PRE_REMINDER_DURATION_MINUTES.toInt(), elapsedMinutesInPrePeriod.toInt(), false)

        

        try {
            val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
            if (largeIconBitmap != null) builder.setLargeIcon(largeIconBitmap)
        } catch (e: Exception) { Log.e(TAG, "Error setting large icon: ${e.message}") }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val progressStyle = Notification.ProgressStyle()
                .setStyledByProgress(false)
                .setProgress(elapsedMinutesInPrePeriod.toInt())
            val segmentDuration = TOTAL_PRE_REMINDER_DURATION_MINUTES / 4f
                progressStyle.setProgressSegments(listOf(
                    Notification.ProgressStyle.Segment(segmentDuration.toInt()).setColor("#A5D6A7".toColorInt()),
                    Notification.ProgressStyle.Segment(segmentDuration.toInt()).setColor("#FFF59D".toColorInt()),
                    Notification.ProgressStyle.Segment(segmentDuration.toInt()).setColor("#FFCC80".toColorInt()),
                    Notification.ProgressStyle.Segment(segmentDuration.toInt()).setColor("#EF9A9A".toColorInt())
                ))

            try {
                val trackerIcon = Icon.createWithResource(this, R.drawable.tracker_dot)
                progressStyle.setProgressTrackerIcon(trackerIcon)
            } catch (e: Exception) { Log.e(TAG, "Error setting tracker icon: ${e.message}") }
            builder.setStyle(progressStyle)
        }

        if (minutesRemainingOverall <= 10) { // Add "Taken" action in the last 10 minutes
            val markAsActionIntent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
                action = NotificationHelper.ACTION_MARK_AS_TAKEN
                putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, currentReminderId)
            }
            val markAsTakenPendingIntent = PendingIntent.getBroadcast(
                this, currentReminderId + 3000, markAsActionIntent, pendingIntentFlags
            )
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_check_circle),
                    getString(R.string.prereminder_action_taken),
                    markAsTakenPendingIntent
                ).build()
            )
        }
        return builder.build()
    }

    private fun buildCompatNotification(timeRemainingMillis: Long): Notification {
        val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)
        val elapsedMinutesInPrePeriod = (TOTAL_PRE_REMINDER_DURATION_MINUTES - minutesRemaining)
            .coerceIn(0L, TOTAL_PRE_REMINDER_DURATION_MINUTES)

        val notificationTapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_tap_prereminder_id", currentReminderId)
        }
        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val tapPendingIntent = PendingIntent.getActivity(this, getNotificationId(currentReminderId) + 2, notificationTapIntent, pendingIntentFlags)

        val titleText = getString(R.string.prereminder_title_medication, medicationNameForNotification)
        val timeRemainingFormatted = formatTimeRemaining(timeRemainingMillis)
        val contentText = if (minutesRemaining > 0) getString(R.string.prereminder_text_approx_minutes_left_plural, minutesRemaining) else getString(R.string.prereminder_text_about_time)


        val compatBuilder = NotificationCompat.Builder(this, NotificationHelper.PRE_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(tapPendingIntent)
            .setProgress(TOTAL_PRE_REMINDER_DURATION_MINUTES.toInt(), elapsedMinutesInPrePeriod.toInt(), false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)


        if (minutesRemaining <= 10) {
            val markAsActionIntent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
                action = NotificationHelper.ACTION_MARK_AS_TAKEN
                putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, currentReminderId)
            }
            val markAsTakenPendingIntent = PendingIntent.getBroadcast(
                this, currentReminderId + 3001, markAsActionIntent, pendingIntentFlags
            )
            compatBuilder.addAction(R.drawable.ic_check_circle, getString(R.string.prereminder_action_taken), markAsTakenPendingIntent)
        }
        return compatBuilder.build()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun updateNotificationContent(timeRemainingMillis: Long) {
        if (currentReminderId != -1) {
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                buildStyledNotification(timeRemainingMillis)
            } else {
                buildCompatNotification(timeRemainingMillis)
            }
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(getNotificationId(currentReminderId), notification)
                } else {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot update notification.")
                    stopSelfService() // Stop if we can't show/update notifications
                }
            } catch (e: SecurityException) { // Catching SecurityException specifically if POST_NOTIFICATIONS is revoked mid-service
                Log.e(TAG, "SecurityException updating notification for ${getNotificationId(currentReminderId)}: ${e.message}")
                stopSelfService()
            } catch (e: Exception) {
                Log.e(TAG, "Generic error updating notification for ${getNotificationId(currentReminderId)}: ${e.message}", e)
                // Optionally stop service here too, or let it try again if it's a transient issue.
            }
        }
    }

    private fun stopSelfService() {
        Log.i(TAG, "Stopping PreReminderForegroundService for reminderId: $currentReminderId")
        handler.removeCallbacks(updateNotificationRunnable)
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
        currentReminderId = -1
        actualTakeTimeMillis = -1L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateNotificationRunnable)
        Log.d(TAG, "PreReminderForegroundService onDestroy for (last known) reminderId: $currentReminderId")
    }
}