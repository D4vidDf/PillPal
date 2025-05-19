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

        val searchFromDateTime = if (medicationStartDate.isAfter(now.toLocalDate())) medicationStartDate.atStartOfDay() else now
        var searchWindowEnd = searchFromDateTime.toLocalDate().plusDays(3)
        if (medicationEndDate != null && medicationEndDate.isBefore(searchWindowEnd)) {
            searchWindowEnd = medicationEndDate
        }
        if (medicationEndDate != null && medicationEndDate.isEqual(searchFromDateTime.toLocalDate())) {
            searchWindowEnd = medicationEndDate
        }

        Log.d(TAG, "Search window for ${medication.name}: From ${searchFromDateTime.toLocalDate()} to $searchWindowEnd")

        val calculatedRemindersMap = ReminderCalculator.generateRemindersForPeriod(
            medication, schedule, searchFromDateTime.toLocalDate(), searchWindowEnd
        )

        var nextReminderDateTimeToSchedule: LocalDateTime? = null
        outerLoop@ for (date in calculatedRemindersMap.keys.sorted()) {
            if (date.isBefore(searchFromDateTime.toLocalDate())) continue
            for (time in calculatedRemindersMap[date]?.sorted() ?: emptyList()) {
                val currentReminderCandidate = LocalDateTime.of(date, time)
                if (currentReminderCandidate.isAfter(searchFromDateTime)) { // Estrictamente después de 'ahora' o inicio de medicación
                    if (medicationEndDate == null || !currentReminderCandidate.toLocalDate().isAfter(medicationEndDate)) {
                        nextReminderDateTimeToSchedule = currentReminderCandidate
                        break@outerLoop
                    }
                }
            }
        }

        // Limpieza de recordatorios futuros existentes ANTES de programar el nuevo
        val existingFutureReminders = medicationReminderRepository.getFutureUntakenRemindersForMedication(
            medication.id, now.format(storableDateTimeFormatter)
        ).firstOrNull()

        existingFutureReminders?.forEach { existingReminder ->
            Log.d(TAG, "Pre-emptive cleanup: Cancelling existing future reminder ID: ${existingReminder.id} for med ID: ${medication.id} at ${existingReminder.reminderTime}")
            notificationScheduler.cancelAllAlarmsForReminder(applicationContext, existingReminder.id)
            medicationReminderRepository.deleteReminderById(existingReminder.id)
        }

        if (nextReminderDateTimeToSchedule == null) {
            Log.i(TAG, "No upcoming reminders found to schedule for ${medication.name} after cleanup/filtering.")
            return
        }

        Log.i(TAG, "FINAL Next reminder for ${medication.name} (ID: ${medication.id}) to be scheduled at: $nextReminderDateTimeToSchedule")

        val reminderObjectToInsert = MedicationReminder(
            medicationId = medication.id,
            medicationScheduleId = schedule.id,
            reminderTime = nextReminderDateTimeToSchedule.format(storableDateTimeFormatter),
            isTaken = false, takenAt = null, notificationId = null
        )

        val actualReminderIdFromDb = medicationReminderRepository.insertReminder(reminderObjectToInsert)
        val reminderWithActualId = reminderObjectToInsert.copy(id = actualReminderIdFromDb.toInt())

        var isInterval = false
        var nextDoseTimeForHelperMillis: Long? = null
        val actualScheduledTimeMillis = nextReminderDateTimeToSchedule.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (schedule.scheduleType == ScheduleType.INTERVAL) {
            isInterval = true
            val searchStartForFollowing = nextReminderDateTimeToSchedule.plusSeconds(1)
            val searchEndForFollowingInterval = medicationEndDate ?: searchStartForFollowing.toLocalDate().plusDays(3)
            val followingRemindersMap = ReminderCalculator.generateRemindersForPeriod(
                medication, schedule, searchStartForFollowing.toLocalDate(), searchEndForFollowingInterval
            )
            var followingReminderDateTime: LocalDateTime? = null
            outerLoopFollowing@ for (date in followingRemindersMap.keys.sorted()) {
                if (date.isBefore(searchStartForFollowing.toLocalDate())) continue
                for (time in followingRemindersMap[date]?.sorted() ?: emptyList()) {
                    val candidate = LocalDateTime.of(date, time)
                    if (candidate.isAfter(searchStartForFollowing)) {
                        if (medicationEndDate == null || !candidate.toLocalDate().isAfter(medicationEndDate)) {
                            followingReminderDateTime = candidate
                            break@outerLoopFollowing
                        }
                    }
                }
            }
            nextDoseTimeForHelperMillis =
                followingReminderDateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            Log.d(TAG, "For interval, nextDoseTimeForHelperMillis: $nextDoseTimeForHelperMillis for med ID ${medication.id}.")
        }

        Log.d(TAG, "Scheduling MAIN reminder with NotificationScheduler: reminderId=${reminderWithActualId.id}, isInterval=$isInterval, nextDoseHelperMillis=$nextDoseTimeForHelperMillis, actualTimeMillis=$actualScheduledTimeMillis")
        try {
            notificationScheduler.scheduleNotification(
                applicationContext, reminderWithActualId, medication.name, medication.dosage ?: "",
                isInterval, nextDoseTimeForHelperMillis, actualScheduledTimeMillis
            )

            // Programar la notificación previa si está habilitada
            if (ENABLE_PRE_REMINDER_NOTIFICATION_FEATURE) {
                val preReminderTargetTimeMillis = actualScheduledTimeMillis - TimeUnit.MINUTES.toMillis(PRE_REMINDER_OFFSET_MINUTES)
                if (preReminderTargetTimeMillis > System.currentTimeMillis()){ // Solo si la hora previa es en el futuro
                    Log.d(TAG, "Scheduling PRE-REMINDER service trigger for reminderId=${reminderWithActualId.id}")
                    notificationScheduler.schedulePreReminderServiceTrigger(
                        applicationContext, reminderWithActualId, actualScheduledTimeMillis, // Pasa la hora de la toma real
                        medication.name
                    )
                } else {
                    Log.d(TAG, "Pre-reminder time is in the past for reminderId=${reminderWithActualId.id}, not scheduling pre-reminder service.")
                }
            }

        } catch (e: IllegalStateException) {
            Log.e(TAG, "ALARM LIMIT EXCEPTION for reminder ID ${reminderWithActualId.id}: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Generic error scheduling alarm for reminder ID ${reminderWithActualId.id}", e)
        }
    }
}