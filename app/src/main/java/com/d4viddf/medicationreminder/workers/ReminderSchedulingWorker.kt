package com.d4viddf.medicationreminder.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
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
        Log.d(TAG, "ReminderSchedulingWorker (CustomFactory) started. InputData: $inputData, Thread: ${Thread.currentThread().name}")
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
        val funcTag = "$TAG[scheduleNextReminderForMedication|MedId:${medication.id}|MedName:${medication.name}]"
        Log.i(funcTag, "Attempting to schedule next reminder.")

        val schedule: MedicationSchedule? = medicationScheduleRepository.getSchedulesForMedication(medication.id)
            .firstOrNull()?.firstOrNull()

        if (schedule == null) {
            Log.w(funcTag, "No schedule found. Cannot schedule next reminder.")
            return
        }

        val now = LocalDateTime.now()
        val medicationStartDate = medication.startDate?.let { ReminderCalculator.dateStorableFormatter.parse(it, LocalDate::from) } ?: now.toLocalDate()
        val medicationEndDate = medication.endDate?.let { ReminderCalculator.dateStorableFormatter.parse(it, LocalDate::from) }
        Log.d(funcTag, "Current datetime (now): $now, Medication StartDate: $medicationStartDate, Medication EndDate: $medicationEndDate")

        if (medicationEndDate != null && now.toLocalDate().isAfter(medicationEndDate)) {
            Log.i(funcTag, "Medication period has ended (EndDate: $medicationEndDate). Cleaning up any existing future reminders.")
            cleanupEndedMedicationReminders(medication.id)
            return
        }

        // 1. Determine Search Window for ideal reminder calculation
        val effectiveSearchStartDateTime = if (medicationStartDate.isAfter(now.toLocalDate())) medicationStartDate.atStartOfDay() else now
        var calculationWindowEndDate = effectiveSearchStartDateTime.toLocalDate().plusDays(3) // How far out to calculate ideal reminders
        if (medicationEndDate != null && medicationEndDate.isBefore(calculationWindowEndDate)) {
            calculationWindowEndDate = medicationEndDate
        }
        Log.d(funcTag, "Calculated effectiveSearchStartDateTime: $effectiveSearchStartDateTime, calculationWindowEndDate: $calculationWindowEndDate")

        if (calculationWindowEndDate.isBefore(effectiveSearchStartDateTime.toLocalDate())) {
            Log.i(funcTag, "Calculation window end ($calculationWindowEndDate) is before effective start (${effectiveSearchStartDateTime.toLocalDate()}). No reminders to schedule.")
            val existingFutureRemindersDb = medicationReminderRepository.getFutureUntakenRemindersForMedication(
                medication.id, now.format(storableDateTimeFormatter)
            ).firstOrNull() ?: emptyList()
            existingFutureRemindersDb.forEach { staleReminder ->
                Log.d(funcTag, "Medication period ended or invalid: Cleaning up stale reminder ID ${staleReminder.id}")
                notificationScheduler.cancelAllAlarmsForReminder(applicationContext, staleReminder.id)
                medicationReminderRepository.deleteReminderById(staleReminder.id)
            }
            return
        }

        // 2. Fetch Existing Future Untaken Reminders
        val existingFutureRemindersDb = medicationReminderRepository.getFutureUntakenRemindersForMedication(
            medication.id, now.format(storableDateTimeFormatter)
        ).firstOrNull() ?: emptyList()
        val existingFutureRemindersMap = existingFutureRemindersDb.associateBy {
            try { LocalDateTime.parse(it.reminderTime, storableDateTimeFormatter) } catch (e: Exception) { null }
        }.filterKeys { it != null } as Map<LocalDateTime, MedicationReminder>
        Log.d(funcTag, "Found ${existingFutureRemindersMap.size} existing future untaken reminders.")


        // 3. Get Ideal Reminders for the period
        val calculatedRemindersMap = ReminderCalculator.generateRemindersForPeriod(
            medication, schedule, effectiveSearchStartDateTime.toLocalDate(), calculationWindowEndDate
        )
        Log.d(funcTag, "CalculatedRemindersMap from ReminderCalculator: ${calculatedRemindersMap.mapValues { entry -> entry.value.map { it.toString() } }}")

        val idealFutureDateTimesSet = mutableSetOf<LocalDateTime>()
        calculatedRemindersMap.forEach { (date, times) ->
            times.forEach { time ->
                val idealDateTime = LocalDateTime.of(date, time)
                if (idealDateTime.isAfter(now) &&
                    (medicationEndDate == null || !idealDateTime.toLocalDate().isAfter(medicationEndDate)) &&
                    !idealDateTime.toLocalDate().isBefore(medicationStartDate)
                ) {
                    idealFutureDateTimesSet.add(idealDateTime)
                }
            }
        }
        Log.d(funcTag, "Populated idealFutureDateTimesSet (before sorting, size ${idealFutureDateTimesSet.size}): ${idealFutureDateTimesSet.map { it.toString() }}")
        if (idealFutureDateTimesSet.isEmpty()) {
            Log.i(funcTag, "idealFutureDateTimesSet is empty after filtering for future and medication period. No new reminders will be scheduled.")
        }

        val sortedIdealFutureDateTimes = idealFutureDateTimesSet.toList().sorted()


        // 4. Schedule New/Missing Reminders
        sortedIdealFutureDateTimes.forEachIndexed { index, idealDateTime ->
            if (!existingFutureRemindersMap.containsKey(idealDateTime)) {
                val reminderObjectToInsert = MedicationReminder(
                    medicationId = medication.id,
                    medicationScheduleId = schedule.id,
                    reminderTime = idealDateTime.format(storableDateTimeFormatter),
                    isTaken = false, takenAt = null, notificationId = null
                )
                val actualReminderIdFromDb = medicationReminderRepository.insertReminder(reminderObjectToInsert)
                Log.i(funcTag, "Scheduling new reminder for idealDateTime: $idealDateTime, actualReminderIdFromDb: $actualReminderIdFromDb")
                val reminderWithActualId = reminderObjectToInsert.copy(id = actualReminderIdFromDb.toInt())

                val actualScheduledTimeMillis = idealDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                var nextDoseTimeForHelperMillis: Long? = null
                if (index + 1 < sortedIdealFutureDateTimes.size) {
                    nextDoseTimeForHelperMillis = sortedIdealFutureDateTimes[index + 1]
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }

                val isIntervalType = schedule.scheduleType == ScheduleType.INTERVAL
                Log.d(funcTag, "Preparing to schedule with NotificationScheduler: reminderId=${reminderWithActualId.id}, isInterval=$isIntervalType, nextDoseHelperMillis=$nextDoseTimeForHelperMillis")

                try {
                    notificationScheduler.scheduleNotification(
                        applicationContext, reminderWithActualId, medication.name, medication.dosage ?: "",
                        isIntervalType, nextDoseTimeForHelperMillis, actualScheduledTimeMillis
                    )
                    if (ENABLE_PRE_REMINDER_NOTIFICATION_FEATURE) {
                        val preReminderTargetTimeMillis = actualScheduledTimeMillis - TimeUnit.MINUTES.toMillis(PRE_REMINDER_OFFSET_MINUTES)
                        if (preReminderTargetTimeMillis > System.currentTimeMillis()) {
                            Log.d(funcTag, "Scheduling pre-reminder for reminderId ${reminderWithActualId.id} at $preReminderTargetTimeMillis")
                            notificationScheduler.schedulePreReminderServiceTrigger(
                                applicationContext, reminderWithActualId, actualScheduledTimeMillis, medication.name
                            )
                        } else {
                            Log.d(funcTag, "Pre-reminder time for reminderId ${reminderWithActualId.id} is in the past ($preReminderTargetTimeMillis). Skipping pre-reminder.")
                        }
                    }
                } catch (e: IllegalStateException) {
                    Log.e(funcTag, "ALARM LIMIT EXCEPTION for reminder ID ${reminderWithActualId.id}: ${e.message}", e)
                } catch (e: Exception) {
                    Log.e(funcTag, "Generic error scheduling alarm for reminder ID ${reminderWithActualId.id}", e)
                }
            } else {
                Log.d(funcTag, "Ideal reminder at $idealDateTime already exists (ID: ${existingFutureRemindersMap[idealDateTime]?.id}). Skipping scheduling.")
            }
        }

        // 5. Cleanup Stale Reminders
        existingFutureRemindersMap.forEach { (dateTime, existingReminder) ->
            if (!idealFutureDateTimesSet.contains(dateTime)) {
                Log.i(funcTag, "Cleaning up stale reminder. DateTime: $dateTime, ExistingReminderId: ${existingReminder.id}.")
                notificationScheduler.cancelAllAlarmsForReminder(applicationContext, existingReminder.id)
                medicationReminderRepository.deleteReminderById(existingReminder.id)
            }
        }
        Log.i(funcTag, "Reminder scheduling/synchronization complete.")
    }
}