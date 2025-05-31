package com.d4viddf.medicationreminder.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.TodayScheduleItem
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
        viewModelScope.launch {
            // Placeholder logic:
            // In a real scenario, this would involve:
            // 1. Fetching Medication by medicationId to get its name (if not passed directly).
            // 2. Fetching MedicationSchedules for this medicationId.
            // 3. Using a ReminderCalculator or similar logic to determine all specific reminder times for *today*.
            // 4. For each calculated time, checking against MedicationReminder entries in the DB
            //    to see if it's already taken or if an entry exists.
            // 5. Mapping all this data to TodayScheduleItem objects.

            // Example placeholder data (replace with actual logic):
            // Assuming medicationName is fetched or known. For placeholder, using "Medication Placeholder".
            val medicationName = "Medication $medicationId" // Placeholder
            _todayScheduleItems.value = listOf(
                TodayScheduleItem(
                    id = "${medicationId}_0900",
                    medicationName = medicationName,
                    time = LocalTime.of(9, 0),
                    isPast = LocalTime.now().isAfter(LocalTime.of(9, 0)),
                    isTaken = false, // This would come from DB
                    underlyingReminderId = 1L // Placeholder ID
                ),
                TodayScheduleItem(
                    id = "${medicationId}_1500",
                    medicationName = medicationName,
                    time = LocalTime.of(15, 0),
                    isPast = LocalTime.now().isAfter(LocalTime.of(15, 0)),
                    isTaken = true, // This would come from DB
                    underlyingReminderId = 2L // Placeholder ID
                ),
                TodayScheduleItem(
                    id = "${medicationId}_2100",
                    medicationName = medicationName,
                    time = LocalTime.of(21, 0),
                    isPast = LocalTime.now().isAfter(LocalTime.of(21, 0)),
                    isTaken = false, // This would come from DB
                    underlyingReminderId = 3L // Placeholder ID
                )
            ).sortedBy { it.time } // Sort by time
            Log.d("MedReminderVM", "Loaded placeholder TodayScheduleItems for medId: $medicationId")
        }
    }

    // Function to Add Past Medication Taken
    fun addPastMedicationTaken(medicationId: Int, medicationNameParam: String, date: LocalDate, time: LocalTime) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateTime = LocalDateTime.of(date, time)
            if (dateTime.isAfter(LocalDateTime.now())) {
                Log.w("MedReminderVM", "Cannot log future medication taken event for medId: $medicationId at $dateTime")
                // Handle error: cannot log future medication (e.g., show a toast or update an error StateFlow)
                return@launch
            }

            // Placeholder logic:
            // 1. Determine the appropriate MedicationSchedule ID if possible/necessary.
            //    This might involve finding a schedule that would have been active at that past date/time.
            //    Or, if it's a one-off dose, it might not link to a specific schedule instance.
            // 2. Create a MedicationReminder entity.
            // val newReminder = MedicationReminder(
            //     medicationId = medicationId,
            //     scheduleId = foundScheduleId, // Optional, or a marker for manual log
            //     reminderTime = dateTime.format(storableDateTimeFormatter), // Store as String, consistent with existing
            //     isTaken = true,
            //     takenAt = dateTime.format(storableDateTimeFormatter) // Log when it was actually taken
            // )
            // reminderRepository.insert(newReminder) // Or an appropriate save method

            Log.d("MedReminderVM", "Adding past medication taken for medId: $medicationId, name: $medicationNameParam, date: $date, time: $time")
            // Simulate DB operation
            // val createdReminderId = reminderRepository.insert(newReminder).await() // Assuming insert returns ID

            // If the logged date is today, refresh the today's schedule list
            if (date.isEqual(LocalDate.now())) {
                // To reflect this newly added past medication, we might need to adjust loadTodaySchedule
                // or specifically add this item to _todayScheduleItems.
                // For simplicity with placeholder, just reloading:
                loadTodaySchedule(medicationId)
                Log.d("MedReminderVM", "Refreshed today's schedule after adding past medication for today.")
            }
        }
    }

    // Function to Update Reminder Status (Toggle)
    fun updateReminderStatus(itemId: String, isTaken: Boolean, medicationId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = _todayScheduleItems.value.toMutableList()
            val itemIndex = currentList.indexOfFirst { it.id == itemId }

            if (itemIndex != -1) {
                val itemToUpdate = currentList[itemIndex]
                Log.d("MedReminderVM", "Updating status for item: $itemId, isTaken: $isTaken, medId: $medicationId")

                // Placeholder for DB update:
                // val reminderInDb = reminderRepository.getReminderById(itemToUpdate.underlyingReminderId)
                // if (reminderInDb != null) {
                //    val updatedReminder = reminderInDb.copy(
                //        isTaken = isTaken,
                //        takenAt = if (isTaken) LocalDateTime.now().format(storableDateTimeFormatter) else null
                //    )
                //    reminderRepository.update(updatedReminder)
                // } else {
                //    // If it was a conceptual reminder (not yet in DB), create it now.
                //    // This depends on how underlyingReminderId is handled for reminders not yet in DB.
                //    // For example, if underlyingReminderId is -1L for conceptual ones:
                //    if (itemToUpdate.underlyingReminderId == -1L) {
                //        val newReminder = MedicationReminder(
                //            medicationId = medicationId,
                //            // scheduleId = ... // needs to be determined
                //            reminderTime = LocalDateTime.of(LocalDate.now(), itemToUpdate.time).format(storableDateTimeFormatter),
                //            isTaken = isTaken,
                //            takenAt = if (isTaken) LocalDateTime.now().format(storableDateTimeFormatter) else null
                //        )
                //        reminderRepository.insert(newReminder)
                //    }
                //    Log.w("MedReminderVM", "Reminder with underlyingId ${itemToUpdate.underlyingReminderId} not found in DB for update.")
                // }


                // Update local list for immediate UI feedback
                currentList[itemIndex] = itemToUpdate.copy(isTaken = isTaken)
                _todayScheduleItems.value = currentList
                Log.d("MedReminderVM", "Updated local _todayScheduleItems for item: $itemId")
            } else {
                Log.w("MedReminderVM", "Item with id $itemId not found in _todayScheduleItems for update.")
            }
        }
    }
}