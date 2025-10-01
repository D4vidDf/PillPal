package com.d4viddf.medicationreminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.d4viddf.medicationreminder.utils.constants.IntentActionConstants
import com.d4viddf.medicationreminder.utils.constants.IntentExtraConstants
import com.d4viddf.medicationreminder.di.ReminderReceiverEntryPoint // Using existing entry point
import com.d4viddf.medicationreminder.notifications.NotificationHelper
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
                    val dosageRepository = entryPoint.medicationDosageRepository()
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

                    val activeDosage = dosageRepository.getActiveDosage(medication.id)
                    if (activeDosage == null) {
                        val dosageNotFoundLog = "Active dosage not found for medication ID ${medication.id}. Aborting snooze."
                        Log.e(TAG, dosageNotFoundLog)
                        FileLogger.log(TAG, dosageNotFoundLog)
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

                    // Use the existing scheduleNotification method from NotificationScheduler
                    notificationScheduler.scheduleNotification(
                        context = context,
                        reminder = originalReminder, // Pass the original reminder object
                        medicationName = medication.name,
                        medicationDosage = activeDosage.dosage, // Use the fetched dosage
                        isIntervalType = false, // Snoozed reminder is not an interval type
                        nextDoseTimeForHelperMillis = null, // No next dose for a one-off snooze
                        actualScheduledTimeMillis = snoozeDateTimeMillis // This is the time for the snoozed reminder
                    )

                    val logScheduled = "Scheduled snoozed reminder for ID $reminderId (medication: ${medication.name}) at $snoozeDateTime using scheduleNotification."
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
