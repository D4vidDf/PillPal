package com.d4viddf.medicationreminder.wear.presentation

import android.content.ContentValues.TAG
import android.content.Intent
import android.util.Log
import com.d4viddf.medicationreminder.wear.data.MedicationFullSyncItem
import com.d4viddf.medicationreminder.wear.persistence.MedicationInfoSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.MedicationReminderSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.MedicationSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.MedicationTypeSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.Reminder
import com.d4viddf.medicationreminder.wear.persistence.ScheduleDetailSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.WearAppDatabase
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataLayerService : WearableListenerService() {

    private val CAPABILITY_NAME = "medication_reminder_wear_app"
    private lateinit var capabilityClient: CapabilityClient

    override fun onCreate() {
        super.onCreate()
        capabilityClient = Wearable.getCapabilityClient(this)
        Log.d(TAG, "Service created, advertising capability.")
        capabilityClient.addLocalCapability(CAPABILITY_NAME)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed, removing capability.")
        capabilityClient.removeLocalCapability(CAPABILITY_NAME)
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        super.onCapabilityChanged(capabilityInfo)
        Log.d(TAG, "Capability Changed: $capabilityInfo")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Message received: ${messageEvent.path}")
        if (messageEvent.path == "/request_manual_sync") {
            Log.i(TAG, "Received manual sync request from phone. Triggering request for initial sync.")
            val messageClient = Wearable.getMessageClient(this)
            messageClient.sendMessage(messageEvent.sourceNodeId, "/request_initial_sync", null)
        } else if (messageEvent.path == "/start_activity") {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "Data changed. Event count: ${dataEvents.count}")

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                when (dataItem.uri.path) {
                    "/full_medication_data_sync" -> {
                        try {
                            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                            val json = dataMap.getString("sync_data_json")
                            if (json != null) {
                                val typeToken = object : TypeToken<List<MedicationFullSyncItem>>() {}.type
                                val receivedSyncItems: List<MedicationFullSyncItem> = Gson().fromJson(json, typeToken)
                                Log.d(TAG, "Successfully deserialized ${receivedSyncItems.size} MedicationFullSyncItem(s).")

                                val medicationEntities = receivedSyncItems.map { syncItem ->
                                    MedicationSyncEntity(
                                        medicationId = syncItem.medicationId,
                                        name = syncItem.name,
                                        dosage = syncItem.dosage,
                                        color = syncItem.color,
                                        startDate = syncItem.startDate,
                                        endDate = syncItem.endDate
                                    )
                                }
                                val scheduleEntities = receivedSyncItems.flatMap { syncItem ->
                                    syncItem.schedules.map { scheduleDetail ->
                                        ScheduleDetailSyncEntity(
                                            medicationId = syncItem.medicationId,
                                            scheduleId = scheduleDetail.scheduleId,
                                            scheduleType = scheduleDetail.scheduleType,
                                            specificTimesJson = scheduleDetail.specificTimes?.let {
                                                Gson().toJson(
                                                    it
                                                )
                                            },
                                            intervalHours = scheduleDetail.intervalHours,
                                            intervalMinutes = scheduleDetail.intervalMinutes,
                                            intervalStartTime = scheduleDetail.intervalStartTime,
                                            intervalEndTime = scheduleDetail.intervalEndTime,
                                            dailyRepetitionDaysJson = scheduleDetail.dailyRepetitionDays?.let {
                                                Gson().toJson(
                                                    it
                                                )
                                            }
                                        )
                                    }
                                }
                                val medicationInfoEntities = receivedSyncItems.mapNotNull { syncItem ->
                                    syncItem.info?.let {
                                        MedicationInfoSyncEntity(
                                            medicationId = syncItem.medicationId,
                                            notes = it.notes,
                                            instructions = it.instructions
                                        )
                                    }
                                }
                                val medicationTypeEntities = receivedSyncItems.mapNotNull { syncItem ->
                                    syncItem.type?.let {
                                        MedicationTypeSyncEntity(
                                            id = it.id,
                                            name = it.name,
                                            iconUrl = it.iconUrl
                                        )
                                    }
                                }
                                val reminderEntities = receivedSyncItems.flatMap { syncItem ->
                                    syncItem.reminders.map { reminder ->
                                        MedicationReminderSyncEntity(
                                            id = reminder.id,
                                            medicationId = syncItem.medicationId,
                                            reminderTime = reminder.reminderTime,
                                            isTaken = reminder.isTaken,
                                            takenAt = reminder.takenAt
                                        )
                                    }
                                }

                                CoroutineScope(Dispatchers.IO).launch {
                                    val db = WearAppDatabase.getDatabase(applicationContext)
                                    db.medicationSyncDao().clearAndInsertFullSyncData(
                                        medicationEntities,
                                        scheduleEntities,
                                        medicationInfoEntities,
                                        medicationTypeEntities,
                                        reminderEntities
                                    )
                                    Log.i(TAG, "Successfully stored data in Room.")
                                }
                            } else {
                                Log.w(TAG, "sync_data_json is null in DataItem.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing data item for /full_medication_data_sync", e)
                        }
                    }
                    "/today_schedule" -> {
                        // Logic for today's schedule is handled in WearViewModel
                    }
                    else -> {
                        if (dataItem.uri.path?.startsWith("/reminder/") == true) {
                            try {
                                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                                val reminderId = dataMap.getInt("reminder_id")
                                val isTaken = dataMap.getBoolean("reminder_is_taken")
                                val takenAt = dataMap.getString("reminder_taken_at")
                                val reminderTime = dataMap.getString("reminder_sched_time")
                                val medicationName = dataMap.getString("reminder_med_name") ?: "Unknown"

                                if (reminderTime != null) {
                                    val reminder = Reminder(
                                        id = reminderId,
                                        isTaken = isTaken,
                                        takenAt = takenAt,
                                        reminderTime = reminderTime,
                                        medicationName = medicationName
                                    )

                                    CoroutineScope(Dispatchers.IO).launch {
                                        val dao = WearAppDatabase.getDatabase(applicationContext).reminderDao()
                                        dao.insertOrUpdate(reminder)
                                        Log.i(TAG, "Successfully updated reminder $reminderId in Room.")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing data item for /reminder/", e)
                            }
                        }
                    }
                }
            }
        }
    }
}