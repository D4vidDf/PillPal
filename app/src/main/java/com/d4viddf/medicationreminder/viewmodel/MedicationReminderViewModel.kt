package com.d4viddf.medicationreminder.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
import com.d4viddf.medicationreminder.data.TodayScheduleItem
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.workers.ReminderSchedulingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MedicationReminderViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val reminderRepository: MedicationReminderRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _allRemindersForSelectedMedication = MutableStateFlow<List<MedicationReminder>>(emptyList())
    val allRemindersForSelectedMedication: StateFlow<List<MedicationReminder>> = _allRemindersForSelectedMedication

    private val _todaysRemindersForSelectedMedication = MutableStateFlow<List<MedicationReminder>>(emptyList())
    val todaysRemindersForSelectedMedication: StateFlow<List<MedicationReminder>> = _todaysRemindersForSelectedMedication

    private val _todayScheduleItems = MutableStateFlow<List<TodayScheduleItem>>(emptyList())
    val todayScheduleItems: StateFlow<List<TodayScheduleItem>> = _todayScheduleItems.asStateFlow()

    private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME


    fun loadRemindersForMedication(medicationId: Int) {
        viewModelScope.launch(Dispatchers.IO) { // Las operaciones de BD en IO
            reminderRepository.getRemindersForMedication(medicationId).collect { allRemindersList ->
                val sortedAllReminders = allRemindersList.sortedBy { it.reminderTime }
                _allRemindersForSelectedMedication.value = sortedAllReminders

                val today = LocalDate.now()
                _todaysRemindersForSelectedMedication.value = sortedAllReminders.filter {
                    try {
                        LocalDateTime.parse(it.reminderTime, storableDateTimeFormatter).toLocalDate().isEqual(today)
                    } catch (e: Exception) {
                        Log.e("MedReminderVM", "Error parsing reminderTime: ${it.reminderTime}", e)
                        false
                    }
                }
                Log.d("MedReminderVM", "Loaded ${allRemindersList.size} total reminders, ${todaysRemindersForSelectedMedication.value.size} for today (medId: $medicationId).")
            }
        }
    }

    suspend fun getAllRemindersForMedicationOnce(medicationId: Int): List<MedicationReminder> {
        return withContext(Dispatchers.IO) {
            reminderRepository.getRemindersForMedication(medicationId).firstOrNull()?.sortedBy { it.reminderTime } ?: emptyList()
        }
    }

    fun markReminderAsTaken(reminderId: Int, takenAt: String) {

        viewModelScope.launch {

            reminderRepository.markReminderAsTaken(reminderId, takenAt)

        }

    }

    fun markReminderAsTakenAndUpdateLists(reminderId: Int, medicationId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val nowString = LocalDateTime.now().format(storableDateTimeFormatter)
            Log.d("MedReminderVM", "Marking reminderId $reminderId (medId $medicationId) as taken at $nowString")
            reminderRepository.markReminderAsTaken(reminderId, nowString)
            triggerNextReminderScheduling(medicationId) // Llama sin pasar el contexto
        }
    }

    internal fun triggerNextReminderScheduling(medicationId: Int) {
        Log.d("MedReminderVM", "Triggering next reminder scheduling for med ID: $medicationId using injected appContext")
        val workManager = WorkManager.getInstance(this.appContext) // Usa this.appContext
        val data = Data.Builder()
            .putInt(ReminderSchedulingWorker.KEY_MEDICATION_ID, medicationId)
            .putBoolean(ReminderSchedulingWorker.KEY_IS_DAILY_REFRESH, false)
            .build()
        val scheduleNextWorkRequest =
            OneTimeWorkRequestBuilder<ReminderSchedulingWorker>()
                .setInputData(data)
                .addTag("${ReminderSchedulingWorker.WORK_NAME_PREFIX}NextFromDetail_${medicationId}")
                .build()
        workManager.enqueueUniqueWork(
            "${ReminderSchedulingWorker.WORK_NAME_PREFIX}NextScheduledFromDetail_${medicationId}",
            ExistingWorkPolicy.REPLACE,
            scheduleNextWorkRequest
        )
        Log.i("MedReminderVM", "Enqueued ReminderSchedulingWorker for med ID $medicationId.")
    }

    // Function to Fetch Today's Reminders for a specific medication
    fun loadTodaySchedule(medicationId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val medication = medicationRepository.getMedicationById(medicationId)
            if (medication == null) {
                Log.w("MedReminderVM", "loadTodaySchedule: Medication with ID $medicationId not found.")
                _todayScheduleItems.value = emptyList()
                return@launch
            }

            val schedules = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()
            if (schedules.isNullOrEmpty()) {
                Log.i("MedReminderVM", "loadTodaySchedule: No schedules found for medication ID $medicationId.")
                _todayScheduleItems.value = emptyList()
                return@launch
            }

            val existingReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
            val allTodayScheduleItems = mutableListOf<TodayScheduleItem>()
            val today = LocalDate.now()
            val currentTime = LocalTime.now()

            schedules.forEach { schedule ->
                // Strategy 1: Deduplicate reminder times from ReminderCalculator for a single schedule
                val distinctReminderDateTimesToday = ReminderCalculator.calculateReminderDateTimes(medication, schedule, today).distinct()
                Log.d("MedReminderVM", "Medication: ${medication.name}, Schedule ID: ${schedule.id}, Type: ${schedule.scheduleType}, Calculated DTs for today: ${distinctReminderDateTimesToday.size} (after distinct)")

                distinctReminderDateTimesToday.forEach { reminderDateTime ->
                    val reminderTimeStr = reminderDateTime.format(ReminderCalculator.storableDateTimeFormatter)
                    val existingReminder = existingReminders.find { it.medicationId == medicationId && it.reminderTime == reminderTimeStr }

                    val scheduleItem = TodayScheduleItem(
                        id = "${medicationId}_${schedule.id}_${reminderDateTime.toLocalTime().toNanoOfDay()}",
                        medicationName = medication.name,
                        time = reminderDateTime.toLocalTime(),
                        isPast = currentTime.isAfter(reminderDateTime.toLocalTime()),
                        isTaken = existingReminder?.isTaken ?: false,
                        underlyingReminderId = existingReminder?.id?.toLong() ?: 0L, // Use existing reminder ID or 0L
                        medicationScheduleId = schedule.id // Populate the new field
                    )
                    allTodayScheduleItems.add(scheduleItem)
                }
            }

            // Strategy 2: Deduplicate the final list of TodayScheduleItem objects
            val uniqueTodayScheduleItems = allTodayScheduleItems.toSet().toList()
            _todayScheduleItems.value = uniqueTodayScheduleItems.sortedBy { it.time }
            Log.d("MedReminderVM", "Updated TodayScheduleItems for medId: $medicationId with ${uniqueTodayScheduleItems.size} unique items.")
        }
    }

    // Function to Add Past Medication Taken
    fun addPastMedicationTaken(medicationId: Int, medicationNameParam: String, date: LocalDate, time: LocalTime) {
        viewModelScope.launch(Dispatchers.IO) {
            val pastDateTime = LocalDateTime.of(date, time)
            if (pastDateTime.isAfter(LocalDateTime.now())) {
                Log.e("MedReminderVM", "Cannot add past medication taken: Date and time are in the future. MedId: $medicationId, DateTime: $pastDateTime")
                return@launch
            }

            val medication = medicationRepository.getMedicationById(medicationId)
            if (medication == null) {
                Log.e("MedReminderVM", "Cannot add past medication taken: Medication with ID $medicationId not found.")
                return@launch
            }

            val schedules = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()
            var matchingScheduleId: Int = 0 // Default to 0 if no schedule matches, as MedicationReminder.medicationScheduleId is non-nullable.
                                          // This assumes 0 is a valid reference or a convention for "no specific schedule" / "manual log".
                                          // A proper solution might involve a nullable field or a dedicated "manual" schedule entry in DB.

            if (!schedules.isNullOrEmpty()) {
                for (schedule in schedules) {
                    val scheduledTimesOnPastDate = ReminderCalculator.calculateReminderDateTimes(medication, schedule, date)
                    if (scheduledTimesOnPastDate.any { it.toLocalTime() == time }) {
                        matchingScheduleId = schedule.id
                        Log.d("MedReminderVM", "Found matching schedule (ID: $matchingScheduleId) for past dose.")
                        break
                    }
                }
            }
            if (matchingScheduleId == 0) {
                Log.i("MedReminderVM", "No specific schedule matched for past dose. Logging with scheduleId 0.")
            }

            val newReminder = MedicationReminder(
                id = 0, // Auto-generated by Room
                medicationId = medicationId,
                medicationScheduleId = if (matchingScheduleId == 0) null else matchingScheduleId, // Use null if no match
                reminderTime = pastDateTime.format(ReminderCalculator.storableDateTimeFormatter), // This is effectively the "taken time"
                isTaken = true,
                takenAt = pastDateTime.format(ReminderCalculator.storableDateTimeFormatter), // Explicitly log when it was marked as taken (which is the past time provided)
                notificationId = null // No notification for a past, manually added dose
            )

            try {
                reminderRepository.insertReminder(newReminder)
                Log.d("MedReminderVM", "Inserted past medication taken for medId: $medicationId at $pastDateTime, linked to scheduleId: ${newReminder.medicationScheduleId}.")

                if (date.isEqual(LocalDate.now())) {
                    loadTodaySchedule(medicationId) // Refresh the list if the added dose was for today
                    Log.d("MedReminderVM", "Refreshed today's schedule after adding past medication for today.")
                }
                // Successfully inserted past medication taken, now trigger worker
                triggerNextReminderScheduling(medicationId)
                Log.i("MedReminderVM", "Triggered ReminderSchedulingWorker for medId: $medicationId after adding past taken dose.")

            } catch (e: Exception) {
                Log.e("MedReminderVM", "Error inserting past medication taken for medId: $medicationId", e)
                // Optionally, communicate error to UI
            }
        }
    }

    // Function to Update Reminder Status (Toggle)
    fun updateReminderStatus(itemId: String, isTaken: Boolean, medicationId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val todayItem = _todayScheduleItems.value.find { it.id == itemId }
            if (todayItem == null) {
                Log.e("MedReminderVM", "updateReminderStatus: TodayScheduleItem not found for ID: $itemId. Cannot update status.")
                return@launch
            }

            val reminderDateTime = LocalDateTime.of(LocalDate.now(), todayItem.time)
            val reminderTimeStr = reminderDateTime.format(ReminderCalculator.storableDateTimeFormatter)
            // Attempt to find existing reminder by medicationId and exact time string.
            // This might require a new DAO method: getReminderByMedicationIdAndExactTimeString(medId: Int, timeStr: String)
            // For now, we fetch all and filter, which is less efficient but works with current repo methods.
            val existingReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
            var reminderInDb = existingReminders.find { it.reminderTime == reminderTimeStr && it.medicationId == medicationId }

            if (isTaken) {
                if (reminderInDb != null) {
                    // Update existing reminder
                    val updatedReminder = reminderInDb.copy(
                        isTaken = true,
                        takenAt = LocalDateTime.now().format(ReminderCalculator.storableDateTimeFormatter)
                    )
                    reminderRepository.updateReminder(updatedReminder)
                    Log.d("MedReminderVM", "Marked existing reminder (ID: ${reminderInDb.id}) as taken for item: $itemId.")
                } else {
                    // Create new reminder
                    val newReminder = MedicationReminder(
                        id = 0, // Auto-generated
                        medicationId = medicationId,
                        medicationScheduleId = todayItem.medicationScheduleId,
                        reminderTime = reminderTimeStr,
                        isTaken = true,
                        takenAt = LocalDateTime.now().format(ReminderCalculator.storableDateTimeFormatter),
                        notificationId = null // Or manage if notifications are linked
                    )
                    reminderRepository.insertReminder(newReminder)
                    Log.d("MedReminderVM", "Created and marked new reminder as taken for item: $itemId.")
                }

                // After successfully marking as taken (either update or insert)
                val medication = medicationRepository.getMedicationById(medicationId)
                val schedule = medication?.id?.let { scheduleRepository.getSchedulesForMedication(it).firstOrNull()?.firstOrNull() }

                if (schedule?.scheduleType == com.d4viddf.medicationreminder.data.ScheduleType.INTERVAL) {
                    triggerNextReminderScheduling(medicationId)
                    Log.i("MedReminderVM", "Triggered ReminderSchedulingWorker for medId: $medicationId after marking as taken (interval schedule). ItemId: $itemId")
                } else {
                    Log.d("MedReminderVM", "Did not trigger ReminderSchedulingWorker for medId: $medicationId. Schedule type is not INTERVAL or schedule/medication not found. ItemId: $itemId, ScheduleType: ${schedule?.scheduleType}")
                }

            } else { // Unmarking as taken
                if (reminderInDb != null) {
                    val updatedReminder = reminderInDb.copy(
                        isTaken = false,
                        takenAt = null
                    )
                    reminderRepository.updateReminder(updatedReminder)
                    Log.d("MedReminderVM", "Marked existing reminder (ID: ${reminderInDb.id}) as NOT taken for item: $itemId.")
                } else {
                    // This case implies an inconsistency or an attempt to unmark a conceptual reminder that was never saved.
                    Log.w("MedReminderVM", "Attempted to unmark a reminder that doesn't exist in DB. ItemId: $itemId, MedId: $medicationId, Time: $reminderDateTime")
                    // Optionally, if a TodayScheduleItem can be "taken" conceptually without a DB record yet,
                    // and then untaken, we might not need to do anything here if it was never in DB.
                    // However, our current loadTodaySchedule implies underlyingReminderId would be 0L if not in DB.
                    // If underlyingReminderId is not 0L, it means it *should* exist.
                    if (todayItem.underlyingReminderId != 0L) {
                         Log.e("MedReminderVM", "Inconsistency: Tried to unmark item $itemId with underlyingReminderId ${todayItem.underlyingReminderId} but not found in DB via time match.")
                    }
                }
            }
            // Refresh the entire list to ensure isTaken status and underlyingReminderId are up-to-date,
            // especially if a new reminder was inserted (which gets a new ID).
            loadTodaySchedule(medicationId)
            Log.d("MedReminderVM", "Refreshed today's schedule after updating status for item: $itemId.")
        }
    }
}