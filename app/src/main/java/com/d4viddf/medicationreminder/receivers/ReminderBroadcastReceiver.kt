package com.d4viddf.medicationreminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.common.IntentActionConstants
import com.d4viddf.medicationreminder.common.IntentExtraConstants
import com.d4viddf.medicationreminder.common.NotificationConstants
import com.d4viddf.medicationreminder.common.WorkerConstants
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.di.ReminderReceiverEntryPoint
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.services.PreReminderForegroundService
import com.d4viddf.medicationreminder.utils.FileLogger
// import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker // Now using WorkerConstants
import com.d4viddf.medicationreminder.data.MedicationReminder // Import MedicationReminder
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
        // Actions moved to IntentActionConstants
        // Extras moved to IntentExtraConstants

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
                    val valueString = if (key == IntentExtraConstants.EXTRA_ACTUAL_REMINDER_TIME_MILLIS || key == IntentExtraConstants.EXTRA_NEXT_DOSE_TIME_MILLIS) {
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
        val initialLog = "onReceive called. Action: ${intent?.action}, Extras: ${getIntentExtrasString(intent)}"
        Log.d(TAG, initialLog)
        FileLogger.log(TAG, initialLog)

        if (context == null || intent == null) {
            val errorNullCtx = "Context or Intent is null. Cannot process broadcast."
            Log.e(TAG, errorNullCtx)
            FileLogger.log(TAG, errorNullCtx)
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
            IntentActionConstants.ACTION_SHOW_REMINDER -> {
                val reminderId = intent.getIntExtra(IntentExtraConstants.EXTRA_REMINDER_ID, -1)
                if (reminderId == -1) {
                    val invalidIdLog = "${IntentActionConstants.ACTION_SHOW_REMINDER}: Invalid reminderId. Aborting."
                    Log.e(TAG, invalidIdLog)
                    FileLogger.log(TAG, invalidIdLog)
                    return
                }
                val stopServiceIntent = Intent(context, PreReminderForegroundService::class.java).apply {
                    action = IntentActionConstants.ACTION_STOP_PRE_REMINDER // Corrected: PreReminderForegroundService.ACTION_STOP_PRE_REMINDER to IntentActionConstants
                    putExtra(IntentExtraConstants.EXTRA_SERVICE_REMINDER_ID, reminderId) // Corrected: PreReminderForegroundService.EXTRA_SERVICE_REMINDER_ID to IntentExtraConstants
                }
                context.startService(stopServiceIntent) // Intenta detenerlo

                val medicationName = intent.getStringExtra(IntentExtraConstants.EXTRA_MEDICATION_NAME) ?: "Medication"
                val medicationDosage = intent.getStringExtra(IntentExtraConstants.EXTRA_MEDICATION_DOSAGE) ?: ""
                val actualReminderTimeMillis = intent.getLongExtra(IntentExtraConstants.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, System.currentTimeMillis())
                val isIntervalType = intent.getBooleanExtra(IntentExtraConstants.EXTRA_IS_INTERVAL, false)
                val nextDoseTimeMillisExtra = intent.getLongExtra(IntentExtraConstants.EXTRA_NEXT_DOSE_TIME_MILLIS, 0L)
                val nextDoseTimeForHelper = if (nextDoseTimeMillisExtra > 0) nextDoseTimeMillisExtra else null

                val extractedInfoLog = "${IntentActionConstants.ACTION_SHOW_REMINDER}: Extracted: reminderId=$reminderId, medicationName='$medicationName', medicationDosage='$medicationDosage', actualReminderTime=${formatMillisToDateTimeString(actualReminderTimeMillis)}, isIntervalType=$isIntervalType, nextDoseTimeForHelper=${formatMillisToDateTimeString(nextDoseTimeForHelper)}"
                Log.i(TAG, extractedInfoLog)
                FileLogger.log(TAG, extractedInfoLog)

                val pendingResult = goAsync()
                scope.launch {
                    var notificationSoundUri: String? = null
                    var reminder: MedicationReminder? = null // Changed type here
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
                                val fetchedDetailsLog = "${IntentActionConstants.ACTION_SHOW_REMINDER}: Fetched details for notification: Color=$medicationColorHex, TypeName=$medicationTypeName for MedicationId=${medication.id}"
                                Log.d(TAG, fetchedDetailsLog)
                                FileLogger.log(TAG, fetchedDetailsLog)
                            } else {
                                val medNotFoundLog = "${IntentActionConstants.ACTION_SHOW_REMINDER}: Medication not found for ReminderId: $reminderId, MedicationId: ${reminder.medicationId}"
                                Log.w(TAG, medNotFoundLog)
                                FileLogger.log(TAG, medNotFoundLog)
                            }
                        } else {
                            val reminderNotFoundLog = "${IntentActionConstants.ACTION_SHOW_REMINDER}: Reminder not found for ID: $reminderId"
                            Log.w(TAG, reminderNotFoundLog)
                            FileLogger.log(TAG, reminderNotFoundLog)
                        }

                        val callingNotificationLog = "${IntentActionConstants.ACTION_SHOW_REMINDER}: Calling NotificationHelper.showReminderNotification with: context, reminderId=$reminderId, medicationName='$medicationName', medicationDosage='$medicationDosage', isIntervalType=$isIntervalType, nextDoseTimeForHelper=${formatMillisToDateTimeString(nextDoseTimeForHelper)}, actualReminderTimeMillis=${formatMillisToDateTimeString(actualReminderTimeMillis)}, notificationSoundUri=$notificationSoundUri, medicationColorHex=$medicationColorHex, medicationTypeName=$medicationTypeName"
                        Log.d(TAG, callingNotificationLog)
                        FileLogger.log(TAG, callingNotificationLog)
                        NotificationHelper.showReminderNotification(
                            context, reminderId, medicationName, medicationDosage,
                            isIntervalType, nextDoseTimeForHelper, actualReminderTimeMillis,
                            notificationSoundUri,
                            medicationColorHex,
                            medicationTypeName
                        )
                    } catch (e: Exception) {
                        val errorCoroutineLog = "${IntentActionConstants.ACTION_SHOW_REMINDER}: Error in coroutine for ReminderId: $reminderId"
                        Log.e(TAG, errorCoroutineLog, e)
                        FileLogger.log(TAG, errorCoroutineLog, e)
                        val fallbackLog = "${IntentActionConstants.ACTION_SHOW_REMINDER}: Fallback NotificationHelper.showReminderNotification with: context, reminderId=$reminderId, medicationName='$medicationName', medicationDosage='$medicationDosage', isIntervalType=$isIntervalType, nextDoseTimeForHelper=${formatMillisToDateTimeString(nextDoseTimeForHelper)}, actualReminderTimeMillis=${formatMillisToDateTimeString(actualReminderTimeMillis)}, null, null, null"
                        Log.d(TAG, fallbackLog)
                        FileLogger.log(TAG, fallbackLog)
                        NotificationHelper.showReminderNotification(
                            context, reminderId, medicationName, medicationDosage,
                            isIntervalType, nextDoseTimeForHelper, actualReminderTimeMillis,
                            null, null, null
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            IntentActionConstants.ACTION_TRIGGER_PRE_REMINDER_SERVICE -> {
                val reminderId = intent.getIntExtra(IntentExtraConstants.EXTRA_REMINDER_ID, -1)
                val actualScheduledTimeMillis = intent.getLongExtra(IntentExtraConstants.EXTRA_ACTUAL_REMINDER_TIME_MILLIS, -1L)
                val medicationName = intent.getStringExtra(IntentExtraConstants.EXTRA_MEDICATION_NAME) ?: "Medication"

                val extractedPreLog = "${IntentActionConstants.ACTION_TRIGGER_PRE_REMINDER_SERVICE}: Extracted: reminderId=$reminderId, actualScheduledTime=${formatMillisToDateTimeString(actualScheduledTimeMillis)}, medicationName='$medicationName'"
                Log.i(TAG, extractedPreLog)
                FileLogger.log(TAG, extractedPreLog)

                if (reminderId != -1 && actualScheduledTimeMillis != -1L) {
                    if (actualScheduledTimeMillis < System.currentTimeMillis()){
                        val pastTimeLog = "${IntentActionConstants.ACTION_TRIGGER_PRE_REMINDER_SERVICE}: Pre-reminder trigger for ID $reminderId, but actual scheduled time ${formatMillisToDateTimeString(actualScheduledTimeMillis)} is in the past. Not starting service."
                        Log.w(TAG, pastTimeLog)
                        FileLogger.log(TAG, pastTimeLog)
                        return
                    }

                    val serviceIntent = Intent(context, PreReminderForegroundService::class.java).apply {
                        putExtra(IntentExtraConstants.EXTRA_SERVICE_REMINDER_ID, reminderId)
                        putExtra(IntentExtraConstants.EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS, actualScheduledTimeMillis)
                        putExtra(IntentExtraConstants.EXTRA_SERVICE_MEDICATION_NAME, medicationName)
                    }
                    context.startForegroundService(serviceIntent)
                    val startedServiceLog = "${IntentActionConstants.ACTION_TRIGGER_PRE_REMINDER_SERVICE}: Started PreReminderForegroundService for reminderId: $reminderId"
                    Log.i(TAG, startedServiceLog)
                    FileLogger.log(TAG, startedServiceLog)
                } else {
                    val invalidDataLog = "${IntentActionConstants.ACTION_TRIGGER_PRE_REMINDER_SERVICE}: Invalid data. reminderId=$reminderId, actualScheduledTimeMillis=$actualScheduledTimeMillis. Cannot start service."
                    Log.e(TAG, invalidDataLog)
                    FileLogger.log(TAG, invalidDataLog)
                }
            }

            IntentActionConstants.ACTION_MARK_AS_TAKEN -> { // Corrected: NotificationHelper.ACTION_MARK_AS_TAKEN
                val reminderId = intent.getIntExtra(IntentExtraConstants.EXTRA_REMINDER_ID, -1)
                val receivedMarkLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: Received for reminderId=$reminderId"
                Log.i(TAG, receivedMarkLog)
                FileLogger.log(TAG, receivedMarkLog)

                if (reminderId != -1) {
                    val pendingResult = goAsync()
                    scope.launch {
                        var medicationIdToReschedule: Int? = null
                        try {
                            val reminder = localReminderRepository.getReminderById(reminderId)
                            if (reminder == null) {
                                val notFoundLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: Reminder with ID $reminderId not found."
                                Log.e(TAG, notFoundLog)
                                FileLogger.log(TAG, notFoundLog)
                                localNotificationScheduler.cancelAllAlarmsForReminder(context, reminderId)
                                return@launch
                            }
                            medicationIdToReschedule = reminder.medicationId
                            val foundMedIdLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: Reminder found. medicationIdToReschedule=$medicationIdToReschedule"
                            Log.d(TAG, foundMedIdLog)
                            FileLogger.log(TAG, foundMedIdLog)

                            if (reminder.isTaken) {
                                val alreadyTakenLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: Reminder ID $reminderId was already marked as taken."
                                Log.w(TAG, alreadyTakenLog)
                                FileLogger.log(TAG, alreadyTakenLog)
                                // Still cancel alarms even if already marked taken, to ensure consistency if UI was out of sync
                                localNotificationScheduler.cancelAllAlarmsForReminder(context, reminderId)
                            } else {
                                val now = LocalDateTime.now()
                                val nowString = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                val updateSuccess = localReminderRepository.markReminderAsTaken(reminderId, nowString)
                                if (updateSuccess) {
                                    val markedTakenDbLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: Reminder ID $reminderId successfully marked as taken in DB at $nowString."
                                    Log.i(TAG, markedTakenDbLog)
                                    FileLogger.log(TAG, markedTakenDbLog)
                                } else {
                                    val markedTakenDbErrorLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: FAILED to mark reminder ID $reminderId as taken in DB (update reported 0 rows affected or reminder not found)."
                                    Log.e(TAG, markedTakenDbErrorLog)
                                    FileLogger.log(TAG, markedTakenDbErrorLog)
                                }
                                // Always cancel alarms and attempt to schedule next, even if DB update failed,
                                // to maintain user-facing behavior consistency. DB sync issues are logged.
                                localNotificationScheduler.cancelAllAlarmsForReminder(context, reminderId)
                            }

                            medicationIdToReschedule?.let { medId ->
                                val enqueueLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: Enqueuing ReminderSchedulingWorker for medication ID: $medId to schedule next reminder."
                                Log.i(TAG, enqueueLog)
                                FileLogger.log(TAG, enqueueLog)
                                val workManager = WorkManager.getInstance(context.applicationContext)
                                val data = Data.Builder()
                                    .putInt(WorkerConstants.KEY_MEDICATION_ID, medId)
                                    .putBoolean(WorkerConstants.KEY_IS_DAILY_REFRESH, false)
                                    .build()
                                val scheduleNextWorkRequest = OneTimeWorkRequestBuilder<ReminderSchedulingWorker>() // Corrected: ReminderSchedulingWorker to its FQDN
                                    .setInputData(data)
                                    .addTag("${WorkerConstants.WORK_NAME_PREFIX}Next_${medId}")
                                    .build()
                                workManager.enqueueUniqueWork(
                                    "${WorkerConstants.WORK_NAME_PREFIX}NextScheduled_${medId}",
                                    ExistingWorkPolicy.REPLACE,
                                    scheduleNextWorkRequest
                                )
                                val enqueuedDetailLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: ReminderSchedulingWorker enqueued for medId=$medId."
                                Log.d(TAG, enqueuedDetailLog)
                                FileLogger.log(TAG, enqueuedDetailLog)
                            } ?: run {
                                val nullMedIdLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: medicationIdToReschedule is null, cannot enqueue worker."
                                Log.w(TAG, nullMedIdLog)
                                FileLogger.log(TAG, nullMedIdLog)
                            }

                        } catch (e: Exception) {
                            val errorProcessingLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: Error processing for reminderId=$reminderId, medicationIdToReschedule=$medicationIdToReschedule"
                            Log.e(TAG, errorProcessingLog, e)
                            FileLogger.log(TAG, errorProcessingLog, e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    val invalidIdReceivedLog = "${IntentActionConstants.ACTION_MARK_AS_TAKEN}: Invalid reminderId (-1) received."
                    Log.e(TAG, invalidIdReceivedLog)
                    FileLogger.log(TAG, invalidIdReceivedLog)
                }
            }
            else -> {
                val unhandledActionLog = "Received unhandled action: ${intent.action}"
                Log.w(TAG, unhandledActionLog)
                FileLogger.log(TAG, unhandledActionLog)
            }
        }
    }
}