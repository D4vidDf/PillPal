package com.d4viddf.medicationreminder.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker // Sigue siendo un CoroutineWorker
import androidx.work.WorkerParameters
import com.d4viddf.medicationreminder.data.Medication // Your Medication data class
import com.d4viddf.medicationreminder.data.MedicationReminder // Your MedicationReminder data class
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationRepository
import com.d4viddf.medicationreminder.data.MedicationScheduleRepository
import com.d4viddf.medicationreminder.logic.ReminderCalculator // Your ReminderCalculator
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// QUITA @HiltWorker
class ReminderSchedulingWorker /* QUITA @AssistedInject */ constructor(
    // El constructor ahora toma las dependencias directamente
    // Mantenemos appContext y workerParams para la superclase
    appContext: Context, // No necesitas @Assisted aquí
    workerParams: WorkerParameters, // No necesitas @Assisted aquí
    private val medicationRepository: MedicationRepository,
    private val medicationScheduleRepository: MedicationScheduleRepository,
    private val medicationReminderRepository: MedicationReminderRepository,
    private val notificationScheduler: NotificationScheduler
) : CoroutineWorker(appContext, workerParams) { // Pasa appContext y workerParams al constructor de CoroutineWorker

    companion object {
        const val WORK_NAME_PREFIX = "ReminderSchedulingWorker_"
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_IS_DAILY_REFRESH = "is_daily_refresh"
        private const val TAG = "ReminderSchedWorker"
        private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "ReminderSchedulingWorker (via CustomFactory) started")
        val medicationId = inputData.getInt(KEY_MEDICATION_ID, -1)
        val isDailyRefresh = inputData.getBoolean(KEY_IS_DAILY_REFRESH, false)

        return try {
            if (isDailyRefresh) {
                Log.d(TAG, "Performing daily refresh for all medications.")
                val allMedications = medicationRepository.getAllMedications().firstOrNull() ?: emptyList()
                allMedications.forEach { medication ->
                    processMedication(medication, LocalDate.now(), LocalDate.now().plusDays(2))
                }
            } else if (medicationId != -1) {
                Log.d(TAG, "Processing medication ID: $medicationId")
                // Asegúrate de que el contexto de la corrutina sea el adecuado si getMedicationById es suspend
                medicationRepository.getMedicationById(medicationId)?.let { medication ->
                    val startDate = medication.startDate?.let { LocalDate.parse(it, ReminderCalculator.dateStorableFormatter) } ?: LocalDate.now()
                    val endDate = medication.endDate?.let { LocalDate.parse(it, ReminderCalculator.dateStorableFormatter) } ?: startDate.plusMonths(1)
                    processMedication(medication, startDate, endDate)
                } ?: Log.e(TAG, "Medication not found for ID: $medicationId")
            } else {
                Log.e(TAG, "No medication ID provided and not a daily refresh.")
                return Result.failure()
            }
            Log.d(TAG, "ReminderSchedulingWorker (via CustomFactory) finished successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ReminderSchedulingWorker (via CustomFactory)", e)
            Result.failure()
        }
    }

    private suspend fun processMedication(
        medication: Medication,
        periodStart: LocalDate,
        periodEnd: LocalDate
    ) {
        Log.d(TAG, "Processing ${medication.name} from $periodStart to $periodEnd (CustomFactory)")
        val schedule = medicationScheduleRepository.getSchedulesForMedication(medication.id)
            .firstOrNull()
            ?.firstOrNull()

        if (schedule == null) {
            Log.w(TAG, "No schedule found for medication ID: ${medication.id} (CustomFactory)")
            return
        }

        val calculatedRemindersMap = ReminderCalculator.generateRemindersForPeriod(
            medication,
            schedule,
            periodStart,
            periodEnd
        )

        Log.d(TAG, "Calculated reminders for ${medication.name}: $calculatedRemindersMap (CustomFactory)")

        calculatedRemindersMap.forEach { (date, times) ->
            times.forEach { time ->
                val reminderDateTime = LocalDateTime.of(date, time)
                var reminderToSchedule = MedicationReminder( // Use var to update its ID
                    medicationId = medication.id,
                    medicationScheduleId = schedule.id,
                    reminderTime = reminderDateTime.format(storableDateTimeFormatter),
                    isTaken = false,
                    takenAt = null,
                    notificationId = null
                )
                Log.d(TAG, "Object before insert: $reminderToSchedule (CustomFactory)")

                val insertedReminderId = medicationReminderRepository.insertReminder(reminderToSchedule) // Get the returned ID

                // Update the reminderToSchedule object with the actual ID from the database
                // This is crucial for NotificationScheduler to use the correct ID for PendingIntents
                reminderToSchedule = reminderToSchedule.copy(id = insertedReminderId.toInt())

                Log.d(TAG, "Object after insert with ID: $reminderToSchedule (CustomFactory)")

                notificationScheduler.scheduleNotification(
                    applicationContext,
                    reminderToSchedule, // Pass the reminder object that now has the correct ID
                    medication.name,
                    medication.dosage ?: ""
                )
            }
        }
    }
}