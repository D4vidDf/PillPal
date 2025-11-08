package com.d4viddf.medicationreminder.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
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
import com.d4viddf.medicationreminder.data.model.MedicationForm
import com.d4viddf.medicationreminder.utils.HyperIslandUtil
import com.d4viddf.medicationreminder.utils.constants.IntentActionConstants
import com.d4viddf.medicationreminder.utils.constants.IntentExtraConstants
import com.d4viddf.medicationreminder.utils.constants.NotificationConstants
import com.d4viddf.medicationreminder.utils.constants.WorkerConstants
import java.util.concurrent.TimeUnit

class PreReminderForegroundService : Service() {

    companion object {
        private const val TAG = "PreReminderService"
        const val TOTAL_PRE_REMINDER_DURATION_MINUTES = WorkerConstants.PRE_REMINDER_OFFSET_MINUTES

        fun getNotificationId(medicationName: String, scheduledTimeMillis: Long): Int {
            return (medicationName + scheduledTimeMillis).hashCode()
        }
    }

    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentReminderId: Int = -1
    private var currentMedicationId: Int = -1
    private var medicationNameForNotification: String = "Medication"
    private var medicationColorForNotification: String? = null
    private var medicationFormForNotification: MedicationForm? = null
    private var actualTakeTimeMillis: Long = -1L
    private var isHyperIslandDevice: Boolean? = null // Cache for the support check

