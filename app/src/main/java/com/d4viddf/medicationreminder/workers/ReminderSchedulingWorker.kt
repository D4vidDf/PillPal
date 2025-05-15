
package com.d4viddf.medicationreminder.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.d4viddf.medicationreminder.data.Medication // Your Medication data class
import com.d4viddf.medicationreminder.data.MedicationReminder // Your MedicationReminder data class
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationRepository
import com.d4viddf.medicationreminder.data.MedicationScheduleRepository
import com.d4viddf.medicationreminder.logic.ReminderCalculator // Your ReminderCalculator
import com.d4viddf.medicationreminder.notifications.NotificationScheduler // We'll create this later
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltWorker
class ReminderSchedulingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val medicationRepository: MedicationRepository,
    private val medicationScheduleRepository: MedicationScheduleRepository, // Assuming you have this
    private val medicationReminderRepository: MedicationReminderRepository,
    private val notificationScheduler: NotificationScheduler // To be created
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME_PREFIX = "ReminderSchedulingWorker_"
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_IS_DAILY_REFRESH = "is_daily_refresh"
        private const val TAG = "ReminderSchedWorker"
        private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME // For MedicationReminder.reminderTime

    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork started")
        val medicationId = inputData.getInt(KEY_MEDICATION_ID, -1)
        val isDailyRefresh = inputData.getBoolean(KEY_IS_DAILY_REFRESH, false)

        return try {
            if (isDailyRefresh) {
                Log.d(TAG, "Performing daily refresh for all medications.")
                val allMedications = medicationRepository.getAllMedications().firstOrNull() ?: emptyList()
                allMedications.forEach { medication ->
                    processMedication(medication, LocalDate.now(), LocalDate.now().plusDays(2)) // Schedule for today and next 2 days
                }
            } else if (medicationId != -1) {
                Log.d(TAG, "Processing medication ID: $medicationId")
                medicationRepository.getMedicationById(medicationId)?.let { medication ->
                    val startDate = medication.startDate?.let { LocalDate.parse(it, ReminderCalculator.dateStorableFormatter) } ?: LocalDate.now()
                    val endDate = medication.endDate?.let { LocalDate.parse(it, ReminderCalculator.dateStorableFormatter) } ?: startDate.plusMonths(1) // Default: schedule for 1 month
                    processMedication(medication, startDate, endDate)
                } ?: Log.e(TAG, "Medication not found for ID: $medicationId")
            } else {
                Log.e(TAG, "No medication ID provided and not a daily refresh.")
                return Result.failure()
            }
            Log.d(TAG, "doWork finished successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ReminderSchedulingWorker", e)
            Result.failure()
        }
    }

    private suspend fun processMedication(
        medication: Medication,
        periodStart: LocalDate,
        periodEnd: LocalDate
    ) {
        Log.d(TAG, "Processing ${medication.name} from $periodStart to $periodEnd")
        // Assuming medicationScheduleRepository.getSchedulesForMedication returns a Flow<List<MedicationSchedule>>
        // and a medication typically has one active schedule.
        val schedule = medicationScheduleRepository.getSchedulesForMedication(medication.id)
            .firstOrNull() // Take the first list emitted
            ?.firstOrNull() // Take the first schedule in that list

        if (schedule == null) {
            Log.w(TAG, "No schedule found for medication ID: ${medication.id}")
            return
        }

        val calculatedRemindersMap = ReminderCalculator.generateRemindersForPeriod(
            medication,
            schedule,
            periodStart,
            periodEnd
        )

        Log.d(TAG, "Calculated reminders for ${medication.name}: $calculatedRemindersMap")


        calculatedRemindersMap.forEach { (date, times) ->
            times.forEach { time ->
                val reminderDateTime = LocalDateTime.of(date, time)
                // TODO: Check if a reminder for this medicationId, scheduleId, and reminderTime already exists
                // to avoid duplicates if the worker runs multiple times for the same period.
                // This might require querying MedicationReminderDao.

                val newReminder = MedicationReminder(
                    medicationId = medication.id,
                    medicationScheduleId = schedule.id, // Assuming one schedule per medication for now
                    reminderTime = reminderDateTime.format(storableDateTimeFormatter), // Store as ISO String
                    isTaken = false,
                    takenAt = null,
                    notificationId = null // Notification ID will be generated when scheduling with AlarmManager
                )
                Log.d(TAG, "Inserting new reminder: $newReminder")
                medicationReminderRepository.insertReminder(newReminder) // Save to DB

                // Schedule with AlarmManager (via NotificationScheduler)
                // The NotificationScheduler will handle generating a unique notificationId
                // and creating the PendingIntent for the BroadcastReceiver.
                notificationScheduler.scheduleNotification(applicationContext, newReminder, medication.name, medication.dosage ?: "")
            }
        }
    }
}