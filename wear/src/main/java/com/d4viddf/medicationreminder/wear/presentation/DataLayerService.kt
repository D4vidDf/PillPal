package com.d4viddf.medicationreminder.wear.presentation

import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.d4viddf.medicationreminder.wear.data.MedicationFullSyncItem
import com.d4viddf.medicationreminder.wear.persistence.MedicationSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.ScheduleDetailSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.WearAppDatabase
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
    private val TAG = "DataLayerService"

    private lateinit var capabilityClient: CapabilityClient

    override fun onCreate() {
        super.onCreate()
        capabilityClient = Wearable.getCapabilityClient(this)
        Log.d(TAG, "Service created, advertising capability.")
        // Advertise the capability as soon as the service is created
        capabilityClient.addLocalCapability(CAPABILITY_NAME)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed, removing capability.")
        // Clean up when the service is destroyed
        capabilityClient.removeLocalCapability(CAPABILITY_NAME)
    }

    // This is called when a connected device's capabilities change
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        super.onCapabilityChanged(capabilityInfo)
        Log.d(TAG, "Capability Changed: $capabilityInfo")
        // You can add logic here if you need to react to phone app changes
    }

    // This is called when you receive a message from the phone
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Message received: ${messageEvent.path}")
        if (messageEvent.path == "/request_manual_sync") {
            // The phone is asking us to request a sync.
            // This is part of the manual sync flow initiated from the phone app.
            Log.i(TAG, "Received manual sync request from phone. Triggering request for initial sync.")
            val messageClient = Wearable.getMessageClient(this)
            messageClient.sendMessage(messageEvent.sourceNodeId, "/request_initial_sync", null)
        }
    }

    // This is called when data items are changed (synced)
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "Data changed. Event count: ${dataEvents.count}")
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == "/full_medication_data_sync") {
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
                                    typeName = syncItem.typeName,
                                    typeIconUrl = syncItem.typeIconUrl,
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
                                        specificTimesJson = scheduleDetail.specificTimes?.let { Gson().toJson(it) },
                                        intervalHours = scheduleDetail.intervalHours,
                                        intervalMinutes = scheduleDetail.intervalMinutes,
                                        intervalStartTime = scheduleDetail.intervalStartTime,
                                        intervalEndTime = scheduleDetail.intervalEndTime,
                                        dailyRepetitionDaysJson = scheduleDetail.dailyRepetitionDays?.let { Gson().toJson(it) }
                                    )
                                }
                            }

                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = WearAppDatabase.getDatabase(applicationContext).medicationSyncDao()
                                dao.clearAndInsertFullSyncData(medicationEntities, scheduleEntities)
                                Log.i(TAG, "Successfully stored ${medicationEntities.size} medications and ${scheduleEntities.size} schedules in Room.")
                            }
                        } else {
                            Log.w(TAG, "sync_data_json is null in DataItem.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing data item for /full_medication_data_sync", e)
                    }
                }
            }
        }
    }
}
