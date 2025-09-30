package com.d4viddf.medicationreminder.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.Notification
import com.d4viddf.medicationreminder.data.repository.NotificationRepository
import com.d4viddf.medicationreminder.ui.MainActivity
import com.d4viddf.medicationreminder.utils.constants.NotificationConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    fun sendLowMedicationNotification(medicationName: String, medicationType: String, medicationColor: String) {
        val notification = Notification(
            title = "Low on $medicationName",
            message = "Time to refill your $medicationName.",
            timestamp = LocalDateTime.now(),
            type = "low_medication",
            icon = medicationType,
            color = medicationColor,
            isRead = false
        )

        coroutineScope.launch {
            notificationRepository.insert(notification)
        }

        showNotification(notification)
    }

    fun sendSecurityAlertNotification(title: String, message: String) {
        val notification = Notification(
            title = title,
            message = message,
            timestamp = LocalDateTime.now(),
            type = "security_alert",
            icon = "security",
            color = null,
            isRead = false
        )

        coroutineScope.launch {
            notificationRepository.insert(notification)
        }

        showNotification(notification)
    }

    private fun showNotification(notification: Notification) {
        val channelId = when (notification.type) {
            "low_medication" -> NotificationConstants.LOW_MEDICATION_CHANNEL_ID
            "security_alert" -> NotificationConstants.MEDICATION_ALERTS_CHANNEL_ID
            else -> return // Or a default channel
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = IntentActionConstants.ACTION_OPEN_NOTIFICATIONS_SCREEN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notification.id, // Use notification id for a unique pending intent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_medication)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(notification.timestamp.hashCode(), builder.build())
        }
    }
}