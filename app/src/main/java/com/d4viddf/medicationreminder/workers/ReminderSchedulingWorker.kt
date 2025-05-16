package com.d4viddf.medicationreminder.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationRepository
import com.d4viddf.medicationreminder.data.MedicationSchedule // Asegúrate que esta entidad esté definida
import com.d4viddf.medicationreminder.data.MedicationScheduleRepository
import com.d4viddf.medicationreminder.data.ScheduleType // Asegúrate que este enum esté definido
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderSchedulingWorker constructor(
    // El applicationContext se obtiene de CoroutineWorker
    appContext: Context,
    workerParams: WorkerParameters,
    private val medicationRepository: MedicationRepository,
    private val medicationScheduleRepository: MedicationScheduleRepository,
    private val medicationReminderRepository: MedicationReminderRepository,
    private val notificationScheduler: NotificationScheduler
) : CoroutineWorker(appContext, workerParams) { // CoroutineWorker ya tiene applicationContext

    companion object {
        const val WORK_NAME_PREFIX = "ReminderSchedulingWorker_"
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_IS_DAILY_REFRESH = "is_daily_refresh"
        private const val TAG = "ReminderSchedWorker"
        private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
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
                    // Solo procesar si la medicación está activa (sin fecha de fin o fecha de fin no pasada)
                    val medicationEndDate = medication.endDate?.let { LocalDate.parse(it, ReminderCalculator.dateStorableFormatter) }
                    if (medicationEndDate == null || !LocalDate.now().isAfter(medicationEndDate)) {
                        scheduleNextReminderForMedication(medication)
                    } else {
                        Log.d(TAG, "Skipping daily refresh for ${medication.name}, medication period ended.")
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

    private suspend fun scheduleNextReminderForMedication(medication: Medication) {
        Log.i(TAG, "Attempting to schedule next reminder for ${medication.name} (ID: ${medication.id})")

        val schedule: MedicationSchedule? = medicationScheduleRepository.getSchedulesForMedication(medication.id)
            .firstOrNull()?.firstOrNull()

        if (schedule == null) {
            Log.w(TAG, "No schedule found for medication ID: ${medication.id}. Cannot schedule next reminder.")
            return
        }

        // Determinar el punto de partida para la búsqueda del próximo recordatorio
        val now = LocalDateTime.now()
        val medicationStartDate = medication.startDate?.let { ReminderCalculator.dateStorableFormatter.parse(it, LocalDate::from) } ?: now.toLocalDate()
        val medicationEndDate = medication.endDate?.let { ReminderCalculator.dateStorableFormatter.parse(it, LocalDate::from) }

        // Si la medicación ha terminado, no programar nada.
        if (medicationEndDate != null && now.toLocalDate().isAfter(medicationEndDate)) {
            Log.i(TAG, "Medication ${medication.name} period has ended. No new reminders will be scheduled.")
            // TODO: Considerar limpiar alarmas/notificaciones existentes si la medicación ha terminado.
            return
        }

        // El punto desde el cual buscar el próximo recordatorio:
        // - Si la medicación aún no ha comenzado, desde el inicio de la fecha de inicio.
        // - Si ya comenzó, desde ahora mismo.
        val searchFromDateTime = if (medicationStartDate.isAfter(now.toLocalDate())) {
            medicationStartDate.atStartOfDay()
        } else {
            now
        }

        // Ventana de búsqueda para el próximo recordatorio: desde searchFromDateTime hasta unos días en el futuro,
        // o hasta la fecha de finalización de la medicación si está definida y es anterior.
        var searchWindowEnd = searchFromDateTime.toLocalDate().plusDays(3) // Ventana por defecto de 3 días
        if (medicationEndDate != null && medicationEndDate.isBefore(searchWindowEnd)) {
            searchWindowEnd = medicationEndDate
        }
        // Si la fecha de fin es hoy, la ventana de búsqueda termina al final de hoy.
        if (medicationEndDate != null && medicationEndDate.isEqual(searchFromDateTime.toLocalDate())) {
            searchWindowEnd = medicationEndDate
        }


        Log.d(TAG, "Search window for ${medication.name}: From ${searchFromDateTime.toLocalDate()} to $searchWindowEnd")

        val calculatedRemindersMap = ReminderCalculator.generateRemindersForPeriod(
            medication,
            schedule,
            searchFromDateTime.toLocalDate(), // Comenzar búsqueda desde esta fecha
            searchWindowEnd
        )

        var nextReminderDateTimeToSchedule: LocalDateTime? = null

        // Encontrar el primer recordatorio que sea estrictamente después de searchFromDateTime
        // (que es 'ahora' o el inicio futuro de la medicación)
        outerLoop@ for (date in calculatedRemindersMap.keys.sorted()) {
            if (date.isBefore(searchFromDateTime.toLocalDate())) continue // Optimización: saltar fechas pasadas
            for (time in calculatedRemindersMap[date]?.sorted() ?: emptyList()) {
                val currentReminderCandidate = LocalDateTime.of(date, time)
                if (currentReminderCandidate.isAfter(searchFromDateTime)) {
                    // Adicionalmente, asegurarse de que no está después de la fecha de fin de la medicación
                    if (medicationEndDate == null || !currentReminderCandidate.toLocalDate().isAfter(medicationEndDate)) {
                        nextReminderDateTimeToSchedule = currentReminderCandidate
                        break@outerLoop // Encontramos el primero, salimos de ambos bucles
                    }
                }
            }
        }

        if (nextReminderDateTimeToSchedule == null) {
            Log.i(TAG, "No upcoming reminders found to schedule for ${medication.name} in the defined window.")
            // TODO: Podríamos necesitar limpiar recordatorios existentes en la BD que ya no son válidos
            // o que fueron para un "siguiente" que ahora está en el pasado.
            return
        }

        Log.i(TAG, "Next reminder for ${medication.name} (ID: ${medication.id}) identified at: $nextReminderDateTimeToSchedule")

        // --- Limpieza de recordatorios futuros existentes para esta medicación ---
        // Antes de insertar el nuevo "próximo" recordatorio, cancela y elimina cualquier otro
        // recordatorio futuro pendiente para esta medicación para evitar duplicados y el límite de alarmas.
        val existingFutureReminders = medicationReminderRepository.getFutureRemindersForMedication(medication.id, now.format(storableDateTimeFormatter))
            .firstOrNull() // Asumiendo que getFutureRemindersForMedication devuelve Flow<List<MedicationReminder>>

        existingFutureReminders?.forEach { existingReminder ->
            Log.d(TAG, "Cancelling existing future reminder ID: ${existingReminder.id} for med ID: ${medication.id}")
            notificationScheduler.cancelAlarmAndNotification(applicationContext, existingReminder.id)
            medicationReminderRepository.deleteReminder(existingReminder) // Necesitarás este método
        }
        // --- Fin de la limpieza ---


        val reminderObjectToInsert = MedicationReminder(
            medicationId = medication.id,
            medicationScheduleId = schedule.id,
            reminderTime = nextReminderDateTimeToSchedule.format(storableDateTimeFormatter),
            isTaken = false,
            takenAt = null,
            notificationId = null
        )

        val actualReminderIdFromDb = medicationReminderRepository.insertReminder(reminderObjectToInsert)
        val reminderWithActualId = reminderObjectToInsert.copy(id = actualReminderIdFromDb.toInt())

        var isInterval = false
        var nextDoseTimeForHelperMillis: Long? = null
        val actualReminderTimeMillis = nextReminderDateTimeToSchedule.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()


        if (schedule.scheduleType == ScheduleType.INTERVAL) {
            isInterval = true
            // Calcular la siguiente dosis DESPUÉS de la que se acaba de programar
            val searchStartForFollowing = nextReminderDateTimeToSchedule.plusSeconds(1)
            val searchEndForFollowingInterval = medicationEndDate ?: searchStartForFollowing.toLocalDate().plusDays(3) // Ventana similar

            val followingRemindersMap = ReminderCalculator.generateRemindersForPeriod(
                medication,
                schedule,
                searchStartForFollowing.toLocalDate(),
                searchEndForFollowingInterval
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
            nextDoseTimeForHelperMillis = followingReminderDateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

            if (nextDoseTimeForHelperMillis != null) {
                Log.d(TAG, "For interval, next actual dose (after current one) for helper: ${LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextDoseTimeForHelperMillis), ZoneId.systemDefault())}")
            } else {
                Log.d(TAG, "For interval, no subsequent dose found for helper for med ID ${medication.id}.")
            }
        }

        Log.d(TAG, "Scheduling with NotificationScheduler: reminderId=${reminderWithActualId.id}, isInterval=$isInterval, nextDoseHelperMillis=$nextDoseTimeForHelperMillis, actualTimeMillis=$actualReminderTimeMillis")
        try {
            notificationScheduler.scheduleNotification(
                applicationContext, // Usar el applicationContext inyectado
                reminderWithActualId,
                medication.name,
                medication.dosage ?: "",
                isInterval,
                nextDoseTimeForHelperMillis,
                actualReminderTimeMillis // Añadido para claridad, aunque NotificationScheduler ya lo calcula
            )
        } catch (e: IllegalStateException) {
            Log.e(TAG, "ALARM LIMIT EXCEPTION for reminder ID ${reminderWithActualId.id}: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Generic error scheduling alarm for reminder ID ${reminderWithActualId.id}", e)
        }
    }
}