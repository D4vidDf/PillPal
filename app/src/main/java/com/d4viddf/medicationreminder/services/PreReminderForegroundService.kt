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
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle // Added for Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.EXTRA_REQUEST_PROMOTED_ONGOING
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.d4viddf.medicationreminder.MainActivity
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.common.IntentActionConstants
import com.d4viddf.medicationreminder.common.IntentExtraConstants
import com.d4viddf.medicationreminder.common.NotificationConstants
import com.d4viddf.medicationreminder.common.WorkerConstants
// import com.d4viddf.medicationreminder.notifications.NotificationHelper // Direct refs replaced
// import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver // Direct refs replaced
// import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker // Now using WorkerConstants
import java.util.concurrent.TimeUnit

class PreReminderForegroundService : Service() {

    companion object {
        // ACTION_STOP_PRE_REMINDER moved to IntentActionConstants
        // EXTRAs moved to IntentExtraConstants

        private const val TAG = "PreReminderService"
        const val TOTAL_PRE_REMINDER_DURATION_MINUTES = WorkerConstants.PRE_REMINDER_OFFSET_MINUTES

        fun getNotificationId(reminderId: Int) = reminderId + NotificationConstants.PRE_REMINDER_NOTIFICATION_ID_OFFSET
    }

    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentReminderId: Int = -1
    private var medicationNameForNotification: String = "Medication"
    private var actualTakeTimeMillis: Long = -1L

    private val updateNotificationRunnable = object : Runnable {
        // Removed @RequiresApi(36) from here as the run method itself doesn't directly use API 36 features.
        // The call to updateNotificationContent handles the API level specific logic.
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
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        Log.d(TAG, "PreReminderForegroundService onCreate")
    }

    @RequiresApi(36)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received with action: ${intent?.action}")
        val reminderIdFromIntent = intent?.getIntExtra(IntentExtraConstants.EXTRA_SERVICE_REMINDER_ID, -1) ?: -1

        if (intent?.action == IntentActionConstants.ACTION_STOP_PRE_REMINDER) {
            Log.d(TAG, "Received ACTION_STOP_PRE_REMINDER for reminderId: $reminderIdFromIntent. Current: $currentReminderId")
            if (currentReminderId == reminderIdFromIntent || currentReminderId == -1 || reminderIdFromIntent == -1) { // Also stop if -1 is passed, signifying generic stop
                stopSelfService()
            }
            return START_NOT_STICKY
        }

        val takeTimeFromIntent = intent?.getLongExtra(IntentExtraConstants.EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS, -1L) ?: -1L
        val medNameFromIntent = intent?.getStringExtra(IntentExtraConstants.EXTRA_SERVICE_MEDICATION_NAME) ?: getString(R.string.medications_title)


        if (reminderIdFromIntent == -1 || takeTimeFromIntent == -1L) {
            Log.e(TAG, "Invalid data for starting service: reminderId=$reminderIdFromIntent, takeTime=$takeTimeFromIntent. Stopping.")
            stopSelfService() // Changed to stopSelfService()
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

        val notificationToShow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34 for UpsideDownCake, but docs imply 36 for these features. Let's stick to 36 based on previous findings.
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
    @RequiresApi(36)
    private fun buildStyledNotification(timeRemainingMillis: Long): Notification {
        val minutesRemainingOverall = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)
        val elapsedMinutesInPrePeriod = (TOTAL_PRE_REMINDER_DURATION_MINUTES - minutesRemainingOverall)
            .coerceIn(0L, TOTAL_PRE_REMINDER_DURATION_MINUTES)

        val notificationTapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NotificationConstants.EXTRA_NOTIFICATION_TAP_PREREMINDER_ID, currentReminderId)
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

