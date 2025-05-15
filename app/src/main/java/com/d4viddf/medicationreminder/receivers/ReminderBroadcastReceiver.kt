// ReminderBroadcastReceiver.kt
package com.d4viddf.medicationreminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.d4viddf.medicationreminder.di.ReminderReceiverEntryPoint // Import your EntryPoint
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
// import javax.inject.Inject // No longer needed here
// import com.d4viddf.medicationreminder.data.MedicationReminderRepository // Accessed via EntryPoint
// import com.d4viddf.medicationreminder.notifications.NotificationScheduler // Accessed via EntryPoint
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// REMOVE @AndroidEntryPoint if it's a manifest-declared receiver needing field injection this way
class ReminderBroadcastReceiver : BroadcastReceiver() {

    // Dependencies will be fetched via EntryPoint
    // private lateinit var reminderRepository: MedicationReminderRepository
    // private lateinit var notificationScheduler: NotificationScheduler

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val ACTION_SHOW_REMINDER = "com.d4viddf.medicationreminder.ACTION_SHOW_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_MEDICATION_DOSAGE = "extra_medication_dosage"
        const val EXTRA_ACTUAL_REMINDER_TIME_MILLIS = "extra_actual_reminder_time_millis"
        const val EXTRA_IS_INTERVAL = "extra_is_interval"
        const val EXTRA_NEXT_DOSE_TIME_MILLIS = "extra_next_dose_time_millis"
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(TAG, "Context or Intent is null.")
            return
        }

        // Get dependencies using Hilt EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext, // Use application context
            ReminderReceiverEntryPoint::class.java
        )
        val localReminderRepository = entryPoint.reminderRepository()
        val localNotificationScheduler = entryPoint.notificationScheduler()


        val action = intent.action
        Log.d(TAG, "Received action: $action")

        when (action) {
            ACTION_SHOW_REMINDER -> {
                val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
                if (reminderId == -1) { Log.e(TAG, "Invalid reminderId in ACTION_SHOW_REMINDER."); return }
                val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medication"
                val medicationDosage = intent.getStringExtra(EXTRA_MEDICATION_DOSAGE) ?: ""
                val actualReminderTimeMillis = intent.getLongExtra(EXTRA_ACTUAL_REMINDER_TIME_MILLIS, System.currentTimeMillis())
                val isIntervalType = intent.getBooleanExtra(EXTRA_IS_INTERVAL, false)
                val nextDoseTimeMillis = intent.getLongExtra(EXTRA_NEXT_DOSE_TIME_MILLIS, 0L)


                Log.d(TAG, "Showing reminder for ID: $reminderId, Name: $medicationName")

                NotificationHelper.showReminderNotification(
                    context,
                    reminderId,
                    medicationName,
                    medicationDosage,
                    isIntervalType,
                    if(nextDoseTimeMillis > 0) nextDoseTimeMillis else null,
                    actualReminderTimeMillis
                )
            }
            NotificationHelper.ACTION_MARK_AS_TAKEN -> {
                val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
                if (reminderId != -1) {
                    Log.d(TAG, "Marking reminder ID $reminderId as taken.")
                    val pendingResult = goAsync()
                    scope.launch {
                        try {
                            val nowString = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            localReminderRepository.markReminderAsTaken(reminderId, nowString)
                            Log.d(TAG, "Reminder ID $reminderId marked as taken in DB.")
                            localNotificationScheduler.cancelAlarmAndNotification(context, reminderId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error marking reminder $reminderId as taken", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            // TODO: Handle NotificationHelper.ACTION_SNOOZE_REMINDER
        }
    }
}