    private val updateNotificationRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        override fun run() {
            if (actualTakeTimeMillis <= 0 || currentReminderId == -1) {
                stopSelfService()
                return
            }

            val currentTime = System.currentTimeMillis()
            val timeRemainingMillis = actualTakeTimeMillis - currentTime

            if (timeRemainingMillis <= TimeUnit.SECONDS.toMillis(20)) {
                stopSelfService()
            } else {
                // Only update the notification content if it's NOT a HyperIsland device
                if (isHyperIslandDevice != true) {
                    updateNotificationContent(timeRemainingMillis)
                }

                // Always reschedule the runnable to ensure the service stops correctly.
                if (currentReminderId != -1) {
                    handler.postDelayed(this, TimeUnit.MINUTES.toMillis(1))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received. Intent: $intent")
        if (intent != null) {
            val extras = intent.extras
            if (extras != null) {
                for (key in extras.keySet()) {
                    Log.d(TAG, "Extra: $key = ${extras.get(key)}")
                }
            } else {
                Log.d(TAG, "Intent has no extras.")
            }
        }

        val reminderIdFromIntent = intent?.getIntExtra(IntentExtraConstants.EXTRA_SERVICE_REMINDER_ID, -1) ?: -1

        if (intent?.action == IntentActionConstants.ACTION_STOP_PRE_REMINDER) {
            if (currentReminderId == reminderIdFromIntent || currentReminderId == -1 || reminderIdFromIntent == -1) {
                stopSelfService()
            }
            return START_NOT_STICKY
        }

        val medIdFromIntent = intent?.getIntExtra(IntentExtraConstants.EXTRA_MEDICATION_ID, -1) ?: -1
        val takeTimeFromIntent = intent?.getLongExtra(IntentExtraConstants.EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS, -1L) ?: -1L
        val medNameFromIntent = intent?.getStringExtra(IntentExtraConstants.EXTRA_SERVICE_MEDICATION_NAME) ?: getString(R.string.medications_title)
        val medColorFromIntent = intent?.getStringExtra(IntentExtraConstants.EXTRA_MEDICATION_COLOR)
        val medFormStringFromIntent = intent?.getStringExtra(IntentExtraConstants.EXTRA_MEDICATION_FORM)
        val medFormFromIntent = medFormStringFromIntent?.let { MedicationForm.valueOf(it) }

        if (reminderIdFromIntent == -1 || takeTimeFromIntent == -1L || medIdFromIntent == -1) {
            stopSelfService()
            return START_NOT_STICKY
        }

        if (currentReminderId != -1 && currentReminderId != reminderIdFromIntent) {
            handler.removeCallbacks(updateNotificationRunnable)
            isHyperIslandDevice = null // Reset cache for new reminder
        }

        currentReminderId = reminderIdFromIntent
        currentMedicationId = medIdFromIntent
        actualTakeTimeMillis = takeTimeFromIntent
        medicationNameForNotification = medNameFromIntent
        medicationColorForNotification = medColorFromIntent
        medicationFormForNotification = medFormFromIntent

        if (isHyperIslandDevice == null) {
            isHyperIslandDevice = HyperIslandUtil.isSupported(this)
            Log.d(TAG, "HyperIsland support check performed. Result: $isHyperIslandDevice")
        }

        val initialTimeRemainingMillis = actualTakeTimeMillis - System.currentTimeMillis()
        if (initialTimeRemainingMillis <= TimeUnit.SECONDS.toMillis(20)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notificationToShow = buildNotification(initialTimeRemainingMillis)

        startForeground(getNotificationId(medicationNameForNotification, actualTakeTimeMillis), notificationToShow)

        handler.removeCallbacks(updateNotificationRunnable)
        handler.post(updateNotificationRunnable)

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildNotification(timeRemainingMillis: Long): Notification {
        return when {
            isHyperIslandDevice == true -> {
                Log.d(TAG, "Device supports HyperIsland (cached). Building dedicated notification.")
                buildHyperIslandNotification(timeRemainingMillis)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> {
                Log.d(TAG, "Device does not support HyperIsland (cached). Building styled progress notification.")
                buildStyledNotification(timeRemainingMillis)
            }
            else -> {
                Log.d(TAG, "Device does not support HyperIsland (cached). Building compatibility notification.")
                buildCompatNotification(timeRemainingMillis)
            }
        }
    }

    private fun createContentIntent(): PendingIntent {
        val notificationTapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NotificationConstants.EXTRA_NOTIFICATION_TAP_PREREMINDER_ID, currentReminderId)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, getNotificationId(medicationNameForNotification, actualTakeTimeMillis) + 1, notificationTapIntent, pendingIntentFlags)
    }

    private fun formatTimeRemaining(millis: Long): String {
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> getString(R.string.time_hr_min, hours, minutes)
            hours > 0 -> getString(R.string.time_hr, hours)
            minutes > 0 -> getString(R.string.time_min, minutes)
            else -> getString(R.string.less_than_a_minute)
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildHyperIslandNotification(timeRemainingMillis: Long): Notification {
        val titleText = getString(R.string.prereminder_title_medication, medicationNameForNotification.split(" ").firstOrNull() ?: "")
        val contentText = formatTimeRemaining(timeRemainingMillis)

        val builder = Notification.Builder(this, NotificationConstants.PRE_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setContentIntent(createContentIntent())
            .setOngoing(true)

        // Following the Xiaomi example's build order
        // 1. Get the bundle with actions and pictures
        val actionAndPicsBundle = HyperIslandUtil.getHyperIslandActionAndPicsBundle(this, currentReminderId, medicationFormForNotification)
        builder.addExtras(actionAndPicsBundle)

        // 2. Build the notification
        val notification = builder.build()

        // 3. Get the JSON payload and add it directly to the built notification's extras
        val islandParams = HyperIslandUtil.buildHyperIslandJson(this, currentMedicationId, medicationNameForNotification, actualTakeTimeMillis, medicationColorForNotification)
        notification.extras.putString("miui.focus.param", islandParams)
        Log.d(TAG, "Final HyperIsland JSON Payload: $islandParams")


        return notification
    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildStyledNotification(timeRemainingMillis: Long): Notification {
        val minutesRemainingOverall = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)
        val elapsedMinutesInPrePeriod = (TOTAL_PRE_REMINDER_DURATION_MINUTES - minutesRemainingOverall)
            .coerceIn(0L, TOTAL_PRE_REMINDER_DURATION_MINUTES)

        val titleText = getString(R.string.prereminder_title_medication, medicationNameForNotification)
        val contentText = formatTimeRemaining(timeRemainingMillis)

        val builder = Notification.Builder(this, NotificationConstants.PRE_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setContentIntent(createContentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(actualTakeTimeMillis)
            .setCategory(Notification.CATEGORY_PROGRESS)

        val extrasBundle = Bundle()
        extrasBundle.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
        builder.addExtras(extrasBundle)
        builder.setColorized(false)

        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(false)
            .setProgress(elapsedMinutesInPrePeriod.toInt())

        val segmentCount = 4
        val segmentDuration = TOTAL_PRE_REMINDER_DURATION_MINUTES.toFloat() / segmentCount
        val segments = mutableListOf<Notification.ProgressStyle.Segment>()
        val colors = listOf("#A5D6A7", "#FFF59D", "#FFCC80", "#EF9A9A")

        for (i in 0 until segmentCount) {
            segments.add(
                Notification.ProgressStyle.Segment(segmentDuration.toInt())
                    .setColor(colors[i % colors.size].toColorInt())
            )
        }
        progressStyle.setProgressSegments(segments)

        try {
            val trackerIcon = Icon.createWithResource(this, R.drawable.tracker_dot)
            progressStyle.setProgressTrackerIcon(trackerIcon)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting tracker icon: ${e.message}")
        }
        builder.setStyle(progressStyle)

        try {
            val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)
            if (largeIconBitmap != null) builder.setLargeIcon(largeIconBitmap)
        } catch (e: Exception) { Log.e(TAG, "Error setting large icon: ${e.message}") }

        return builder.build()
    }

    private fun buildCompatNotification(timeRemainingMillis: Long): Notification {
        val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis).coerceAtLeast(0)

        val titleText = getString(R.string.prereminder_title_medication, medicationNameForNotification)
        val contentText = formatTimeRemaining(timeRemainingMillis)

        val compatBuilder = NotificationCompat.Builder(this, NotificationConstants.PRE_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setContentIntent(createContentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (minutesRemaining <= 10) {
            val markAsActionIntent = Intent(this, com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver::class.java).apply {
                action = IntentActionConstants.ACTION_MARK_AS_TAKEN
                putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, currentReminderId)
            }
            val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val markAsTakenPendingIntent = PendingIntent.getBroadcast(
                this, currentReminderId + 3001, markAsActionIntent, pendingIntentFlags
            )
            compatBuilder.addAction(R.drawable.ic_check, getString(R.string.prereminder_action_taken), markAsTakenPendingIntent)
        }
        return compatBuilder.build()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun updateNotificationContent(timeRemainingMillis: Long) {
        if (currentReminderId != -1) {
            val notification = buildNotification(timeRemainingMillis)
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(getNotificationId(medicationNameForNotification, actualTakeTimeMillis), notification)
                } else {
                    stopSelfService()
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on update: ${e.message}")
                stopSelfService()
            } catch (e: Exception) {
                Log.e(TAG, "Generic error on update: ${e.message}", e)
            }
        }
    }

    private fun stopSelfService() {
        Log.i(TAG, "Stopping PreReminderForegroundService for reminderId: $currentReminderId")
        handler.removeCallbacks(updateNotificationRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        currentReminderId = -1
        currentMedicationId = -1
        actualTakeTimeMillis = -1L
        isHyperIslandDevice = null // Reset the cache
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateNotificationRunnable)
    }
}