        val builder = Notification.Builder(this, NotificationConstants.PRE_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // .setColorized(true) // Removed for Live Update compatibility on API 36+
            .setShowWhen(true)
            .setWhen(actualTakeTimeMillis)
            .setCategory(Notification.CATEGORY_PROGRESS) // Changed to CATEGORY_PROGRESS for live updates

        if (Build.VERSION.SDK_INT >= 36) {
            // builder.requestPromotedOngoing(true) // Replaced due to beta version issues
            val extrasBundle = Bundle()
            extrasBundle.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
            builder.addExtras(extrasBundle)
            builder.setColorized(false) // Explicitly set false for API 36+ Live Updates

            val progressStyle = Notification.ProgressStyle()
                .setStyledByProgress(false) // Required for custom segments and tracker
                .setProgress(elapsedMinutesInPrePeriod.toInt()) // Current progress

            // Define segments for the progress bar
            val segmentCount = 4 // Example: 4 segments
            val segmentDuration = TOTAL_PRE_REMINDER_DURATION_MINUTES.toFloat() / segmentCount
            val segments = mutableListOf<Notification.ProgressStyle.Segment>()
            val colors = listOf("#A5D6A7", "#FFF59D", "#FFCC80", "#EF9A9A") // Green, Yellow, Orange, Red

            for (i in 0 until segmentCount) {
                segments.add(
                    Notification.ProgressStyle.Segment(segmentDuration.toInt()) // Corrected to use .toInt()
                        .setColor(colors[i % colors.size].toColorInt())
                )
            }
            progressStyle.setProgressSegments(segments)

            try {
                val trackerIcon = Icon.createWithResource(this, R.drawable.tracker_dot) // Ensure tracker_dot drawable exists
                progressStyle.setProgressTrackerIcon(trackerIcon)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting tracker icon for ProgressStyle: ${e.message}")
            }
            builder.setStyle(progressStyle)

        } else {
            // For older versions, we might keep colorized if it was intended
            builder.setColorized(true)
        }


        try {
            val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
            if (largeIconBitmap != null) builder.setLargeIcon(largeIconBitmap)
        } catch (e: Exception) { Log.e(TAG, "Error setting large icon: ${e.message}") }

        return builder.build()
    }

    private fun buildCompatNotification(timeRemainingMillis: Long): Notification {
        val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)
        val elapsedMinutesInPrePeriod = (TOTAL_PRE_REMINDER_DURATION_MINUTES - minutesRemaining)
            .coerceIn(0L, TOTAL_PRE_REMINDER_DURATION_MINUTES)

        val notificationTapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NotificationConstants.EXTRA_NOTIFICATION_TAP_PREREMINDER_ID, currentReminderId)
        }
        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val tapPendingIntent = PendingIntent.getActivity(this, getNotificationId(currentReminderId) + 2, notificationTapIntent, pendingIntentFlags)

        val titleText = getString(R.string.prereminder_title_medication, medicationNameForNotification)
        val timeRemainingFormatted = formatTimeRemaining(timeRemainingMillis)
        val contentText = if (minutesRemaining > 0) getString(R.string.prereminder_text_approx_minutes_left_plural, minutesRemaining) else getString(R.string.prereminder_text_about_time)


        val compatBuilder = NotificationCompat.Builder(this, NotificationConstants.PRE_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(tapPendingIntent)
            // Removed .setProgress from compat notification
            .setCategory(NotificationCompat.CATEGORY_REMINDER)


        if (minutesRemaining <= 10) {
            val markAsActionIntent = Intent(this, com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver::class.java).apply { // FQDN for ReminderBroadcastReceiver
                action = IntentActionConstants.ACTION_MARK_AS_TAKEN
                putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, currentReminderId)
            }
            val markAsTakenPendingIntent = PendingIntent.getBroadcast(
                this, currentReminderId + 3001, markAsActionIntent, pendingIntentFlags
            )
            compatBuilder.addAction(R.drawable.ic_check, getString(R.string.prereminder_action_taken), markAsTakenPendingIntent)
        }
        return compatBuilder.build()
    }

    @RequiresApi(36) // This annotation might be too broad if the function body also handles <36
    private fun updateNotificationContent(timeRemainingMillis: Long) {
        if (currentReminderId != -1) {
            // The @RequiresApi(36) on the function might be misleading as the function itself
            // can be called on lower API levels, but the styled notification part is conditional.
            // It's better to ensure the internal checks are robust.
            val notification = if (Build.VERSION.SDK_INT >= 36) {
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
        stopForeground(STOP_FOREGROUND_REMOVE)
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