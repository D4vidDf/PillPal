package com.d4viddf.medicationreminder.workers

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.d4viddf.medicationreminder.common.WorkerConstants.ENABLE_PRE_REMINDER_NOTIFICATION_FEATURE
import com.d4viddf.medicationreminder.common.WorkerConstants.KEY_IS_DAILY_REFRESH
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
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
        // Constants moved to WorkerConstants.kt
        private const val TAG = "ReminderSchedWorker"
        private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override suspend fun doWork(): Result {
        Log.d(TAG, "ReminderSchedulingWorker (CustomFactory) started. InputData: $inputData, Thread: ${Thread.currentThread().name}")
        val medicationIdInput = inputData.getInt(com.d4viddf.medicationreminder.common.WorkerConstants.KEY_MEDICATION_ID, -1)
        val isDailyRefresh = inputData.getBoolean(com.d4viddf.medicationreminder.common.WorkerConstants.KEY_IS_DAILY_REFRESH, false)

        return try {
            if (isDailyRefresh) {
                Log.d(TAG, "Performing daily refresh for all active medications.")
                val allMedications = medicationRepository.getAllMedications().firstOrNull() ?: emptyList()
                Log.i(TAG, "Daily refresh: Found ${allMedications.size} medications to process.")
                if (allMedications.isEmpty()) {
                    Log.i(TAG, "No medications found for daily refresh.")
                }
                allMedications.forEach { medication ->
                    Log.d(TAG, "Daily refresh: Processing medication ID: ${medication.id}, Name: ${medication.name}")
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
                    Log.i(TAG, "Specific scheduling: Processing medication ID: ${medication.id}, Name: ${medication.name}")
                    scheduleNextReminderForMedication(medication)
                } ?: Log.e(TAG, "Medication not found for ID: $medicationIdInput for specific scheduling.")
            } else {
                Log.w(TAG, "Worker run without medication ID and not a daily refresh.")
            }
            Log.i(TAG, "ReminderSchedulingWorker (CustomFactory) finished successfully")
            // Check if stopped even on success path, as it might be a graceful stop
            if (isStopped) {
                Log.w(TAG, "Worker finished but was stopped. Stop reason: ${getStopReason()}")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ReminderSchedulingWorker (CustomFactory)", e)
            if (isStopped) {
                Log.w(TAG, "Worker was stopped due to error. Stop reason: ${getStopReason()}")
            }
            Result.failure()
        } finally {
            // This block executes regardless of whether an exception occurred or not.
            // Useful for final check if the worker was stopped for reasons not caught by the main try-catch.
            if (isStopped) {
                Log.w(TAG, "ReminderSchedulingWorker isStopped in finally block. Stop reason: ${getStopReason()}")
            }
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

        Log.d(funcTag, "Processing medication: ${medication.name} (ID: ${medication.id}) with schedule ID: ${schedule?.id}")

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
        val isDailyRefresh = inputData.getBoolean(KEY_IS_DAILY_REFRESH, false)
        // effectiveSearchStartDateTime is for the ReminderCalculator (conceptually calculatorSearchStartDateTime)
        val effectiveSearchStartDateTime = if (medicationStartDate.isAfter(now.toLocalDate())) {
            medicationStartDate.atStartOfDay()
        } else {
            if (isDailyRefresh && medicationStartDate.isBefore(now.toLocalDate().plusDays(1))) { // If daily refresh and med has started
                now.toLocalDate().atStartOfDay() // For today's processing, start from beginning of day for calculator
            } else {
                now // Otherwise, for specific scheduling or future meds, use now for calculator
            }
        }
        var calculationWindowEndDate = effectiveSearchStartDateTime.toLocalDate().plusDays(14) // How far out to calculate ideal reminders
        Log.d(funcTag, "Initial calculation window end date (14 days out or med end date): $calculationWindowEndDate")
        if (medicationEndDate != null && medicationEndDate.isBefore(calculationWindowEndDate)) {
            calculationWindowEndDate = medicationEndDate
            Log.d(funcTag, "Calculation window end date adjusted to medication end date: $calculationWindowEndDate")
        }
        Log.d(funcTag, "Calculator Search Start DateTime: $effectiveSearchStartDateTime, FINAL calculationWindowEndDate: $calculationWindowEndDate.")

        if (calculationWindowEndDate.isBefore(effectiveSearchStartDateTime.toLocalDate())) {
            Log.i(funcTag, "Calculation window end ($calculationWindowEndDate) is before effective start for calculator (${effectiveSearchStartDateTime.toLocalDate()}). No reminders to schedule.")
            // Cleanup logic remains the same
            val existingFutureUntakenRemindersDb = medicationReminderRepository.getFutureUntakenRemindersForMedication(
                medication.id, now.format(storableDateTimeFormatter)
            ).firstOrNull() ?: emptyList()
            existingFutureUntakenRemindersDb.forEach { staleReminder ->
                Log.d(funcTag, "Medication period ended or invalid (calc window): Cleaning up stale reminder ID ${staleReminder.id}")
                notificationScheduler.cancelAllAlarmsForReminder(applicationContext, staleReminder.id)
                medicationReminderRepository.deleteReminderById(staleReminder.id)
            }
            return
        }

        // Define dbQueryStartDateTime for fetching existing reminders from DB
        val dbQueryStartDateTime = if (medicationStartDate.isAfter(now.toLocalDate())) {
            medicationStartDate.atStartOfDay()
        } else {
            // If medication has started or starts today, always fetch existing DB entries from the beginning of today
            now.toLocalDate().atStartOfDay()
        }
        Log.d(funcTag, "DB Query Start DateTime for allExistingRemindersForPeriodMap: $dbQueryStartDateTime")

        // 2. Fetch ALL Existing Reminders for the defined period (taken and untaken) using dbQueryStartDateTime
        val dbQueryWindowStartStr = dbQueryStartDateTime.format(storableDateTimeFormatter)
        val dbQueryWindowEndStr = calculationWindowEndDate.atTime(23, 59, 59).format(storableDateTimeFormatter) // End of day for calculationWindowEndDate
        Log.d(funcTag, "DB Query Window for allExistingReminders: Start: $dbQueryWindowStartStr, End: $dbQueryWindowEndStr for MedId: ${medication.id}")

        val allExistingRemindersForPeriod = medicationReminderRepository.getRemindersForMedicationInWindow(
            medication.id,
            dbQueryWindowStartStr, // Use the new dbQueryStartDateTime string
            dbQueryWindowEndStr
        )
        val tempMap = mutableMapOf<LocalDateTime, MedicationReminder>()
        Log.d(funcTag, "Building allExistingRemindersForPeriodMap for MedId: ${medication.id}:")
        allExistingRemindersForPeriod.forEach { reminder ->
            try {
                val parsedKey = LocalDateTime.parse(reminder.reminderTime, storableDateTimeFormatter)
                tempMap[parsedKey] = reminder
                Log.d(funcTag, "  - Added to map: Key=$parsedKey, ReminderId=${reminder.id}, ReminderTimeStr='${reminder.reminderTime}', IsTaken=${reminder.isTaken}")
            } catch (e: Exception) {
                Log.e(funcTag, "  - Failed to parse reminderTime for ReminderId=${reminder.id}, TimeStr='${reminder.reminderTime}'. Skipping.", e)
            }
        }
        val allExistingRemindersForPeriodMap = tempMap.toMap()
        Log.d(funcTag, "Finished building allExistingRemindersForPeriodMap. Size: ${allExistingRemindersForPeriodMap.size}")
        Log.d(funcTag, "Found ${allExistingRemindersForPeriodMap.size} existing reminders (taken and untaken) in the period.") // This log seems redundant now but keeping for safety

        // Fetch Existing Future Untaken Reminders (still needed for stale cleanup logic)
        Log.d(funcTag, "Fetching existing future untaken reminders for medication ID: ${medication.id} for stale cleanup.")
        val existingFutureUntakenRemindersDb = medicationReminderRepository.getFutureUntakenRemindersForMedication(
            medication.id, now.format(storableDateTimeFormatter)
        ).firstOrNull() ?: emptyList()
        val existingFutureUntakenRemindersMap = existingFutureUntakenRemindersDb.associateBy { // Name changed for clarity
            try { LocalDateTime.parse(it.reminderTime, storableDateTimeFormatter) } catch (e: Exception) { null }
        }.filterKeys { it != null } as Map<LocalDateTime, MedicationReminder>
        Log.d(funcTag, "Found ${existingFutureUntakenRemindersMap.size} existing future untaken reminders (for stale cleanup).")


        // 3. Get Ideal Reminders for the period
        var lastTakenForIntervalCalc: LocalDateTime? = null
        if (schedule.scheduleType == ScheduleType.INTERVAL) {
            val mostRecentTakenReminder = medicationReminderRepository.getMostRecentTakenReminder(medication.id)
            if (mostRecentTakenReminder?.takenAt != null) {
                try {
                    lastTakenForIntervalCalc = LocalDateTime.parse(mostRecentTakenReminder.takenAt, storableDateTimeFormatter)
                    Log.i(funcTag, "Found most recent taken dose at $lastTakenForIntervalCalc for interval calculation (Med ID: ${medication.id}, Reminder ID: ${mostRecentTakenReminder.id})")
                } catch (e: Exception) {
                    Log.e(funcTag, "Error parsing takenAt for most recent taken reminder (Med ID: ${medication.id}, Reminder ID: ${mostRecentTakenReminder.id}, TakenAt: ${mostRecentTakenReminder.takenAt})", e)
                }
            } else {
                Log.d(funcTag, "No recent taken dose found with valid takenAt time for interval calculation (Med ID: ${medication.id})")
            }
        }

        Log.d(funcTag, "Calling ReminderCalculator.generateRemindersForPeriod with MedId: ${medication.id}, SchedId: ${schedule.id}, periodStart: ${effectiveSearchStartDateTime.toLocalDate()}, periodEnd: $calculationWindowEndDate, lastTaken: $lastTakenForIntervalCalc")
        val calculatedRemindersMap = ReminderCalculator.generateRemindersForPeriod(
            medication,
            schedule,
            effectiveSearchStartDateTime.toLocalDate(),
            calculationWindowEndDate,
            lastTakenForIntervalCalc // Pass the last taken date time, or null
        )
        Log.d(funcTag, "Raw calculatedRemindersMap from ReminderCalculator: ${calculatedRemindersMap.mapValues { entry -> entry.value.map { it.toString() } }}")

        val idealFutureDateTimesSet = mutableSetOf<LocalDateTime>()
        calculatedRemindersMap.forEach { (date, times) ->
            times.forEach { time ->
                val idealDateTime = LocalDateTime.of(date, time)
                val isToday = idealDateTime.toLocalDate().isEqual(now.toLocalDate())
                val twelveHoursAgoMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(12)

                val isValidForScheduling = if (isToday) {
                    // For today, allow if it's not older than 12 hours ago, or if it's in the future
                    idealDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() >= twelveHoursAgoMillis
                } else {
                    // For other days (future), it must be after now
                    idealDateTime.isAfter(now)
                }

                if (isValidForScheduling &&
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
        Log.d(funcTag, "Final sortedIdealFutureDateTimes (size ${sortedIdealFutureDateTimes.size}): ${sortedIdealFutureDateTimes.map { it.toString() }}")


        // 4. Schedule New/Missing Reminders
        sortedIdealFutureDateTimes.forEachIndexed { index, idealDateTime ->
            val existingReminderForThisTime = allExistingRemindersForPeriodMap[idealDateTime]

            if (existingReminderForThisTime == null) {
                Log.w(funcTag, "No existing reminder found in map for idealDateTime: $idealDateTime. Will schedule a new one.")
                // No reminder exists in DB for this ideal time, schedule a new one
                val reminderObjectToInsert = MedicationReminder(
                    medicationId = medication.id,
                    medicationScheduleId = schedule.id,
                    reminderTime = idealDateTime.format(storableDateTimeFormatter),
                    isTaken = false, takenAt = null, notificationId = null
                )
                val actualReminderIdFromDb = medicationReminderRepository.insertReminder(reminderObjectToInsert)
                Log.i(funcTag, "Scheduling NEW reminder for idealDateTime: $idealDateTime, actualReminderIdFromDb: $actualReminderIdFromDb")
                val reminderWithActualId = reminderObjectToInsert.copy(id = actualReminderIdFromDb.toInt())

                val actualScheduledTimeMillis = idealDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                var nextDoseTimeForHelperMillis: Long? = null
                if (index + 1 < sortedIdealFutureDateTimes.size) {
                    nextDoseTimeForHelperMillis = sortedIdealFutureDateTimes[index + 1]
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }

                val isIntervalType = schedule.scheduleType == ScheduleType.INTERVAL

                // New condition for actual alarm scheduling
                if (actualScheduledTimeMillis > System.currentTimeMillis()) {
                    Log.d(funcTag, "Preparing to schedule alarm with NotificationScheduler for future time: reminderId=${reminderWithActualId.id}, isInterval=$isIntervalType, nextDoseHelperMillis=$nextDoseTimeForHelperMillis, actualScheduledTimeMillis=$actualScheduledTimeMillis")
                    try {
                        notificationScheduler.scheduleNotification(
                            applicationContext, reminderWithActualId, medication.name, medication.dosage ?: "",
                            isIntervalType, nextDoseTimeForHelperMillis, actualScheduledTimeMillis
                        )
                        if (ENABLE_PRE_REMINDER_NOTIFICATION_FEATURE) {
                            // The pre-reminder target time is calculated inside schedulePreReminderServiceTrigger
                            // and it also checks if it's in the past. So, direct call is fine.
                            Log.d(funcTag, "Scheduling pre-reminder for reminderId ${reminderWithActualId.id}")
                            notificationScheduler.schedulePreReminderServiceTrigger(
                                applicationContext, reminderWithActualId, actualScheduledTimeMillis, medication.name
                            )
                        }
                        Log.i(funcTag, "Successfully scheduled main alarm (and pre-reminder if applicable) for NEW reminder ID ${reminderWithActualId.id} at $idealDateTime.")
                    } catch (e: IllegalStateException) {
                        Log.e(funcTag, "ALARM LIMIT EXCEPTION for new reminder ID ${reminderWithActualId.id}: ${e.message}", e)
                    } catch (e: Exception) {
                        Log.e(funcTag, "Generic error scheduling alarm for new reminder ID ${reminderWithActualId.id}", e)
                    }
                } else {
                    Log.w(funcTag, "Skipping actual alarm scheduling for reminder ID ${reminderWithActualId.id} because its time ($idealDateTime / $actualScheduledTimeMillis ms) is not in the future. DB entry was still created/updated.")
                }
            } else {
                // A reminder already exists for this ideal time
                if (existingReminderForThisTime.isTaken) {
                    Log.i(funcTag, "Ideal reminder at $idealDateTime already exists and IS TAKEN (ID: ${existingReminderForThisTime.id}). Doing nothing.")
                } else {
                    Log.i(funcTag, "Ideal reminder at $idealDateTime already exists and IS NOT TAKEN (ID: ${existingReminderForThisTime.id}). Doing nothing, should be already scheduled.")
                }
            }
        }

        // 5. Cleanup Stale Reminders (using existingFutureUntakenRemindersMap)
        // This logic remains the same, ensuring only UNTAKEN reminders that are no longer ideal are removed.
        existingFutureUntakenRemindersMap.forEach { (dateTime, existingUntakenReminder) -> // Variable name changed for clarity
            if (!idealFutureDateTimesSet.contains(dateTime)) {
                Log.i(funcTag, "STALE UNTAKEN reminder check: DateTime $dateTime (ReminderId: ${existingUntakenReminder.id}) is NOT in idealFutureDateTimesSet. Proceeding with deletion.")
                notificationScheduler.cancelAllAlarmsForReminder(applicationContext, existingUntakenReminder.id)
                medicationReminderRepository.deleteReminderById(existingUntakenReminder.id)
                Log.i(funcTag, "Successfully cancelled and deleted STALE UNTAKEN reminder ID ${existingUntakenReminder.id} for datetime $dateTime.")
            }
        }
        Log.i(funcTag, "Reminder scheduling/synchronization complete.")
    }
}