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
        val funcTag = "loadTodaySchedule[MedId:$medicationId]"
        Log.d("MedReminderVM", "$funcTag: Starting to load today's schedule.")

        viewModelScope.launch(Dispatchers.IO) {
            val medication = medicationRepository.getMedicationById(medicationId)
            if (medication == null) {
                Log.w("MedReminderVM", "$funcTag: Medication not found.")
                _todayScheduleItems.value = emptyList()
                return@launch
            }
            Log.d("MedReminderVM", "$funcTag: Medication found: ${medication.name}")

            val schedules = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()
            if (schedules.isNullOrEmpty()) {
                Log.i("MedReminderVM", "$funcTag: No schedules found for medication.")
                // Even if no schedules, there might be ad-hoc taken doses, so proceed but calculatedItemsMap will be empty.
            } else {
                Log.d("MedReminderVM", "$funcTag: Found ${schedules.size} schedule(s).")
            }

            val today = LocalDate.now()
            val currentTime = LocalTime.now() // For isPast calculation
            val calculatedItemsMap = mutableMapOf<LocalTime, TodayScheduleItem>()
            val adHocItems = mutableListOf<TodayScheduleItem>()

            // Determine lastTakenOnTodayTime for ReminderCalculator
            var lastTakenOnTodayTime: LocalTime? = null
            val mostRecentTakenDbReminder = reminderRepository.getMostRecentTakenReminder(medicationId)
            if (mostRecentTakenDbReminder?.takenAt != null) {
                try {
                    val takenAtDateTime = LocalDateTime.parse(mostRecentTakenDbReminder.takenAt, storableDateTimeFormatter)
                    if (takenAtDateTime.toLocalDate().isEqual(today)) {
                        lastTakenOnTodayTime = takenAtDateTime.toLocalTime()
                        Log.i("MedReminderVM", "$funcTag: Last taken dose on today was at $lastTakenOnTodayTime (from Reminder ID: ${mostRecentTakenDbReminder.id})")
                    }
                } catch (e: Exception) {
                    Log.e("MedReminderVM", "$funcTag: Error parsing takenAt for mostRecentTakenDbReminder (ID: ${mostRecentTakenDbReminder.id}, TakenAt: ${mostRecentTakenDbReminder.takenAt})", e)
                }
            }

            // 1. Create TodayScheduleItem objects from all calculated times for today
            schedules?.forEach { schedule ->
                Log.d("MedReminderVM", "$funcTag: Calculating reminders for Schedule ID: ${schedule.id}, Type: ${schedule.scheduleType}")
                val calculatedDateTimesToday = ReminderCalculator.calculateReminderDateTimes(medication, schedule, today, lastTakenOnTodayTime)
                Log.d("MedReminderVM", "$funcTag: Schedule ID ${schedule.id} produced ${calculatedDateTimesToday.size} calculated times for today (using lastTakenOnTodayTime: $lastTakenOnTodayTime).")

                calculatedDateTimesToday.forEach { reminderDateTime ->
                    val reminderTime = reminderDateTime.toLocalTime()
                    if (!calculatedItemsMap.containsKey(reminderTime)) { // Avoid overwriting if multiple schedules have same time
                        val scheduleItem = TodayScheduleItem(
                            id = "${medicationId}_${schedule.id}_${reminderTime.toNanoOfDay()}", // Unique enough for map keying
                            medicationName = medication.name,
                            time = reminderTime,
                            isPast = currentTime.isAfter(reminderTime),
                            isTaken = false, // Initially false
                            underlyingReminderId = 0L, // Default, will be updated if DB record found
                            medicationScheduleId = schedule.id, // Link to the schedule that generated it
                            takenAt = null
                        )
                        calculatedItemsMap[reminderTime] = scheduleItem
                        Log.d("MedReminderVM", "$funcTag: Added calculated item for time $reminderTime from SchedID ${schedule.id}")
                    } else {
                        Log.d("MedReminderVM", "$funcTag: Calculated time $reminderTime from SchedID ${schedule.id} already exists in map (from another schedule). Skipping.")
                    }
                }
            }
            Log.i("MedReminderVM", "$funcTag: Finished processing calculated items. Map size: ${calculatedItemsMap.size}")

            // 2. Fetch all actual MedicationReminder DB records for the medication (filter for today in Kotlin)
            val allDbReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
            Log.d("MedReminderVM", "$funcTag: Fetched ${allDbReminders.size} total DB reminders for MedId $medicationId.")

            val todayDbReminders = allDbReminders.filter { reminder ->
                val reminderDate = try { LocalDateTime.parse(reminder.reminderTime, storableDateTimeFormatter).toLocalDate() } catch (e: Exception) { null }
                val takenDate = reminder.takenAt?.let { try { LocalDateTime.parse(it, storableDateTimeFormatter).toLocalDate() } catch (e: Exception) { null } }
                reminderDate?.isEqual(today) == true || takenDate?.isEqual(today) == true
            }
            Log.i("MedReminderVM", "$funcTag: Filtered to ${todayDbReminders.size} DB reminders relevant for today.")

            // 3. Iterate these DB records and update the map or add to ad-hoc list
            todayDbReminders.forEach { dbReminder ->
                val reminderTimeFromDb = try { LocalDateTime.parse(dbReminder.reminderTime, storableDateTimeFormatter).toLocalTime() } catch (e: Exception) { null }
                val takenAtTimeFromDb = dbReminder.takenAt?.let { try { LocalDateTime.parse(it, storableDateTimeFormatter).toLocalTime() } catch (e: Exception) { null } }

                // Determine the primary time for this DB entry (taken time if available and taken, otherwise scheduled time)
                val displayTime: LocalTime? = if (dbReminder.isTaken && takenAtTimeFromDb != null) {
                    takenAtTimeFromDb
                } else {
                    reminderTimeFromDb
                }
                Log.d("MedReminderVM", "$funcTag: Processing DB Reminder ID ${dbReminder.id}. IsTaken: ${dbReminder.isTaken}, SchedTime: $reminderTimeFromDb, TakenAtTime: $takenAtTimeFromDb. Effective displayTime: $displayTime")


                if (displayTime != null) {
                    if (calculatedItemsMap.containsKey(displayTime)) {
                        val existingItem = calculatedItemsMap[displayTime]!!
                        Log.d("MedReminderVM", "$funcTag: DB Reminder ID ${dbReminder.id} matches calculated slot at $displayTime. Updating item.")
                        calculatedItemsMap[displayTime] = existingItem.copy(
                            isTaken = dbReminder.isTaken,
                            underlyingReminderId = dbReminder.id.toLong(),
                            // medicationScheduleId could be different if manually logged against a different schedule, prefer original from calculation
                            takenAt = if (dbReminder.isTaken) dbReminder.takenAt else null, // Store the actual takenAt string
                            isPast = currentTime.isAfter(displayTime) // Re-evaluate isPast based on displayTime
                        )
                    } else {
                        // This is an ad-hoc reminder (e.g., manually added past dose not matching any calculated slot for today)
                        Log.d("MedReminderVM", "$funcTag: DB Reminder ID ${dbReminder.id} at $displayTime is ad-hoc (not in calculated map).")
                        adHocItems.add(
                            TodayScheduleItem(
                                id = "adhoc_${medicationId}_${dbReminder.id}",
                                medicationName = medication.name,
                                time = displayTime,
                                isPast = currentTime.isAfter(displayTime),
                                isTaken = dbReminder.isTaken,
                                underlyingReminderId = dbReminder.id.toLong(),
                                medicationScheduleId = dbReminder.medicationScheduleId ?: 0, // Use actual if available, else 0/null
                                takenAt = dbReminder.takenAt
                            )
                        )
                    }
                } else {
                     Log.w("MedReminderVM", "$funcTag: DB Reminder ID ${dbReminder.id} could not determine a valid displayTime. ReminderTime: ${dbReminder.reminderTime}, TakenAt: ${dbReminder.takenAt}. Skipping.")
                }
            }
            Log.i("MedReminderVM", "$funcTag: Finished processing DB reminders. Ad-hoc items count: ${adHocItems.size}")

            // 4. Combine, sort, and update StateFlow
            val combinedList = (calculatedItemsMap.values + adHocItems).distinctBy {
                // Create a composite key for distinctness, considering time and taken status primarily for UI representation
                // If an item was calculated and then matched with a taken DB record, it's one item.
                // Ad-hoc items are distinct by their original reminder ID.
                if (it.id.startsWith("adhoc_")) it.id else "${it.time}_${it.isTaken}"
            }.sortedBy { it.time }

            Log.i("MedReminderVM", "$funcTag: Combined list size before final sort/distinct: ${calculatedItemsMap.values.size + adHocItems.size}, after distinctBy and sort: ${combinedList.size}.")
            _todayScheduleItems.value = combinedList
            Log.d("MedReminderVM", "$funcTag: Successfully updated _todayScheduleItems. Final items: ${combinedList.joinToString { it.time.toString() + " (T:" + it.isTaken + ")" }}")
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

            val funcTag = "updateReminderStatus[MedId:$medicationId, ItemId:$itemId, IsTaken:$isTaken]"
            Log.d("MedReminderVM", "$funcTag: Starting update.")

            var reminderInDb: MedicationReminder? = null

            if (todayItem.underlyingReminderId != 0L) {
                Log.d("MedReminderVM", "$funcTag: Attempting to find reminder by underlyingReminderId: ${todayItem.underlyingReminderId}.")
                reminderInDb = reminderRepository.getReminderById(todayItem.underlyingReminderId.toInt())
                if (reminderInDb == null) {
                    Log.w("MedReminderVM", "$funcTag: Failed to find reminder by underlyingReminderId: ${todayItem.underlyingReminderId} even though it was non-zero. This might indicate a stale UI item.")
                    // It's possible the item was deleted elsewhere. loadTodaySchedule will refresh UI.
                    // For now, we might not be able to proceed with this specific update if the DB record is gone.
                    // However, if we are marking as TAKEN, we might still want to create a new record if the old one vanished.
                } else {
                    Log.d("MedReminderVM", "$funcTag: Found reminder by ID: ${reminderInDb.id}.")
                }
            }

            if (isTaken) {
                // Determine the correct date and time to record
                val timeFromListItem = todayItem.time // This is LocalTime
                val dateToRecord = LocalDate.now()    // Use current date for "taken now" action
                val dateTimeToRecord = LocalDateTime.of(dateToRecord, timeFromListItem)
                val dateTimeToRecordStr = dateTimeToRecord.format(storableDateTimeFormatter)
                Log.d("MedReminderVM", "$funcTag: Determined dateTimeToRecordStr: $dateTimeToRecordStr for marking as taken.")

                if (reminderInDb != null) { // Found by ID
                    Log.d("MedReminderVM", "$funcTag: Marking reminder (ID: ${reminderInDb.id}) as TAKEN. Original reminderTime: ${reminderInDb.reminderTime}, New takenAt: $dateTimeToRecordStr")
                    val updatedReminder = reminderInDb.copy(
                        isTaken = true,
                        takenAt = dateTimeToRecordStr // Use determined dateTimeToRecordStr
                    )
                    reminderRepository.updateReminder(updatedReminder)
                    Log.d("MedReminderVM", "$funcTag: Reminder (ID: ${reminderInDb.id}) successfully marked as taken.")
                } else { // Not found by ID (or ID was 0L, or it vanished)
                    Log.d("MedReminderVM", "$funcTag: Reminder not found by ID (underlyingId: ${todayItem.underlyingReminderId}). Attempting time-based lookup or creating new.")

                    // Time-based search should use the original scheduled time from todayItem to find an UNTAKEN slot
                    val scheduledTimeForSearch = LocalDateTime.of(dateToRecord, todayItem.time).format(storableDateTimeFormatter)

                    val existingRemindersForTime = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
                    // Try to find an existing UNTAKEN reminder at the item's time slot for today
                    val reminderByTime = existingRemindersForTime.find {
                        val rt = try { LocalDateTime.parse(it.reminderTime, storableDateTimeFormatter) } catch (e:Exception) { null }
                        rt?.toLocalDate()?.isEqual(dateToRecord) == true && rt?.toLocalTime()?.equals(todayItem.time) == true && !it.isTaken && it.medicationId == medicationId
                    }

                    if (reminderByTime != null) {
                        Log.d("MedReminderVM", "$funcTag: Found UNTAKEN reminder by time (ID: ${reminderByTime.id}, Time: ${reminderByTime.reminderTime}). Marking as TAKEN with takenAt: $dateTimeToRecordStr.")
                        val updatedReminder = reminderByTime.copy(
                            isTaken = true,
                            takenAt = dateTimeToRecordStr // Use determined dateTimeToRecordStr
                        )
                        reminderRepository.updateReminder(updatedReminder)
                        Log.d("MedReminderVM", "$funcTag: Reminder (ID: ${reminderByTime.id}) successfully marked as taken via time-based search.")
                    } else {
                        Log.d("MedReminderVM", "$funcTag: No existing UNTAKEN reminder found by ID or time. Creating NEW reminder as TAKEN. ReminderTime & TakenAt: $dateTimeToRecordStr")
                        val newReminder = MedicationReminder(
                            id = 0, // Auto-generated
                            medicationId = medicationId,
                            // Ensure medicationScheduleId is null if todayItem.medicationScheduleId is 0 (placeholder for no schedule)
                            medicationScheduleId = todayItem.medicationScheduleId.let { if (it == 0) null else it },
                            reminderTime = dateTimeToRecordStr, // NEW reminder's scheduled time is the taken time
                            isTaken = true,
                            takenAt = dateTimeToRecordStr,      // Also recorded as takenAt
                            notificationId = null // Manage if needed
                        )
                        reminderRepository.insertReminder(newReminder)
                        Log.d("MedReminderVM", "$funcTag: Successfully created and marked NEW reminder as taken.")
                    }
                }

                // After successfully marking as taken (either update or insert), check for interval schedule
                val medication = medicationRepository.getMedicationById(medicationId) // KEEP this one
                val schedule = medication?.id?.let { scheduleRepository.getSchedulesForMedication(it).firstOrNull()?.firstOrNull() } // KEEP this one

                // REMOVE THE DUPLICATES that were here

                if (schedule?.scheduleType == com.d4viddf.medicationreminder.data.ScheduleType.INTERVAL) {
                    triggerNextReminderScheduling(medicationId)
                    Log.i("MedReminderVM", "$funcTag: Triggered ReminderSchedulingWorker for interval schedule.")
                } else {
                    Log.d("MedReminderVM", "$funcTag: Did not trigger ReminderSchedulingWorker. Schedule type is not INTERVAL (${schedule?.scheduleType}) or schedule/medication not found.")
                }

            } else { // Unmarking as taken (isTaken = false)
                if (reminderInDb != null) { // Found by ID, this is the preferred path
                    Log.d("MedReminderVM", "$funcTag: Marking reminder (ID: ${reminderInDb.id}) as NOT taken.")
                    val updatedReminder = reminderInDb.copy(
                        isTaken = false,
                        takenAt = null
                        // Consider if notificationId should be reset or managed here if alarms need rescheduling for untaken doses.
                        // For now, just clearing taken status.
                    )
                    reminderRepository.updateReminder(updatedReminder)
                    Log.d("MedReminderVM", "$funcTag: Reminder (ID: ${reminderInDb.id}) successfully marked as NOT taken.")
                } else {
                    // If underlyingReminderId was 0 or lookup by ID failed.
                    // It's less common to "untake" something that wasn't concretely in the DB via ID.
                    // A time-based fallback here might be risky if it affects a different underlying reminder
                    // that happens to share a time slot but wasn't the one represented by todayItem.
                    // If todayItem.underlyingReminderId was 0L, it means it was purely conceptual or its DB record was lost.
                    // If it was non-zero but not found, it's an inconsistency.
                    if (todayItem.underlyingReminderId != 0L) {
                        Log.e("MedReminderVM", "$funcTag: Attempted to unmark item with underlyingReminderId ${todayItem.underlyingReminderId}, but it was not found in DB. No action taken for unmarking.")
                    } else {
                        Log.w("MedReminderVM", "$funcTag: Attempted to unmark item ${todayItem.id} which had no (or zero) underlyingReminderId. No action taken in DB.")
                    }
                    // No actual DB update occurs in this specific 'else' branch for unmarking, as we couldn't find the target by ID.
                }
                // Note: Unlike marking as taken, we generally don't trigger rescheduling for un-taking an interval med,
                // as the "last taken" is still the previous one. If this behavior needs to change, logic could be added here.
            }

            // Refresh the list to show the new status.
            // especially if a new reminder was inserted (which gets a new ID).
            loadTodaySchedule(medicationId)
            Log.d("MedReminderVM", "Refreshed today's schedule after updating status for item: $itemId.")
        }
    }
}