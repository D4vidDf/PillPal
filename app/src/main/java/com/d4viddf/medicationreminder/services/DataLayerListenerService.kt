package com.d4viddf.medicationreminder.services

import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import com.d4viddf.medicationreminder.data.MedicationFullSyncItem
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationScheduleDetailSyncItem
import com.d4viddf.medicationreminder.data.ScheduleType // Enum for schedule type
import com.d4viddf.medicationreminder.data.TodayScheduleItem
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
import com.d4viddf.medicationreminder.repository.MedicationTypeRepository
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    lateinit var medicationRepository: MedicationRepository
    @Inject
    lateinit var scheduleRepository: MedicationScheduleRepository
    @Inject
    lateinit var medicationTypeRepository: MedicationTypeRepository

    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val gson by lazy { Gson() }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // For opening Play Store on phone
    private lateinit var wearConnectivityHelper: WearConnectivityHelper // Initialize in onCreate

    override fun onCreate() {
        super.onCreate()
        wearConnectivityHelper = WearConnectivityHelper(applicationContext)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Message received: ${messageEvent.path} from node ${messageEvent.sourceNodeId}")

        when (messageEvent.path) {
            PATH_OPEN_PLAY_STORE_ON_PHONE -> {
                Log.i(TAG, "Received request to open Play Store on phone from watch node: ${messageEvent.sourceNodeId}")
                // The WearConnectivityHelper's openPlayStoreOnPhone method is designed for this
                wearConnectivityHelper.openPlayStoreOnPhone()
            }
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
                                val reminder = medicationReminderRepository.getReminderById(reminderId)
                                if (reminder != null) {
                                    val medicationId = reminder.medicationId
                                    val updatedScheduleItems = getTodayScheduleForMedication(medicationId)
                                    val jsonPayload = gson.toJson(updatedScheduleItems.map { item ->
                                        mapOf(
                                            "id" to item.id,
                                            "medicationName" to item.medicationName,
                                            "time" to item.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                                            "isTaken" to item.isTaken,
                                            "underlyingReminderId" to item.underlyingReminderId.toString(),
                                            "medicationScheduleId" to item.medicationScheduleId,
                                            "takenAt" to item.takenAt
                                        )
                                    })
                                    sendDataToWear(PATH_TODAY_SCHEDULE_SYNC, jsonPayload, "today's schedule (update)")
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
            else -> {
                Log.w(TAG, "Unknown message path: ${messageEvent.path}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

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
        val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

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
        return sortedList.distinctBy {
            if (it.isTaken && it.underlyingReminderId != 0L) "db_taken_${it.underlyingReminderId}"
            else if (it.isTaken && it.takenAt != null) "adhoc_taken_${it.takenAt}"
            else "slot_untaken_${it.medicationScheduleId}_${it.time.truncatedTo(ChronoUnit.MINUTES)}"
        }
    }

    private fun sendDataToWear(path: String, jsonData: String, itemTypeForLog: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val putDataMapReq = PutDataMapRequest.create(path)
                putDataMapReq.dataMap.putString("sync_data_json", jsonData)
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
        private const val PATH_FULL_MED_DATA_SYNC = "/full_medication_data_sync"
        private const val PATH_TODAY_SCHEDULE_SYNC = "/today_schedule"
        private const val PATH_OPEN_PLAY_STORE_ON_PHONE = "/open_play_store" // Path from Wear OS app
    }

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
            if (!medication.endDate.isNullOrBlank()) {
                try {
                    val endDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
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
                if ((schedule.scheduleType == ScheduleType.DAILY || schedule.scheduleType == ScheduleType.CUSTOM_ALARMS) && schedule.specificTimes.isNullOrEmpty()) {
                    Log.w(TAG, "Medication ${medication.id}, Schedule ${schedule.id} is ${schedule.scheduleType.name} but has no specific times. Skipping this schedule.")
                    null
                } else {
                    MedicationScheduleDetailSyncItem(
                        scheduleId = schedule.id.toLong(),
                        scheduleType = schedule.scheduleType.name,
                        specificTimes = schedule.specificTimes?.map { it.format(DateTimeFormatter.ofPattern("HH:mm")) },
                        intervalHours = schedule.intervalHours,
                        intervalMinutes = schedule.intervalMinutes,
                        intervalStartTime = schedule.intervalStartTime?.let { LocalTime.parse(it).format(DateTimeFormatter.ofPattern("HH:mm")) },
                        intervalEndTime = schedule.intervalEndTime?.let { LocalTime.parse(it).format(DateTimeFormatter.ofPattern("HH:mm")) },
                        dailyRepetitionDays = schedule.daysOfWeek?.map { it.name }
                    )
                }
            }

            if (schedulesForMed.isNotEmpty() && scheduleDetailSyncItems.isEmpty() &&
                schedulesForMed.any { (it.scheduleType == ScheduleType.DAILY || it.scheduleType == ScheduleType.CUSTOM_ALARMS) && it.specificTimes.isNullOrEmpty() }) {
                Log.w(TAG, "Medication ${medication.name} (ID: ${medication.id}) has schedules, but all were deemed invalid for sync. Skipping this medication.")
                continue
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
                    startDate = medication.startDate,
                    endDate = medication.endDate
                )
            )
        }

        if (medicationFullSyncItems.isEmpty() && allDbMedications.isNotEmpty()) {
             Log.w(TAG, "All medications were filtered out (e.g. past end date or invalid schedules). Sending empty list.")
        }

        val jsonToSend = gson.toJson(medicationFullSyncItems)
        sendDataToWear(PATH_FULL_MED_DATA_SYNC, jsonToSend, "full medication data")
        Log.i(TAG, "Full medication data sync triggered. Sent ${medicationFullSyncItems.size} medication items.")
    }
}
