package com.d4viddf.medicationreminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.d4viddf.medicationreminder.common.IntentActionConstants
import com.d4viddf.medicationreminder.common.IntentExtraConstants
import com.d4viddf.medicationreminder.data.MedicationReminder // Assuming this is the correct data class
import com.d4viddf.medicationreminder.di.ReminderReceiverEntryPoint // Using existing entry point
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.repository.ReminderRepository
import com.d4viddf.medicationreminder.utils.FileLogger
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class SnoozeBroadcastReceiver : BroadcastReceiver() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "SnoozeReceiverLog"
        private const val SNOOZE_MINUTES = 10 // 10 minutes for snooze
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val initialLog = "SnoozeBroadcastReceiver onReceive. Action: ${intent?.action}"
        Log.d(TAG, initialLog)
        FileLogger.log(TAG, initialLog)

        if (context == null || intent == null) {
            val errorNullCtx = "Context or Intent is null. Cannot process snooze broadcast."
            Log.e(TAG, errorNullCtx)
            FileLogger.log(TAG, errorNullCtx)
            return
        }

        if (intent.action == IntentActionConstants.ACTION_SNOOZE_REMINDER) {
            val reminderId = intent.getIntExtra(IntentExtraConstants.EXTRA_REMINDER_ID, -1)
            if (reminderId == -1) {
                val invalidIdLog = "Invalid reminderId (-1) received for snooze. Aborting."
                Log.e(TAG, invalidIdLog)
                FileLogger.log(TAG, invalidIdLog)
                return
            }

            val logReceived = "Snooze action received for reminderId: $reminderId"
            Log.i(TAG, logReceived)
            FileLogger.log(TAG, logReceived)

            // Cancel the current notification
            NotificationHelper.cancelNotification(context, reminderId)
            val logCancelled = "Cancelled original notification for snoozed reminderId: $reminderId"
            Log.d(TAG, logCancelled)
            FileLogger.log(TAG, logCancelled)

            val pendingResult = goAsync()
            scope.launch {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        ReminderReceiverEntryPoint::class.java // Reusing existing Hilt entry point
                    )
                    val reminderRepository = entryPoint.reminderRepository()
                    val medicationRepository = entryPoint.medicationRepository()
                    val notificationScheduler = entryPoint.notificationScheduler()

                    val originalReminder = reminderRepository.getReminderById(reminderId)
                    if (originalReminder == null) {
                        val notFoundLog = "Original reminder with ID $reminderId not found for snooze. Aborting."
                        Log.e(TAG, notFoundLog)
                        FileLogger.log(TAG, notFoundLog)
                        pendingResult.finish()
                        return@launch
                    }

                    val medication = medicationRepository.getMedicationById(originalReminder.medicationId)
                    if (medication == null) {
                        val medNotFoundLog = "Medication with ID ${originalReminder.medicationId} not found for snoozed reminder. Aborting."
                        Log.e(TAG, medNotFoundLog)
                        FileLogger.log(TAG, medNotFoundLog)
                        pendingResult.finish()
                        return@launch
                    }

                    // Calculate new reminder time (10 minutes from now)
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.MINUTE, SNOOZE_MINUTES)
                    val snoozeDateTimeMillis = calendar.timeInMillis
                    val snoozeDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(snoozeDateTimeMillis), ZoneId.systemDefault())

                    val logSnoozeTime = "Snooze time calculated: ${snoozeDateTimeMillis} ($snoozeDateTime) for reminderId: $reminderId"
                    Log.i(TAG, logSnoozeTime)
                    FileLogger.log(TAG, logSnoozeTime)

                    // We are creating a one-time snooze alarm.
                    // The NotificationScheduler typically schedules based on MedicationReminder entities.
                    // For a snooze, we are essentially creating a temporary, one-off reminder.
                    // The existing NotificationScheduler.scheduleNotification is designed to take a full Reminder object.
                    // We might need to adapt or use a more direct way to schedule a single notification intent.

                    // For simplicity, let's try to directly schedule the ACTION_SHOW_REMINDER intent
                    // similar to how NotificationScheduler might do for a single alarm.

                    // Create an intent similar to what NotificationScheduler would prepare
                    val showReminderIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                        action = IntentActionConstants.ACTION_SHOW_REMINDER
                        putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, reminderId) // Pass the original reminder ID
                        putExtra(IntentExtraConstants.EXTRA_MEDICATION_NAME, medication.name)
                        putExtra(IntentExtraConstants.EXTRA_MEDICATION_DOSAGE, medication.dosage) // Assuming dosage is a simple string
                        putExtra(IntentExtraConstants.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, snoozeDateTimeMillis)
                        // For a snoozed reminder, it's not strictly an "interval" type in the original sense,
                        // nor does it have a "next dose time" from a recurring schedule.
                        // We'll pass false for interval and omit nextDoseTimeMillis, or pass 0L.
                        putExtra(IntentExtraConstants.EXTRA_IS_INTERVAL, false)
                        putExtra(IntentExtraConstants.EXTRA_NEXT_DOSE_TIME_MILLIS, 0L)
                    }

                    // Use NotificationScheduler's utility to schedule this specific intent
                    // This assumes NotificationScheduler has a method to schedule a generic PendingIntent,
                    // or we might need to replicate the AlarmManager logic here.
                    // Looking at NotificationScheduler, it uses scheduleExactAlarm.

                    notificationScheduler.scheduleExactAlarm(
                        context,
                        reminderId, // Using original reminder ID for the alarm. This might cause conflicts if not handled.
                                    // Consider using a unique ID for the snooze alarm, but then how to link back?
                                    // For now, re-using reminderId for the alarm might be okay if the previous one is truly cancelled.
                                    // The notification itself uses reminderId, so this should be fine.
                        snoozeDateTimeMillis,
                        showReminderIntent,
                        isPreReminder = false // This is for the main reminder, not a pre-reminder
                    )

                    val logScheduled = "Scheduled snoozed reminder for ID $reminderId at $snoozeDateTime"
                    Log.i(TAG, logScheduled)
                    FileLogger.log(TAG, logScheduled)

                } catch (e: Exception) {
                    val errorLog = "Error processing snooze for reminderId $reminderId: ${e.message}"
                    Log.e(TAG, errorLog, e)
                    FileLogger.log(TAG, errorLog, e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            val unhandledActionLog = "SnoozeBroadcastReceiver received unhandled action: ${intent.action}"
            Log.w(TAG, unhandledActionLog)
            FileLogger.log(TAG, unhandledActionLog)
        }
    }
}
