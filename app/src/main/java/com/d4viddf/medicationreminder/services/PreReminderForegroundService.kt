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
import com.d4viddf.medicationreminder.data.model.MedicationForm
import com.d4viddf.medicationreminder.utils.constants.IntentActionConstants
import com.d4viddf.medicationreminder.utils.constants.IntentExtraConstants
import com.d4viddf.medicationreminder.utils.constants.NotificationConstants
import com.d4viddf.medicationreminder.utils.constants.WorkerConstants
import com.d4viddf.medicationreminder.utils.HyperIslandUtil
import java.util.concurrent.TimeUnit

class PreReminderForegroundService : Service() {

    companion object {
        private const val TAG = "PreReminderService"
        const val TOTAL_PRE_REMINDER_DURATION_MINUTES = WorkerConstants.PRE_REMINDER_OFFSET_MINUTES
        private const val TEST_HYPERISLAND_NOTIFICATION_ID = 9999
        private const val ACTION_FOCUS_NOTIFICATION_TEST = "miui.focus.action_test"


        fun getNotificationId(reminderId: Int) = reminderId + NotificationConstants.PRE_REMINDER_NOTIFICATION_ID_OFFSET
    }

    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var currentReminderId: Int = -1
    private var medicationNameForNotification: String = "Medication"
    private var medicationColorForNotification: String? = null
    private var medicationFormForNotification: MedicationForm? = null
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
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        Log.d(TAG, "PreReminderForegroundService onCreate")
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received with action: ${intent?.action}")
        val reminderIdFromIntent = intent?.getIntExtra(IntentExtraConstants.EXTRA_SERVICE_REMINDER_ID, -1) ?: -1

        if (intent?.action == IntentActionConstants.ACTION_STOP_PRE_REMINDER) {
            Log.d(TAG, "Received ACTION_STOP_PRE_REMINDER for reminderId: $reminderIdFromIntent. Current: $currentReminderId")
            if (currentReminderId == reminderIdFromIntent || currentReminderId == -1 || reminderIdFromIntent == -1) {
                stopSelfService()
            }
            return START_NOT_STICKY
        }

        val takeTimeFromIntent = intent?.getLongExtra(IntentExtraConstants.EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS, -1L) ?: -1L
        val medNameFromIntent = intent?.getStringExtra(IntentExtraConstants.EXTRA_SERVICE_MEDICATION_NAME) ?: getString(R.string.medications_title)
        val medColorFromIntent = intent?.getStringExtra(IntentExtraConstants.EXTRA_MEDICATION_COLOR)
        val medFormStringFromIntent = intent?.getStringExtra(IntentExtraConstants.EXTRA_MEDICATION_FORM)
        val medFormFromIntent = medFormStringFromIntent?.let { MedicationForm.valueOf(it) }


        if (reminderIdFromIntent == -1 || takeTimeFromIntent == -1L) {
            Log.e(TAG, "Invalid data for starting service: reminderId=$reminderIdFromIntent, takeTime=$takeTimeFromIntent. Stopping.")
            stopSelfService()
            return START_NOT_STICKY
        }

        if (currentReminderId != -1 && currentReminderId != reminderIdFromIntent) {
            Log.w(TAG, "New pre-reminder request for $reminderIdFromIntent while $currentReminderId is active. Stopping old, starting new.")
            handler.removeCallbacks(updateNotificationRunnable)
        }

        currentReminderId = reminderIdFromIntent
        actualTakeTimeMillis = takeTimeFromIntent
        medicationNameForNotification = medNameFromIntent
        medicationColorForNotification = medColorFromIntent
        medicationFormForNotification = medFormFromIntent

        Log.i(TAG, "Starting/Updating PreReminderService for reminderId: $currentReminderId, med: $medicationNameForNotification, takeTime: $actualTakeTimeMillis")

        val initialTimeRemainingMillis = actualTakeTimeMillis - System.currentTimeMillis()
        if (initialTimeRemainingMillis <= TimeUnit.SECONDS.toMillis(20)) {
            Log.w(TAG, "Pre-reminder for $currentReminderId, but actual take time is too close. Not starting foreground.")
            stopSelf()
            return START_NOT_STICKY
        }

        val notificationToShow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // Send the test notification for debugging purposes
            sendTestHyperIslandNotification()
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
            hours > 0 && minutes > 0 -> getString(R.string.time_hr_min, hours, minutes)
            hours > 0 -> getString(R.string.time_hr, hours)
            minutes > 0 -> getString(R.string.time_min, minutes)
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
            .setShowWhen(true)
            .setWhen(actualTakeTimeMillis)
            .setCategory(Notification.CATEGORY_PROGRESS)

