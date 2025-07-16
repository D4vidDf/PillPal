package com.d4viddf.medicationreminder.services

import android.annotation.SuppressLint
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import com.d4viddf.medicationreminder.data.MedicationFullSyncItem
import com.d4viddf.medicationreminder.repository.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationScheduleDetailSyncItem
import com.d4viddf.medicationreminder.data.ScheduleType // Enum for schedule type
import com.d4viddf.medicationreminder.data.TodayScheduleItem
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.d4viddf.medicationreminder.common.IntentActionConstants
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
import com.d4viddf.medicationreminder.utils.WearConnectivityHelper // Added import

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
    @Inject
    lateinit var medicationInfoRepository: MedicationInfoRepository

    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val gson by lazy { Gson() }

    private val serviceJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception: ${throwable.message}", throwable)
    }
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob + exceptionHandler)

    // For opening Play Store on phone
    private lateinit var wearConnectivityHelper: WearConnectivityHelper // Initialize in onCreate

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        wearConnectivityHelper = WearConnectivityHelper(applicationContext)
        val filter = IntentFilter(IntentActionConstants.ACTION_DATA_CHANGED)
        registerReceiver(dataChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
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
                                Log.i(TAG, "Reminder $reminderId marked as taken successfully via watch. Triggering full sync.")
                                triggerFullSyncToWear() // Ensure this is called to update the watch
                            } else {
                                Log.e(TAG, "Failed to mark reminder $reminderId as taken via watch.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error marking reminder as taken or triggering full sync: ${e.message}", e)
                        }
                    }
                } else {
                    Log.e(TAG, "Could not parse reminder ID from message data: $reminderIdString")
                }
            }
            // Ensure DATA_CHANGED is handled if you are using DataItems for other purposes.
            // For now, the primary sync is triggered by messages.
            PATH_ADHOC_TAKEN_ON_WATCH -> {
                val payloadJson = String(messageEvent.data, StandardCharsets.UTF_8)
                Log.d(TAG, "Received adhoc_taken_on_watch with payload: $payloadJson")
                try {
                    val payload = gson.fromJson(payloadJson, AdhocTakenPayload::class.java)
                    // TODO: Implement logic to find or create a reminder record on the phone
                    // based on medicationId, scheduleId, reminderTimeKey, and mark it as taken at payload.takenAt.
                    // This might involve:
                    // 1. Checking if an existing reminder in phone's DB matches these criteria for today.
                    // 2. If not, potentially creating a new reminder record for this ad-hoc taken event.
                    // 3. Then marking it as taken.
                    // For now, just log it.
                    Log.i(TAG, "Processing ad-hoc taken event: MedID=${payload.medicationId}, SchedID=${payload.scheduleId}, TimeKey=${payload.reminderTimeKey}, TakenAt=${payload.takenAt}")
                    // After processing, trigger a full sync to ensure watch reflects any server-side changes or consolidations.
                    serviceScope.launch {
                        // Example: medicationReminderRepository.recordAdhocTakenEvent(payload.medicationId, payload.scheduleId, payload.reminderTimeKey, payload.takenAt)
                        // For now, simply log and trigger sync. A more robust implementation is needed here.
                        Log.w(TAG, "Ad-hoc taken event processing logic is a TODO. Triggering full sync.")
                        triggerFullSyncToWear()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing adhoc_taken_on_watch payload: $payloadJson", e)
                }
            }
            "/reminder-taken" -> {
                val reminderId = String(messageEvent.data).toIntOrNull()
                if (reminderId != null) {
                    serviceScope.launch {
                        val currentTimeIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                        medicationReminderRepository.markReminderAsTaken(reminderId, currentTimeIso)
                    }
                }
            }
            else -> {
                Log.w(TAG, "Unknown message path: ${messageEvent.path}")
            }
        }
    }

    private val dataChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == IntentActionConstants.ACTION_DATA_CHANGED) {
                Log.d(TAG, "Received data changed broadcast, triggering full sync to wear.")
                serviceScope.launch {
                    triggerFullSyncToWear()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        unregisterReceiver(dataChangeReceiver)
    }

    // Removed getTodayScheduleForMedication as full sync is preferred.
    // If specific today schedule sync is ever re-introduced, this can be added back.


    companion object {
        private const val TAG = "DataLayerListenerSvc"
        private const val PATH_MARK_AS_TAKEN = "/mark_as_taken"
        private const val PATH_REQUEST_INITIAL_SYNC = "/request_initial_sync"
        private const val PATH_FULL_MED_DATA_SYNC = "/full_medication_data_sync"
        // private const val PATH_TODAY_SCHEDULE_SYNC = "/today_schedule" // Considered legacy, full sync preferred
        private const val PATH_OPEN_PLAY_STORE_ON_PHONE = "/open_play_store" // Path from Wear OS app
        private const val PATH_ADHOC_TAKEN_ON_WATCH = "/mark_adhoc_taken_on_watch" // Path from Wear OS for ad-hoc
    }

    // Payload class for ad-hoc taken events from watch
    private data class AdhocTakenPayload(val medicationId: Int, val scheduleId: Long, val reminderTimeKey: String, val takenAt: String)

    suspend fun triggerFullSyncToWear() {
        Log.i(TAG, "Starting full medication data sync to Wear OS.")
        val allDbMedications = medicationRepository.getAllMedications().firstOrNull() ?: emptyList()

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

            val medType = if (medication.typeId != null) {
                medicationTypeRepository.getMedicationTypeById(medication.typeId)
            } else {
                null
            }
            val medInfo = medicationInfoRepository.getMedicationInfoById(medication.id)
            val reminders = medicationReminderRepository.getRemindersForMedication(medication.id).firstOrNull() ?: emptyList()

            medicationFullSyncItems.add(
                MedicationFullSyncItem(
                    medicationId = medication.id,
                    name = medication.name,
                    dosage = medication.dosage,
                    color = medication.color,
                    type = medType?.let {
                        com.d4viddf.medicationreminder.data.MedicationTypeSyncItem(
                            id = it.id,
                            name = it.name,
                            iconUrl = it.imageUrl
                        )
                    },
                    info = medInfo?.let {
                        com.d4viddf.medicationreminder.data.MedicationInfoSyncItem(
                            medicationId = it.medicationId,
                            notes = it.description,
                            instructions = it.safetyNotes
                        )
                    },
                    schedules = scheduleDetailSyncItems,
                    reminders = reminders.map {
                        com.d4viddf.medicationreminder.data.MedicationReminderSyncItem(
                            id = it.id,
                            medicationId = it.medicationId,
                            reminderTime = it.reminderTime.toString(),
                            isTaken = it.isTaken,
                            takenAt = it.takenAt
                        )
                    },
                    startDate = medication.startDate,
                    endDate = medication.endDate
                )
            )
        }

        if (medicationFullSyncItems.isEmpty() && allDbMedications.isNotEmpty()) {
             Log.w(TAG, "All medications were filtered out (e.g. past end date or invalid schedules). Sending empty list.")
        }

        val jsonToSend = gson.toJson(medicationFullSyncItems)
        val putDataMapReq = PutDataMapRequest.create(PATH_FULL_MED_DATA_SYNC)
        putDataMapReq.dataMap.putString("sync_data_json", jsonToSend)
        putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
        val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
        dataClient.putDataItem(putDataReq).await()
        Log.i(TAG, "Full medication data sync triggered. Sent ${medicationFullSyncItems.size} medication items.")

        sendTodayReminders()
    }

    private suspend fun sendTodayReminders() {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay().toString()
        val endOfDay = today.atTime(23, 59, 59).toString()
        val allReminders = medicationReminderRepository.getRemindersForDay(startOfDay, endOfDay).firstOrNull() ?: emptyList()
        val todayScheduleItems = allReminders.map { reminder ->
            val medication = medicationRepository.getMedicationById(reminder.medicationId)
            TodayScheduleItem(
                id = reminder.id.toString(),
                medicationName = medication?.name ?: "Unknown",
                time = try {
                    LocalDateTime.parse(reminder.reminderTime).toLocalTime()
                } catch (e: Exception) {
                    LocalTime.now()
                },
                isTaken = reminder.isTaken,
                underlyingReminderId = reminder.id.toLong(),
                medicationScheduleId = 0L, // This is not available in the reminder table
                takenAt = reminder.takenAt,
                isPast = LocalTime.parse(reminder.reminderTime).atDate(today).isBefore(LocalDateTime.now())
            )
        }

        val json = gson.toJson(todayScheduleItems)
        val putDataMapReq = PutDataMapRequest.create("/today_schedule")
        putDataMapReq.dataMap.putString("schedule_json", json)
        putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
        val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
        dataClient.putDataItem(putDataReq).await()
        Log.i(TAG, "Sent ${todayScheduleItems.size} today reminders to Wear OS.")
    }
}
