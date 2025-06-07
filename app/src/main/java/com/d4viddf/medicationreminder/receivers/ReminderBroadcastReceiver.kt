package com.d4viddf.medicationreminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.di.ReminderReceiverEntryPoint
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.services.PreReminderForegroundService
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    private val job = SupervisorJob() // Create a SupervisorJob for the scope
    private val scope = CoroutineScope(Dispatchers.IO + job) // Create a CoroutineScope with IO dispatcher

    companion object {
        const val ACTION_SHOW_REMINDER = "com.d4viddf.medicationreminder.ACTION_SHOW_REMINDER"
        const val ACTION_TRIGGER_PRE_REMINDER_SERVICE = "com.d4viddf.medicationreminder.ACTION_TRIGGER_PRE_REMINDER_SERVICE"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_MEDICATION_DOSAGE = "extra_medication_dosage"
        const val EXTRA_ACTUAL_REMINDER_TIME_MILLIS = "extra_actual_reminder_time_millis"
        const val EXTRA_IS_INTERVAL = "extra_is_interval"
        const val EXTRA_NEXT_DOSE_TIME_MILLIS = "extra_next_dose_time_millis"
        private const val TAG = "ReminderReceiverLog" // Updated TAG

        private val humanReadableDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        private fun formatMillisToDateTimeString(millis: Long?): String {
            if (millis == null || millis == 0L) return "null"
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(humanReadableDateTimeFormatter)
        }

        private fun getIntentExtrasString(intent: Intent?): String {
            if (intent?.extras == null) return "null"
            return intent.extras?.let { bundle ->
                bundle.keySet().joinToString(", ") { key ->
                    val value = bundle.get(key)
                    val valueString = if (key == EXTRA_ACTUAL_REMINDER_TIME_MILLIS || key == EXTRA_NEXT_DOSE_TIME_MILLIS) {
                        formatMillisToDateTimeString(value as? Long)
                    } else {
                        value.toString()
                    }
                    "$key=$valueString"
                }
            } ?: "null"
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive called. Action: ${intent?.action}, Extras: ${getIntentExtrasString(intent)}")

        if (context == null || intent == null) {
            Log.e(TAG, "Context or Intent is null. Cannot process broadcast.")
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReminderReceiverEntryPoint::class.java
        )
        val localReminderRepository = entryPoint.reminderRepository()
        val localNotificationScheduler = entryPoint.notificationScheduler()
        val localMedicationRepository = entryPoint.medicationRepository()
        val localMedicationTypeRepository = entryPoint.medicationTypeRepository()

        when (intent.action) {
            ACTION_SHOW_REMINDER -> {
                val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
                if (reminderId == -1) { Log.e(TAG, "$ACTION_SHOW_REMINDER: Invalid reminderId. Aborting."); return }

                // Stop PreReminderForegroundService if it was active for this reminder
                val stopServiceIntent = Intent(context, PreReminderForegroundService::class.java).apply {
                    this.action = PreReminderForegroundService.ACTION_STOP_PRE_REMINDER
                    putExtra(PreReminderForegroundService.EXTRA_SERVICE_REMINDER_ID, reminderId)
                }
                context.startService(stopServiceIntent) // Intenta detenerlo

                val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medication"
                val medicationDosage = intent.getStringExtra(EXTRA_MEDICATION_DOSAGE) ?: ""
                val actualReminderTimeMillis = intent.getLongExtra(EXTRA_ACTUAL_REMINDER_TIME_MILLIS, System.currentTimeMillis())
                val isIntervalType = intent.getBooleanExtra(EXTRA_IS_INTERVAL, false)
                val nextDoseTimeMillisExtra = intent.getLongExtra(EXTRA_NEXT_DOSE_TIME_MILLIS, 0L)
                val nextDoseTimeForHelper = if (nextDoseTimeMillisExtra > 0) nextDoseTimeMillisExtra else null

                Log.i(TAG, "$ACTION_SHOW_REMINDER: Extracted: reminderId=$reminderId, medicationName='$medicationName', medicationDosage='$medicationDosage', actualReminderTime=${formatMillisToDateTimeString(actualReminderTimeMillis)}, isIntervalType=$isIntervalType, nextDoseTimeForHelper=${formatMillisToDateTimeString(nextDoseTimeForHelper)}")

                val pendingResult = goAsync()
                scope.launch {
                    var notificationSoundUri: String? = null
                    var reminder: com.d4viddf.medicationreminder.data.Reminder? = null
                    var medicationColorHex: String? = null
                    var medicationTypeName: String? = null
                    try {
                        notificationSoundUri = userPreferencesRepository.notificationSoundUriFlow.firstOrNull()
                        reminder = localReminderRepository.getReminderById(reminderId)

                        if (reminder != null) {
                            val medication = localMedicationRepository.getMedicationById(reminder.medicationId)
                            if (medication != null) {
                                medicationColorHex = medication.color
                                medication.typeId?.let { actualTypeId ->
                                    val medicationType = localMedicationTypeRepository.getMedicationTypeById(actualTypeId)
                                    medicationTypeName = medicationType?.name
                                }
                                Log.d(TAG, "$ACTION_SHOW_REMINDER: Fetched details for notification: Color=$medicationColorHex, TypeName=$medicationTypeName for MedicationId=${medication.id}")
                            } else {
                                Log.w(TAG, "$ACTION_SHOW_REMINDER: Medication not found for ReminderId: $reminderId, MedicationId: ${reminder.medicationId}")
                            }
                        } else {
                            Log.w(TAG, "$ACTION_SHOW_REMINDER: Reminder not found for ID: $reminderId")
                        }

                        Log.d(TAG, "$ACTION_SHOW_REMINDER: Calling NotificationHelper.showReminderNotification with: context, reminderId=$reminderId, medicationName='$medicationName', medicationDosage='$medicationDosage', isIntervalType=$isIntervalType, nextDoseTimeForHelper=${formatMillisToDateTimeString(nextDoseTimeForHelper)}, actualReminderTimeMillis=${formatMillisToDateTimeString(actualReminderTimeMillis)}, notificationSoundUri=$notificationSoundUri, medicationColorHex=$medicationColorHex, medicationTypeName=$medicationTypeName")
                        NotificationHelper.showReminderNotification(
                            context, reminderId, medicationName, medicationDosage,
                            isIntervalType, nextDoseTimeForHelper, actualReminderTimeMillis,
                            notificationSoundUri,
                            medicationColorHex,
                            medicationTypeName
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "$ACTION_SHOW_REMINDER: Error in coroutine for ReminderId: $reminderId", e)
                        // Fallback call might be needed if the primary fails before showing any notification
                        Log.d(TAG, "$ACTION_SHOW_REMINDER: Fallback NotificationHelper.showReminderNotification with: context, reminderId=$reminderId, medicationName='$medicationName', medicationDosage='$medicationDosage', isIntervalType=$isIntervalType, nextDoseTimeForHelper=${formatMillisToDateTimeString(nextDoseTimeForHelper)}, actualReminderTimeMillis=${formatMillisToDateTimeString(actualReminderTimeMillis)}, null, null, null")
                        NotificationHelper.showReminderNotification(
                            context, reminderId, medicationName, medicationDosage,
                            isIntervalType, nextDoseTimeForHelper, actualReminderTimeMillis,
                            null, null, null // Fallback with no sound/color/type
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_TRIGGER_PRE_REMINDER_SERVICE -> {
                val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
                val actualScheduledTimeMillis = intent.getLongExtra(EXTRA_ACTUAL_REMINDER_TIME_MILLIS, -1L)
                val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medication"

                Log.i(TAG, "$ACTION_TRIGGER_PRE_REMINDER_SERVICE: Extracted: reminderId=$reminderId, actualScheduledTime=${formatMillisToDateTimeString(actualScheduledTimeMillis)}, medicationName='$medicationName'")

                if (reminderId != -1 && actualScheduledTimeMillis != -1L) {
                    if (actualScheduledTimeMillis < System.currentTimeMillis()){
                        Log.w(TAG, "$ACTION_TRIGGER_PRE_REMINDER_SERVICE: Pre-reminder trigger for ID $reminderId, but actual scheduled time ${formatMillisToDateTimeString(actualScheduledTimeMillis)} is in the past. Not starting service.")
                        return
                    }

                    val serviceIntent = Intent(context, PreReminderForegroundService::class.java).apply {
                        putExtra(PreReminderForegroundService.EXTRA_SERVICE_REMINDER_ID, reminderId)
                        putExtra(PreReminderForegroundService.EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS, actualScheduledTimeMillis)
                        putExtra(PreReminderForegroundService.EXTRA_SERVICE_MEDICATION_NAME, medicationName)
                    }
                    context.startForegroundService(serviceIntent)
                    Log.i(TAG, "$ACTION_TRIGGER_PRE_REMINDER_SERVICE: Started PreReminderForegroundService for reminderId: $reminderId")
                } else {
                    Log.e(TAG, "$ACTION_TRIGGER_PRE_REMINDER_SERVICE: Invalid data. reminderId=$reminderId, actualScheduledTimeMillis=$actualScheduledTimeMillis. Cannot start service.")
                }
            }

            NotificationHelper.ACTION_MARK_AS_TAKEN -> {
                val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
                Log.i(TAG, "${NotificationHelper.ACTION_MARK_AS_TAKEN}: Received for reminderId=$reminderId")

                if (reminderId != -1) {
                    val pendingResult = goAsync()
                    scope.launch {
                        var medicationIdToReschedule: Int? = null
                        try {
                            val reminder = localReminderRepository.getReminderById(reminderId)
                            if (reminder == null) {
                                Log.e(TAG, "${NotificationHelper.ACTION_MARK_AS_TAKEN}: Reminder with ID $reminderId not found.")
                                localNotificationScheduler.cancelAllAlarmsForReminder(context, reminderId)
                                return@launch
                            }
                            medicationIdToReschedule = reminder.medicationId
                            Log.d(TAG, "${NotificationHelper.ACTION_MARK_AS_TAKEN}: Reminder found. medicationIdToReschedule=$medicationIdToReschedule")

                            if (reminder.isTaken) {
                                Log.w(TAG, "${NotificationHelper.ACTION_MARK_AS_TAKEN}: Reminder ID $reminderId was already marked as taken.")
                                localNotificationScheduler.cancelAllAlarmsForReminder(context, reminderId)
                            } else {
                                val now = LocalDateTime.now()
                                val nowString = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) // Ensure this matches DB format if critical, else just for logging
                                localReminderRepository.markReminderAsTaken(reminderId, nowString)
                                Log.i(TAG, "${NotificationHelper.ACTION_MARK_AS_TAKEN}: Reminder ID $reminderId marked as taken in DB at $nowString.")
                                localNotificationScheduler.cancelAllAlarmsForReminder(context, reminderId)
                            }

                            medicationIdToReschedule?.let { medId ->
                                Log.i(TAG, "${NotificationHelper.ACTION_MARK_AS_TAKEN}: Enqueuing ReminderSchedulingWorker for medication ID: $medId to schedule next reminder.")
                                val workManager = WorkManager.getInstance(context.applicationContext)
                                val data = Data.Builder()
                                    .putInt(ReminderSchedulingWorker.KEY_MEDICATION_ID, medId)
                                    .putBoolean(ReminderSchedulingWorker.KEY_IS_DAILY_REFRESH, false)
                                    .build()
                                val scheduleNextWorkRequest = OneTimeWorkRequestBuilder<ReminderSchedulingWorker>()
                                    .setInputData(data)
                                    .addTag("${ReminderSchedulingWorker.WORK_NAME_PREFIX}Next_${medId}") // Unique tag per medication
                                    .build()
                                workManager.enqueueUniqueWork(
                                    "${ReminderSchedulingWorker.WORK_NAME_PREFIX}NextScheduled_${medId}", // Unique work name
                                    ExistingWorkPolicy.REPLACE,
                                    scheduleNextWorkRequest
                                )
                                Log.d(TAG, "${NotificationHelper.ACTION_MARK_AS_TAKEN}: ReminderSchedulingWorker enqueued for medId=$medId.")
                            } ?: Log.w(TAG, "${NotificationHelper.ACTION_MARK_AS_TAKEN}: medicationIdToReschedule is null, cannot enqueue worker.")

                        } catch (e: Exception) {
                            Log.e(TAG, "${NotificationHelper.ACTION_MARK_AS_TAKEN}: Error processing for reminderId=$reminderId, medicationIdToReschedule=$medicationIdToReschedule", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    Log.e(TAG, "${NotificationHelper.ACTION_MARK_AS_TAKEN}: Invalid reminderId (-1) received.")
                }
            }
            else -> Log.w(TAG, "Received unhandled action: ${intent.action}")
        }
    }
}