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
import com.d4viddf.medicationreminder.workers.WorkerScheduler
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
import java.time.temporal.ChronoUnit // Added for truncatedTo
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
            WorkerScheduler.scheduleRemindersForMedication(appContext, medicationId)
            Log.i("MedReminderVM", "Scheduled medication-specific reminder scheduling for medId $medicationId after marking reminder as taken and updating lists.")
        }
    }

    // internal fun triggerNextReminderScheduling(medicationId: Int) {
    //     Log.d("MedReminderVM", "Triggering next reminder scheduling for med ID: $medicationId using injected appContext")
    //     val workManager = WorkManager.getInstance(this.appContext) // Usa this.appContext
    //     val data = Data.Builder()
    //         .putInt(ReminderSchedulingWorker.KEY_MEDICATION_ID, medicationId)
    //         .putBoolean(ReminderSchedulingWorker.KEY_IS_DAILY_REFRESH, false)
    //         .build()
    //     val scheduleNextWorkRequest =
    //         OneTimeWorkRequestBuilder<ReminderSchedulingWorker>()
    //             .setInputData(data)
    //             .addTag("${ReminderSchedulingWorker.WORK_NAME_PREFIX}NextFromDetail_${medicationId}")
    //             .build()
    //     workManager.enqueueUniqueWork(
    //         "${ReminderSchedulingWorker.WORK_NAME_PREFIX}NextScheduledFromDetail_${medicationId}",
    //         ExistingWorkPolicy.REPLACE,
    //         scheduleNextWorkRequest
    //     )
    //     Log.i("MedReminderVM", "Enqueued ReminderSchedulingWorker for med ID $medicationId.")
    // }

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

            // New: Check if medication endDate is in the past
            if (!medication.endDate.isNullOrBlank()) {
                try {
                    val endDateValue = LocalDate.parse(medication.endDate, ReminderCalculator.dateStorableFormatter) // "dd/MM/yyyy"
                    if (endDateValue.isBefore(LocalDate.now())) {
                        Log.i("MedReminderVM", "$funcTag: Medication ${medication.name} (ID: $medicationId) has an endDate ($endDateValue) in the past. Clearing today's schedule items.")
                        _todayScheduleItems.value = emptyList()
                        return@launch // Stop further processing for today's schedule
                    }
                } catch (e: Exception) {
                    Log.e("MedReminderVM", "$funcTag: Error parsing endDate '${medication.endDate}' for medication ID: $medicationId. Proceeding with schedule loading.", e)
                    // If endDate is malformed, proceed as if there's no valid end date for now
                }
            }

            val schedules = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()
            if (schedules.isNullOrEmpty()) {
                Log.i("MedReminderVM", "$funcTag: No schedules found for medication.")
                // Even if no schedules, there might be ad-hoc taken doses, so proceed but calculatedItemsMap will be empty.
            } else {
                Log.d("MedReminderVM", "$funcTag: Found ${schedules.size} schedule(s).")
            }

            val today = LocalDate.now()
            val currentTime = LocalTime.now()
            val potentialSlotsToday = mutableListOf<TodayScheduleItem>()

            // 1. Get all potential slots for today from schedules
            // Fetch all DB reminders for the medication once, as it's needed for Type B logic and later merging.
            val allDbReminders = reminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()
            Log.d("MedReminderVM", "$funcTag: Fetched ${allDbReminders.size} total DB reminders for medicationId: $medicationId.")

            schedules?.forEach { schedule ->
                if (schedule.scheduleType == com.d4viddf.medicationreminder.data.ScheduleType.INTERVAL &&
                    schedule.intervalStartTime.isNullOrBlank()) {
                    // This is a Type B (Continuous) Interval
                    Log.d("MedReminderVM", "$funcTag: Handling Type B Interval. Schedule ID: ${schedule.id}")

                    val lastTakenDbReminder = allDbReminders
                        .filter { it.isTaken && it.takenAt != null && it.medicationId == medicationId } // Ensure it's for the current medication
                        .maxByOrNull { LocalDateTime.parse(it.takenAt, storableDateTimeFormatter) }

                    val lastTakenDateTime = lastTakenDbReminder?.takenAt?.let {
                        try { LocalDateTime.parse(it, storableDateTimeFormatter) } catch (e: Exception) { null }
                    }
                    Log.d("MedReminderVM", "$funcTag: For Type B Interval, lastTakenDateTime: $lastTakenDateTime (from DB reminder ID: ${lastTakenDbReminder?.id})")

                    val remindersForTodayMap = ReminderCalculator.generateRemindersForPeriod(
                        medication = medication,
                        schedule = schedule,
                        periodStartDate = today,
                        periodEndDate = today,
                        lastTakenDateTime = lastTakenDateTime
                    )

                    val dailySlotsFromTypeB = remindersForTodayMap[today] ?: emptyList()
                    Log.d("MedReminderVM", "$funcTag: Type B Interval generated ${dailySlotsFromTypeB.size} slots for today: $dailySlotsFromTypeB")

                    dailySlotsFromTypeB.forEach { slotTime ->
                        potentialSlotsToday.add(
                            TodayScheduleItem(
                                id = "slot_typeB_${medication.id}_${schedule.id}_${slotTime.toNanoOfDay()}", // Ensure unique ID
                                medicationName = medication.name,
                                time = slotTime,
                                isPast = currentTime.isAfter(slotTime),
                                isTaken = false, // Default, will be merged later
                                underlyingReminderId = 0L,
                                medicationScheduleId = schedule.id,
                                takenAt = null
                            )
                        )
                    }
                } else {
                    // Existing logic for Type A intervals and other schedule types
                    Log.d("MedReminderVM", "$funcTag: Handling non-Type B Interval or other schedule type. Schedule ID: ${schedule.id}, Type: ${schedule.scheduleType}")
                    val dailySlots = ReminderCalculator.getAllPotentialSlotsForDay(schedule, today)
                    dailySlots.forEach { slotTime ->
                        potentialSlotsToday.add(
                            TodayScheduleItem(
                                id = "slot_${medication.id}_${schedule.id}_${slotTime.toNanoOfDay()}",
                                medicationName = medication.name,
                                time = slotTime,
                                isPast = currentTime.isAfter(slotTime),
                                isTaken = false,
                                underlyingReminderId = 0L,
                                medicationScheduleId = schedule.id,
                                takenAt = null
                            )
                        )
                    }
                }
            }
            Log.i("MedReminderVM", "$funcTag: Generated ${potentialSlotsToday.size} potential slots for today.")

            // 2. Fetch relevant DB reminders (scheduled for today OR taken today)
            // allDbReminders is already fetched above.
            val relevantDbReminders = allDbReminders.filter { reminder ->
                val reminderScheduledDate = try { LocalDateTime.parse(reminder.reminderTime, storableDateTimeFormatter).toLocalDate() } catch (e: Exception) { null }
                val reminderTakenDate = reminder.takenAt?.let { try { LocalDateTime.parse(it, storableDateTimeFormatter).toLocalDate() } catch (e: Exception) { null } }
                reminderScheduledDate?.isEqual(today) == true || reminderTakenDate?.isEqual(today) == true
            }
            Log.i("MedReminderVM", "$funcTag: Fetched ${relevantDbReminders.size} relevant DB reminders for today.")

            val finalTodayScheduleItems = mutableListOf<TodayScheduleItem>()
            val processedDbReminderIds = mutableSetOf<Int>()

            // 3. Merge DB state with potential slots
            potentialSlotsToday.forEach { slot ->
                val slotDateTime = LocalDateTime.of(today, slot.time)
                // Find a DB reminder that matches this scheduleId and time (within a minute to be safe with parsing/storage)
                val matchingDbReminder = relevantDbReminders.find { dbReminder ->
                    val dbReminderTime = try { LocalDateTime.parse(dbReminder.reminderTime, storableDateTimeFormatter) } catch (e: Exception) { null }
                    dbReminder.medicationScheduleId == slot.medicationScheduleId &&
                            dbReminderTime?.toLocalDate()?.isEqual(today) == true &&
                            dbReminderTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES) == slot.time.truncatedTo(ChronoUnit.MINUTES)
                }

                if (matchingDbReminder != null) {
                    Log.d("MedReminderVM", "$funcTag: Matched slot ${slot.time} with DB Reminder ID ${matchingDbReminder.id}")
                    finalTodayScheduleItems.add(
                        slot.copy(
                            isTaken = matchingDbReminder.isTaken,
                            takenAt = matchingDbReminder.takenAt,
                            underlyingReminderId = matchingDbReminder.id.toLong(),
                            // isPast is relative to currentTime and slot.time, already set
                        )
                    )
                    processedDbReminderIds.add(matchingDbReminder.id)
                } else {
                    finalTodayScheduleItems.add(slot) // Add the untaken potential slot
                }
            }

            // 4. Add ad-hoc taken doses (DB reminders taken today but not matching any potential slot)
            relevantDbReminders.forEach { dbReminder ->
                if (dbReminder.isTaken && !processedDbReminderIds.contains(dbReminder.id)) {
                    val takenAtDateTime = dbReminder.takenAt?.let { try { LocalDateTime.parse(it, storableDateTimeFormatter) } catch (e: Exception) { null } }
                    if (takenAtDateTime != null && takenAtDateTime.toLocalDate().isEqual(today)) {
                         Log.d("MedReminderVM", "$funcTag: Adding ad-hoc taken DB Reminder ID ${dbReminder.id}")
                        finalTodayScheduleItems.add(
                            TodayScheduleItem(
                                id = "adhoc_${dbReminder.id}",
                                medicationName = medication.name,
                                time = takenAtDateTime.toLocalTime(),
                                isPast = currentTime.isAfter(takenAtDateTime.toLocalTime()),
                                isTaken = true,
                                underlyingReminderId = dbReminder.id.toLong(),
                                medicationScheduleId = dbReminder.medicationScheduleId ?: 0, // Use 0 or a specific marker
                                takenAt = dbReminder.takenAt
                            )
                        )
                    }
                }
            }
            Log.i("MedReminderVM", "$funcTag: Added ad-hoc items. Total items before sort/distinct: ${finalTodayScheduleItems.size}")


            // 5. Sort and make distinct, then update StateFlow
            // Sort by time, then by whether it was a DB entry (preferring DB entries if times are identical)
            // and then ensure taken items come before untaken ones if times are identical.
            val sortedList = finalTodayScheduleItems.sortedWith(
                compareBy<TodayScheduleItem> { it.time }
                    .thenByDescending { it.underlyingReminderId != 0L } // Prefer items linked to DB if times are same
                    .thenByDescending { it.isTaken } // If still same, taken ones first
            )

            // Distinct strategy: if multiple items end up representing the "same" logical reminder slot
            // (e.g., a calculated one and a DB one that slightly differs in nanos but matches on minute),
            // prefer the one that has an underlyingReminderId (from DB) or isTaken.
            // A simpler distinct might be by time truncated to minute for untaken, and by ID/takenAt for taken.
            val distinctList = sortedList.distinctBy {
                if (it.isTaken && it.underlyingReminderId != 0L) "db_taken_${it.underlyingReminderId}"
                else if (it.isTaken && it.takenAt != null) "adhoc_taken_${it.takenAt}"
                else "slot_untaken_${it.medicationScheduleId}_${it.time.truncatedTo(ChronoUnit.MINUTES)}"
            }


            Log.i("MedReminderVM", "$funcTag: Final list size after sort and distinctBy: ${distinctList.size}.")
            _todayScheduleItems.value = distinctList
            Log.d("MedReminderVM", "$funcTag: Successfully updated _todayScheduleItems. Final items: ${distinctList.joinToString { it.time.toString() + " (T:" + it.isTaken + ", ID:"+ it.id +", DBID:"+it.underlyingReminderId+")" }}")
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
                WorkerScheduler.scheduleRemindersForMedication(appContext, medicationId)
                Log.i("MedReminderVM", "Scheduled medication-specific reminder scheduling for medId: $medicationId after adding past taken dose.")

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

                // After successfully marking as taken (either update or insert)
                // Always trigger rescheduling to ensure consistency and recalculation for all types.
                WorkerScheduler.scheduleRemindersForMedication(appContext, medicationId)
                Log.i("MedReminderVM", "$funcTag: Scheduled medication-specific reminder scheduling for medication ID $medicationId after marking as taken. Schedule type was ${scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()?.firstOrNull()?.scheduleType}.")

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