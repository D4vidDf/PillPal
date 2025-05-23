package com.d4viddf.medicationreminder.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationRepository
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.MedicationScheduleRepository
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ReminderSchedulingWorker constructor(
    appContext: Context,
    workerParams: WorkerParameters,
    private val medicationRepository: MedicationRepository,
    private val medicationScheduleRepository: MedicationScheduleRepository,
    private val medicationReminderRepository: MedicationReminderRepository,
    private val notificationScheduler: NotificationScheduler
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME_PREFIX = "ReminderSchedulingWorker_"
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_IS_DAILY_REFRESH = "is_daily_refresh"
        private const val TAG = "ReminderSchedWorker"
        private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        // Flags de configuración (simulados por ahora)
        const val ENABLE_PRE_REMINDER_NOTIFICATION_FEATURE = true
        const val PRE_REMINDER_OFFSET_MINUTES = 60L // Configurable: 60 minutos antes
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "ReminderSchedulingWorker (CustomFactory) started. InputData: $inputData")
        val medicationIdInput = inputData.getInt(KEY_MEDICATION_ID, -1)
        val isDailyRefresh = inputData.getBoolean(KEY_IS_DAILY_REFRESH, false)

        return try {
            if (isDailyRefresh) {
                Log.d(TAG, "Performing daily refresh for all active medications.")
                val allMedications = medicationRepository.getAllMedications().firstOrNull() ?: emptyList()
                if (allMedications.isEmpty()) {
                    Log.i(TAG, "No medications found for daily refresh.")
                }
                allMedications.forEach { medication ->
                    val medicationEndDate = medication.endDate?.let { LocalDate.parse(it, ReminderCalculator.dateStorableFormatter) }
                    if (medicationEndDate == null || !LocalDate.now().isAfter(medicationEndDate)) {
                        scheduleNextReminderForMedication(medication)
                    } else {
                        Log.d(TAG, "Skipping daily refresh for ${medication.name}, medication period ended.")
                        cleanupEndedMedicationReminders(medication.id)
                    }
                }
            } else if (medicationIdInput != -1) {
                medicationRepository.getMedicationById(medicationIdInput)?.let { medication ->
                    Log.d(TAG, "Processing specific medication ID: ${medication.id}")
                    scheduleNextReminderForMedication(medication)
                } ?: Log.e(TAG, "Medication not found for ID: $medicationIdInput for specific scheduling.")
            } else {
                Log.w(TAG, "Worker run without medication ID and not a daily refresh.")
            }
            Log.i(TAG, "ReminderSchedulingWorker (CustomFactory) finished successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ReminderSchedulingWorker (CustomFactory)", e)
            Result.failure()
        }
    }

    private suspend fun cleanupEndedMedicationReminders(medicationId: Int) {
        Log.d(TAG, "Cleaning up reminders for ended medication ID: $medicationId")
        val now = LocalDateTime.now()
        val existingFutureReminders = medicationReminderRepository.getFutureUntakenRemindersForMedication(
            medicationId,
            now.format(storableDateTimeFormatter)
        ).firstOrNull()

        existingFutureReminders?.forEach { existingReminder ->
            Log.d(TAG, "Cleaning up (medication ended): Cancelling reminder ID: ${existingReminder.id} for med ID: $medicationId")
            notificationScheduler.cancelAllAlarmsForReminder(applicationContext, existingReminder.id) // Cancelar principal y previa
            medicationReminderRepository.deleteReminderById(existingReminder.id)
        }
    }


    private suspend fun scheduleNextReminderForMedication(medication: Medication) {
        Log.i(TAG, "Attempting to schedule next reminder for ${medication.name} (ID: ${medication.id})")

        val schedule: MedicationSchedule? = medicationScheduleRepository.getSchedulesForMedication(medication.id)
            .firstOrNull()?.firstOrNull()

        if (schedule == null) {
            Log.w(TAG, "No schedule found for medication ID: ${medication.id}. Cannot schedule next reminder.")
            return
        }

        val now = LocalDateTime.now()
        val medicationStartDate = medication.startDate?.let { ReminderCalculator.dateStorableFormatter.parse(it, LocalDate::from) } ?: now.toLocalDate()
        val medicationEndDate = medication.endDate?.let { ReminderCalculator.dateStorableFormatter.parse(it, LocalDate::from) }

        if (medicationEndDate != null && now.toLocalDate().isAfter(medicationEndDate)) {
            Log.i(TAG, "Medication ${medication.name} period has ended. Cleaning up any existing future reminders.")
            cleanupEndedMedicationReminders(medication.id)
            return
        }

        // 1. Determine Search Window for ideal reminder calculation
        // Search from 'now' (or medication start if in future) up to a defined window (e.g., 3 days), respecting medication end date.
        val effectiveSearchStartDateTime = if (medicationStartDate.isAfter(now.toLocalDate())) medicationStartDate.atStartOfDay() else now
        var calculationWindowEndDate = effectiveSearchStartDateTime.toLocalDate().plusDays(3) // How far out to calculate ideal reminders
        if (medicationEndDate != null && medicationEndDate.isBefore(calculationWindowEndDate)) {
            calculationWindowEndDate = medicationEndDate
        }
        // Ensure calculation window doesn't start after it ends, especially if medicationEndDate is today or in past relative to effectiveSearchStartDateTime's date part
        if (calculationWindowEndDate.isBefore(effectiveSearchStartDateTime.toLocalDate())) {
            Log.i(TAG, "Calculation window end ($calculationWindowEndDate) is before effective start (${effectiveSearchStartDateTime.toLocalDate()}). No reminders to schedule for ${medication.name}.")
            // Still need to cleanup any existing future reminders if the medication period might have changed.
            val existingFutureRemindersDb = medicationReminderRepository.getFutureUntakenRemindersForMedication(
                medication.id, now.format(storableDateTimeFormatter)
            ).firstOrNull() ?: emptyList()
            existingFutureRemindersDb.forEach { staleReminder ->
                Log.d(TAG, "Medication period ended or invalid: Cleaning up stale reminder ID ${staleReminder.id} for ${medication.name}")
                notificationScheduler.cancelAllAlarmsForReminder(applicationContext, staleReminder.id)
                medicationReminderRepository.deleteReminderById(staleReminder.id)
            }
            return
        }

        Log.d(TAG, "Effective search start for ideal reminders: $effectiveSearchStartDateTime. Calculation window end: $calculationWindowEndDate for ${medication.name}")

        // 2. Fetch Existing Future Untaken Reminders
        val existingFutureRemindersDb = medicationReminderRepository.getFutureUntakenRemindersForMedication(
            medication.id, now.format(storableDateTimeFormatter) // Get reminders strictly after 'now'
        ).firstOrNull() ?: emptyList()

        val existingFutureRemindersMap = existingFutureRemindersDb.associateBy {
            try { LocalDateTime.parse(it.reminderTime, storableDateTimeFormatter) } catch (e: Exception) { null }
        }.filterKeys { it != null } as Map<LocalDateTime, MedicationReminder>
        Log.d(TAG, "Found ${existingFutureRemindersMap.size} existing future untaken reminders for ${medication.name}.")


        // 3. Get Ideal Reminders for the period
        val calculatedRemindersMap = ReminderCalculator.generateRemindersForPeriod(
            medication, schedule, effectiveSearchStartDateTime.toLocalDate(), calculationWindowEndDate
        )

        val idealFutureDateTimesSet = mutableSetOf<LocalDateTime>()
        calculatedRemindersMap.forEach { (date, times) ->
            times.forEach { time ->
                val idealDateTime = LocalDateTime.of(date, time)
                // Ensure idealDateTime is in the future relative to 'now' and respects medication start/end.
                if (idealDateTime.isAfter(now) && // Strictly after current time
                    (medicationEndDate == null || !idealDateTime.toLocalDate().isAfter(medicationEndDate)) &&
                    !idealDateTime.toLocalDate().isBefore(medicationStartDate)
                ) {
                    idealFutureDateTimesSet.add(idealDateTime)
                }
            }
        }
        Log.d(TAG, "Calculated ${idealFutureDateTimesSet.size} ideal future date times for ${medication.name} within window.")

        val sortedIdealFutureDateTimes = idealFutureDateTimesSet.toList().sorted()


        // 4. Schedule New/Missing Reminders
        sortedIdealFutureDateTimes.forEachIndexed { index, idealDateTime ->
            if (!existingFutureRemindersMap.containsKey(idealDateTime)) {
                Log.i(TAG, "Scheduling new reminder for ${medication.name} at $idealDateTime.")
                val reminderObjectToInsert = MedicationReminder(
                    medicationId = medication.id,
                    medicationScheduleId = schedule.id,
                    reminderTime = idealDateTime.format(storableDateTimeFormatter),
                    isTaken = false, takenAt = null, notificationId = null // notificationId might be set by AlarmManager
                )
                val actualReminderIdFromDb = medicationReminderRepository.insertReminder(reminderObjectToInsert)
                val reminderWithActualId = reminderObjectToInsert.copy(id = actualReminderIdFromDb.toInt())

                val actualScheduledTimeMillis = idealDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                var nextDoseTimeForHelperMillis: Long? = null
                if (index + 1 < sortedIdealFutureDateTimes.size) {
                    nextDoseTimeForHelperMillis = sortedIdealFutureDateTimes[index + 1]
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }

                val isIntervalType = schedule.scheduleType == ScheduleType.INTERVAL
                Log.d(TAG, "Scheduling with NotificationScheduler: reminderId=${reminderWithActualId.id}, isInterval=$isIntervalType, nextDoseHelperMillis=$nextDoseTimeForHelperMillis")

                try {
                    notificationScheduler.scheduleNotification(
                        applicationContext, reminderWithActualId, medication.name, medication.dosage ?: "",
                        isIntervalType, nextDoseTimeForHelperMillis, actualScheduledTimeMillis
                    )
                    if (ENABLE_PRE_REMINDER_NOTIFICATION_FEATURE) {
                        val preReminderTargetTimeMillis = actualScheduledTimeMillis - TimeUnit.MINUTES.toMillis(PRE_REMINDER_OFFSET_MINUTES)
                        if (preReminderTargetTimeMillis > System.currentTimeMillis()) {
                            notificationScheduler.schedulePreReminderServiceTrigger(
                                applicationContext, reminderWithActualId, actualScheduledTimeMillis, medication.name
                            )
                        }
                    }
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "ALARM LIMIT EXCEPTION for reminder ID ${reminderWithActualId.id}: ${e.message}", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Generic error scheduling alarm for reminder ID ${reminderWithActualId.id}", e)
                }
            } else {
                Log.d(TAG, "Ideal reminder at $idealDateTime for ${medication.name} already exists. Skipping scheduling.")
            }
        }

        // 5. Cleanup Stale Reminders
        existingFutureRemindersMap.forEach { (dateTime, existingReminder) ->
            if (!idealFutureDateTimesSet.contains(dateTime)) {
                Log.i(TAG, "Cleaning up stale reminder ID ${existingReminder.id} at $dateTime for ${medication.name}.")
                notificationScheduler.cancelAllAlarmsForReminder(applicationContext, existingReminder.id)
                medicationReminderRepository.deleteReminderById(existingReminder.id)
            }
        }
        Log.i(TAG, "Reminder scheduling/synchronization complete for ${medication.name}")
    }
}