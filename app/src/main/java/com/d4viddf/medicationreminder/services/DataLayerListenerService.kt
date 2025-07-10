package com.d4viddf.medicationreminder.services

import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint

import android.util.Log
import com.d4viddf.medicationreminder.data.MedicationFullSyncItem // New Sync Model
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationScheduleDetailSyncItem // New Sync Model
import com.d4viddf.medicationreminder.data.ScheduleType // Enum for schedule type
import com.d4viddf.medicationreminder.data.TodayScheduleItem
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
import com.d4viddf.medicationreminder.repository.MedicationTypeRepository // For medication type details
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson // Already here from previous change, good.
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // Already here from previous change, good.
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DataLayerListenerService : WearableListenerService() {

    @Inject
    lateinit var medicationReminderRepository: MedicationReminderRepository
    @Inject
    lateinit var medicationRepository: MedicationRepository // Added
    @Inject
    lateinit var scheduleRepository: MedicationScheduleRepository // Added

    private val dataClient by lazy { Wearable.getDataClient(this) } // Added
    private val gson by lazy { Gson() } // Added


    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Message received: ${messageEvent.path} from node ${messageEvent.sourceNodeId}")

        when (messageEvent.path) {
            PATH_REQUEST_INITIAL_SYNC -> {
                Log.i(TAG, "Received initial sync request from watch.")
                serviceScope.launch {
                    triggerFullSyncToWear()
                }
            }
            PATH_MARK_AS_TAKEN -> {
                val reminderIdString = String(messageEvent.data, StandardCharsets.UTF_8)
                val reminderId = reminderIdString.toIntOrNull()
                if (reminderId != null) {
                    Log.d(TAG, "Received mark_as_taken for reminder ID: $reminderId")
                    serviceScope.launch {
                        try {
                            val currentTimeIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                            val success = medicationReminderRepository.markReminderAsTaken(reminderId, currentTimeIso)
                            if (success) {
                                Log.i(TAG, "Reminder $reminderId marked as taken successfully via watch.")
                                // Now fetch the medicationId for this reminder to update its schedule
                                val reminder = medicationReminderRepository.getReminderById(reminderId)
                                if (reminder != null) {
                                    val medicationId = reminder.medicationId
                                    // Fetch and sync the updated schedule for this medication
                                    val updatedScheduleItems = getTodayScheduleForMedication(medicationId)
                                    syncTodayScheduleToWear(updatedScheduleItems)
                                } else {
                                    Log.e(TAG, "Could not find reminder with ID $reminderId to get medicationId for sync.")
                                }
                            } else {
                                Log.e(TAG, "Failed to mark reminder $reminderId as taken via watch.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error marking reminder as taken or syncing: ${e.message}", e)
                        }
                    }
                } else {
                    Log.e(TAG, "Could not parse reminder ID from message data: $reminderIdString")
                }
            }
            // Add other message paths here if needed
            else -> {
                Log.w(TAG, "Unknown message path: ${messageEvent.path}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel coroutines when the service is destroyed
    }

    // Adapted from MedicationReminderViewModel
    private suspend fun getTodayScheduleForMedication(medicationId: Int): List<TodayScheduleItem> {
        val funcTag = "DataLayerListenerSvc.getTodaySchedule[MedId:$medicationId]"
        Log.d(TAG, "$funcTag: Starting to load today's schedule.")

        val medication = medicationRepository.getMedicationById(medicationId)
        if (medication == null) {
            Log.w(TAG, "$funcTag: Medication not found.")
            return emptyList()
        }

        if (!medication.endDate.isNullOrBlank()) {
            try {
                // Use the same date format as ReminderCalculator.dateStorableFormatter
                val endDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val endDateValue = LocalDate.parse(medication.endDate, endDateFormatter)
                if (endDateValue.isBefore(LocalDate.now())) {
                    Log.i(TAG, "$funcTag: Medication ${medication.name} (ID: $medicationId) has an endDate ($endDateValue) in the past. Returning empty schedule.")
                    return emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "$funcTag: Error parsing endDate '${medication.endDate}'. Proceeding.", e)
            }
        }

        val schedules = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()
        val today = LocalDate.now()
        val currentTime = LocalTime.now()
        val potentialSlotsToday = mutableListOf<TodayScheduleItem>()
        val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME // Added

        val allDbReminders = medicationReminderRepository.getRemindersForMedication(medicationId).firstOrNull() ?: emptyList()

        schedules?.forEach { schedule ->
            if (schedule.scheduleType == com.d4viddf.medicationreminder.data.ScheduleType.INTERVAL &&
                schedule.intervalStartTime.isNullOrBlank()) { // Type B (Continuous)
                val lastTakenDbReminder = allDbReminders
                    .filter { it.isTaken && it.takenAt != null && it.medicationId == medicationId }
                    .maxByOrNull { LocalDateTime.parse(it.takenAt, storableDateTimeFormatter) }
                val lastTakenDateTime = lastTakenDbReminder?.takenAt?.let {
                    try { LocalDateTime.parse(it, storableDateTimeFormatter) } catch (e: Exception) { null }
                }
                val remindersForTodayMap = ReminderCalculator.generateRemindersForPeriod(
                    medication = medication,
                    schedule = schedule,
                    periodStartDate = today,
                    periodEndDate = today,
                    lastTakenDateTime = lastTakenDateTime
                )
                val dailySlotsFromTypeB = remindersForTodayMap[today] ?: emptyList()
                dailySlotsFromTypeB.forEach { slotTime ->
                    potentialSlotsToday.add(
                        TodayScheduleItem(
                            id = "slot_typeB_${medication.id}_${schedule.id}_${slotTime.toNanoOfDay()}",
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
            } else { // Type A and others
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

        val relevantDbReminders = allDbReminders.filter { reminder ->
            val reminderScheduledDate = try { LocalDateTime.parse(reminder.reminderTime, storableDateTimeFormatter).toLocalDate() } catch (e: Exception) { null }
            val reminderTakenDate = reminder.takenAt?.let { try { LocalDateTime.parse(it, storableDateTimeFormatter).toLocalDate() } catch (e: Exception) { null } }
            reminderScheduledDate?.isEqual(today) == true || reminderTakenDate?.isEqual(today) == true
        }

        val finalTodayScheduleItems = mutableListOf<TodayScheduleItem>()
        val processedDbReminderIds = mutableSetOf<Int>()

        potentialSlotsToday.forEach { slot ->
            val matchingDbReminder = relevantDbReminders.find { dbReminder ->
                val dbReminderTime = try { LocalDateTime.parse(dbReminder.reminderTime, storableDateTimeFormatter) } catch (e: Exception) { null }
                dbReminder.medicationScheduleId == slot.medicationScheduleId &&
                        dbReminderTime?.toLocalDate()?.isEqual(today) == true &&
                        dbReminderTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES) == slot.time.truncatedTo(ChronoUnit.MINUTES)
            }
            if (matchingDbReminder != null) {
                finalTodayScheduleItems.add(
                    slot.copy(
                        isTaken = matchingDbReminder.isTaken,
                        takenAt = matchingDbReminder.takenAt,
                        underlyingReminderId = matchingDbReminder.id.toLong()
                    )
                )
                processedDbReminderIds.add(matchingDbReminder.id)
            } else {
                finalTodayScheduleItems.add(slot)
            }
        }

        relevantDbReminders.forEach { dbReminder ->
            if (dbReminder.isTaken && !processedDbReminderIds.contains(dbReminder.id)) {
                val takenAtDateTime = dbReminder.takenAt?.let { try { LocalDateTime.parse(it, storableDateTimeFormatter) } catch (e: Exception) { null } }
                if (takenAtDateTime != null && takenAtDateTime.toLocalDate().isEqual(today)) {
                    finalTodayScheduleItems.add(
                        TodayScheduleItem(
                            id = "adhoc_${dbReminder.id}",
                            medicationName = medication.name,
                            time = takenAtDateTime.toLocalTime(),
                            isPast = currentTime.isAfter(takenAtDateTime.toLocalTime()),
                            isTaken = true,
                            underlyingReminderId = dbReminder.id.toLong(),
                            medicationScheduleId = dbReminder.medicationScheduleId ?: 0,
                            takenAt = dbReminder.takenAt
                        )
                    )
                }
            }
        }

        val sortedList = finalTodayScheduleItems.sortedWith(
            compareBy<TodayScheduleItem> { it.time }
                .thenByDescending { it.underlyingReminderId != 0L }
                .thenByDescending { it.isTaken }
        )
        val distinctList = sortedList.distinctBy {
            if (it.isTaken && it.underlyingReminderId != 0L) "db_taken_${it.underlyingReminderId}"
            else if (it.isTaken && it.takenAt != null) "adhoc_taken_${it.takenAt}"
            else "slot_untaken_${it.medicationScheduleId}_${it.time.truncatedTo(ChronoUnit.MINUTES)}"
        }
        Log.i(TAG, "$funcTag: Final list size: ${distinctList.size}.")
        return distinctList
    }


    @Inject
    lateinit var medicationTypeRepository: MedicationTypeRepository // Added for type info


    // Generic method to send data to wear
    private fun sendDataToWear(path: String, jsonData: String, itemTypeForLog: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val putDataMapReq = PutDataMapRequest.create(path)
                putDataMapReq.dataMap.putString("sync_data_json", jsonData) // Generic key
                putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                dataClient.putDataItem(putDataReq).await()
                Log.i(TAG, "Successfully synced $itemTypeForLog to Wear OS via $path. JSON size: ${jsonData.length}")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing $itemTypeForLog to Wear OS via $path", e)
            }
        }
    }

    companion object {
        private const val TAG = "DataLayerListenerSvc"
        private const val PATH_MARK_AS_TAKEN = "/mark_as_taken"
        private const val PATH_REQUEST_INITIAL_SYNC = "/request_initial_sync"
        private const val PATH_FULL_MED_DATA_SYNC = "/full_medication_data_sync" // New path for full data
        // Old path for today's schedule, can be deprecated or kept for specific updates
        private const val PATH_TODAY_SCHEDULE_SYNC = "/today_schedule"
    }

    // Modified method to trigger a full sync with comprehensive data
    private suspend fun triggerFullSyncToWear() {
        Log.i(TAG, "Starting full medication data sync to Wear OS.")
        val allDbMedications = medicationRepository.getAllMedications().firstOrNull() ?: emptyList()

        if (allDbMedications.isEmpty()) {
            Log.i(TAG, "No medications found to sync.")
            sendDataToWear(PATH_FULL_MED_DATA_SYNC, gson.toJson(emptyList<MedicationFullSyncItem>()), "full medication data (empty)")
            return
        }

        val medicationFullSyncItems = mutableListOf<MedicationFullSyncItem>()

        for (medication in allDbMedications) {
            // Filter out medications that have an end date in the past
            if (!medication.endDate.isNullOrBlank()) {
                try {
                    val endDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy") // Assuming this format from your codebase
                    val endDateValue = LocalDate.parse(medication.endDate, endDateFormatter)
                    if (endDateValue.isBefore(LocalDate.now())) {
                        Log.i(TAG, "Skipping medication ${medication.name} (ID: ${medication.id}) for sync as its endDate ($endDateValue) is in the past.")
                        continue
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse endDate '${medication.endDate}' for medication ${medication.id}. Proceeding with sync.", e)
                }
            }

            val schedulesForMed = scheduleRepository.getSchedulesForMedication(medication.id).firstOrNull() ?: emptyList()
            val scheduleDetailSyncItems = schedulesForMed.mapNotNull { schedule ->
                // Basic validation: if schedule type is specific times but no times are set, it might be invalid
                if (schedule.scheduleType == ScheduleType.DAILY_SPECIFIC_TIMES && schedule.specificTimes.isNullOrEmpty()) {
                    Log.w(TAG, "Medication ${medication.id}, Schedule ${schedule.id} is DAILY_SPECIFIC_TIMES but has no specific times. Skipping this schedule.")
                    null // Skip this invalid schedule
                } else {
                    MedicationScheduleDetailSyncItem(
                        scheduleId = schedule.id,
                        scheduleType = schedule.scheduleType.name, // Convert enum to string
                        specificTimes = schedule.specificTimes?.map { LocalTime.parse(it).format(DateTimeFormatter.ofPattern("HH:mm")) },
                        intervalHours = schedule.intervalHours,
                        intervalMinutes = schedule.intervalMinutes,
                        intervalStartTime = schedule.intervalStartTime?.let { LocalTime.parse(it).format(DateTimeFormatter.ofPattern("HH:mm")) },
                        intervalEndTime = schedule.intervalEndTime?.let { LocalTime.parse(it).format(DateTimeFormatter.ofPattern("HH:mm")) },
                        dailyRepetitionDays = schedule.daysOfWeek // Assuming daysOfWeek is already List<String> like ["MONDAY", "TUESDAY"]
                    )
                }
            }

            if (schedulesForMed.isNotEmpty() && scheduleDetailSyncItems.isEmpty() && schedulesForMed.any { it.scheduleType == ScheduleType.DAILY_SPECIFIC_TIMES && it.specificTimes.isNullOrEmpty()}) {
                // This case means all schedules for this medication were skipped due to being invalid (e.g. specific times type with no times)
                Log.w(TAG, "Medication ${medication.name} (ID: ${medication.id}) has schedules, but all were deemed invalid for sync (e.g., specific times type with no times). Skipping this medication from sync.")
                continue // Skip this medication if all its schedules are invalid
            }


            var medicationTypeName: String? = null
            var medicationIconUrl: String? = null
            if (medication.typeId != null) {
                val medType = medicationTypeRepository.getMedicationTypeById(medication.typeId)
                medicationTypeName = medType?.name
                medicationIconUrl = medType?.imageUrl
            }

            medicationFullSyncItems.add(
                MedicationFullSyncItem(
                    medicationId = medication.id,
                    name = medication.name,
                    dosage = medication.dosage,
                    color = medication.color,
                    typeName = medicationTypeName,
                    typeIconUrl = medicationIconUrl,
                    schedules = scheduleDetailSyncItems,
                    startDate = medication.startDate, // Assuming format "dd/MM/yyyy"
                    endDate = medication.endDate // Assuming format "dd/MM/yyyy"
                )
            )
        }

        if (medicationFullSyncItems.isEmpty() && allDbMedications.isNotEmpty()) {
             Log.w(TAG, "All medications were filtered out (e.g. past end date or invalid schedules). Sending empty list.")
        }

        val jsonToSend = gson.toJson(medicationFullSyncItems)
        sendDataToWear(PATH_FULL_MED_DATA_SYNC, jsonToSend, "full medication data")
        Log.i(TAG, "Full medication data sync triggered. Sent ${medicationFullSyncItems.size} medication items.")

        // For backwards compatibility or specific today's view, you might still want to send the old format:
        // val todayScheduleItems = medicationFullSyncItems.flatMap { medSyncItem -> ... convert to TodayScheduleItem ... }
        // sendDataToWear(PATH_TODAY_SCHEDULE_SYNC, gson.toJson(todayScheduleItems), "today's schedule")
    }
}
