// NotificationScheduler.kt
package com.d4viddf.medicationreminder.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor() {

    companion object {
        private const val TAG = "NotificationScheduler"
        private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    fun scheduleNotification(
        context: Context,
        reminder: MedicationReminder,
        medicationName: String,
        medicationDosage: String
        // Removed isIntervalType and nextDoseTimeMillis from here,
        // as this information should be derived from the reminder/schedule
        // when the receiver gets the reminderId and fetches its details.
        // Or, if ReminderSchedulingWorker calculates them, it can pass them to this method
        // to be then passed to the receiver. For now, simplifying to focus on actualReminderTime.
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderDateTime: LocalDateTime
        val reminderTimeMillis: Long

        try {
            reminderDateTime = LocalDateTime.parse(reminder.reminderTime, storableDateTimeFormatter)
            reminderTimeMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing reminderTime '${reminder.reminderTime}' for reminder ID ${reminder.id}", e)
            return // Cannot schedule if time is invalid
        }

        if (reminderTimeMillis < System.currentTimeMillis()) {
            Log.w(TAG, "Skipping past reminder for ${medicationName} at ${reminder.reminderTime} (ID: ${reminder.id})")
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_DOSAGE, medicationDosage)
            putExtra(ReminderBroadcastReceiver.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, reminderTimeMillis) // <-- PASS THE ACTUAL TIME
            // TODO: If needed, pass isIntervalType and calculate/pass nextDoseTimeMillis from ReminderSchedulingWorker here
            // putExtra(ReminderBroadcastReceiver.EXTRA_IS_INTERVAL, schedule.scheduleType == ScheduleType.INTERVAL)
            // putExtra(ReminderBroadcastReceiver.EXTRA_NEXT_DOSE_TIME_MILLIS, calculateNextDose(reminderDateTime, schedule))
        }

        Log.d(TAG, "Scheduling notification for reminder with DB ID: ${reminder.id}, Name: $medicationName") // Add log for ID

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent)
                    Log.d(TAG, "Scheduled exact alarm for ${medicationName} at ${reminder.reminderTime} (ID: ${reminder.id})")
                } else {
                    Log.w(TAG, "Cannot schedule exact alarms. Scheduling inexact for reminder ID: ${reminder.id}.")
                    alarmManager.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeMillis - (2 * 60 * 1000), // 2 min window before
                        5 * 60 * 1000, // 5 min window
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent)
                Log.d(TAG, "Scheduled exact alarm (legacy) for ${medicationName} at ${reminder.reminderTime} (ID: ${reminder.id})")
            }
        } catch (e: SecurityException) { // Catch SecurityException for setExactAndAllowWhileIdle if permission is missing
            Log.e(TAG, "SecurityException scheduling alarm for reminder ID ${reminder.id}. Check SCHEDULE_EXACT_ALARM permission.", e)
            // Fallback to non-exact alarm if a SecurityException occurs for exact alarm
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                reminderTimeMillis - (2 * 60 * 1000),
                5 * 60 * 1000,
                pendingIntent
            )
            Log.w(TAG, "Fell back to inexact alarm for reminder ID: ${reminder.id} due to SecurityException.")
        } catch (e: Exception) {
            Log.e(TAG, "Generic error scheduling alarm for reminder ID ${reminder.id}", e)
        }
    }

    fun cancelAlarmAndNotification(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled AlarmManager alarm for reminder ID: $reminderId")
        } else {
            Log.d(TAG, "No AlarmManager alarm found to cancel for reminder ID: $reminderId")
        }
        NotificationHelper.cancelNotification(context, reminderId)
    }
}