        val hyperIslandExtras = HyperIslandUtil.getHyperIslandExtrasBundle(this, medicationNameForNotification, timeRemainingMillis, medicationColorForNotification, medicationFormForNotification)

        // Log the extras bundle for debugging
        val extrasLog = hyperIslandExtras.keySet().joinToString(separator = "\n") { key ->
            "Key: $key, Value: ${hyperIslandExtras.get(key)}"
        }
        Log.d(TAG, "HyperIsland Extras Bundle:\n$extrasLog")

        builder.addExtras(hyperIslandExtras)

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
            Log.e(TAG, "Error setting tracker icon for ProgressStyle: ${e.message}")
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
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        val hyperIslandExtras = HyperIslandUtil.getHyperIslandExtrasBundle(this, medicationNameForNotification, timeRemainingMillis, medicationColorForNotification, medicationFormForNotification)
        compatBuilder.addExtras(hyperIslandExtras)

        if (minutesRemaining <= 10) {
            val markAsActionIntent = Intent(this, com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver::class.java).apply {
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

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun sendTestHyperIslandNotification() {
        Log.d(TAG, "Sending Test HyperIsland Notification")
        val islandParams = """
        {
            "param_v2": {
                "protocol": 1,
                "business":"taxi",
                "enableFloat": true,
                "updatable": true,
                "ticker": "ticker",
                "tickerPic": "miui.focus.pic_ticker",
                "aodTitle": "aodTitle",
                "aodPic": "miui.focus.pic_aod",
                "param_island": {
                    "islandProperty": 1,
                    "bigIslandArea": {
                        "imageTextInfoLeft": {
                            "type": 1,
                            "picInfo": {
                                "type": 1,
                                "pic": "miui.focus.pic_imageText"
                            },
                            "textInfo": {
                                "frontTitle": "Charging",
                                "title": "24%",
                                "content": "5 min left",
                                "useHighLight": false
                            }
                        },
                        "picInfo": {
                            "type": 1,
                            "pic": "miui.focus.pic_imageText"
                        }
                    },
                    "smallIslandArea": {
                        "picInfo": {
                            "type": 1,
                            "pic": "miui.focus.pic_imageText"
                        }
                    },
                    "shareData": {
                        "title": "share_title"
                    }
                },
                "baseInfo": {
                    "title": "Pickup pending",
                    "content": "Anning Huating District 2, No. 8 Bottom Store Cainiao Station",
                    "colorTitle": "#006EFF",
                    "type": 2
                },
                "hintInfo": {
                    "type": 1,
                    "title": "2 packages",
                    "actionInfo": {
                        "action": "miui.focus.action_test"
                    }
                },
                "extraInfo": {
                    "carType": "YU7",
                    "carColor": "White"
                }
            }
        }
        """

        val builder = Notification.Builder(this, NotificationConstants.PRE_REMINDER_CHANNEL_ID)
            .setContentTitle("Test Notification")
            .setContentText("This is a test")
            .setSmallIcon(R.drawable.ic_stat_medication)

        val bundle = Bundle()
        val actions = Bundle()
        val intent = Intent(ACTION_FOCUS_NOTIFICATION_TEST)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val action = Notification.Action
                .Builder(Icon.createWithResource(this, R.drawable.ic_check), "Test Action", pendingIntent)
                .build()
        actions.putParcelable("miui.focus.action_test", action)

        bundle.putBundle("miui.focus.actions", actions)

        val pics = Bundle()
        pics.putParcelable("miui.focus.pic_imageText", Icon.createWithResource(this, R.drawable.ic_stat_medication))
        pics.putParcelable("miui.focus.pic_highlight", Icon.createWithResource(this, R.mipmap.ic_launcher_round))
        bundle.putBundle("miui.focus.pics", pics)

        bundle.putString("miui.focus.param", islandParams)

        builder.addExtras(bundle)
        val notification = builder.build()

        notificationManager.notify(TEST_HYPERISLAND_NOTIFICATION_ID, notification)
        Log.d(TAG, "Test HyperIsland Notification Sent")
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
                    stopSelfService()
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException updating notification for ${getNotificationId(currentReminderId)}: ${e.message}")
                stopSelfService()
            } catch (e: Exception) {
                Log.e(TAG, "Generic error updating notification for ${getNotificationId(currentReminderId)}: ${e.message}", e)
            }
        }
    }

    private fun stopSelfService() {
        Log.i(TAG, "Stopping PreReminderForegroundService for reminderId: $currentReminderId")
        handler.removeCallbacks(updateNotificationRunnable)
        notificationManager.cancel(TEST_HYPERISLAND_NOTIFICATION_ID) // Also cancel the test notification
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