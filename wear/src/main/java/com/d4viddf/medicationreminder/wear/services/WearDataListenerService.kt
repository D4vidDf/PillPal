package com.d4viddf.medicationreminder.wear.services

import android.util.Log
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.google.android.gms.wearable.DataEvent
import com.d4viddf.medicationreminder.wear.data.MedicationFullSyncItem
import com.d4viddf.medicationreminder.wear.data.MedicationScheduleDetailSyncItem
import com.d4viddf.medicationreminder.wear.persistence.MedicationSyncDao
import com.d4viddf.medicationreminder.wear.persistence.MedicationSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.ScheduleDetailSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.WearAppDatabase
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// TODO: Replace with actual Hilt injection or manual singleton access for repository/ViewModel
object GlobalWearReminderRepository { // Temporary placeholder
    val reminders = kotlinx.coroutines.flow.MutableStateFlow<List<WearReminder>>(emptyList())
}

class WearDataListenerService : WearableListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val gson = Gson()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: $dataEvents")
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                when (dataItem.uri.path) {
                    TODAY_SCHEDULE_PATH -> {
                        Log.i(TAG, "Received data update for $TODAY_SCHEDULE_PATH (legacy or specific today view)")
                        // Current logic for TODAY_SCHEDULE_PATH - might be deprecated or used for quick updates
                        try {
                            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                            val json = dataMap.getString("schedule_json") // Assuming old key was "schedule_json"
                            if (json != null) {
                                val typeToken = object : TypeToken<List<Map<String, Any?>>>() {}.type
                                val receivedMaps: List<Map<String, Any?>> = gson.fromJson(json, typeToken)
                                val reminders = receivedMaps.mapNotNull { map ->
                                    try {
                                        WearReminder(
                                            id = map["id"] as? String ?: System.currentTimeMillis().toString(),
                                            underlyingReminderId = (map["underlyingReminderId"] as? String)?.toLongOrNull() ?: 0L,
                                            medicationName = map["medicationName"] as? String ?: "Unknown",
                                            time = map["time"] as? String ?: "00:00",
                                            isTaken = map["isTaken"] as? Boolean ?: false,
                                            dosage = map["dosage"] as? String ?: ""
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing individual reminder map for legacy schedule: $map", e)
                                        null
                                    }
                                }
                                serviceScope.launch {
                                    GlobalWearReminderRepository.reminders.value = reminders
                                    Log.i(TAG, "Updated global reminder state (legacy) with ${reminders.size} items.")
                                }
                            } else {
                                Log.w(TAG, "schedule_json is null in DataItem for $TODAY_SCHEDULE_PATH.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing data item for $TODAY_SCHEDULE_PATH", e)
                        }
                    }
                    PATH_FULL_MED_DATA_SYNC -> {
                        Log.i(TAG, "Received data update for $PATH_FULL_MED_DATA_SYNC")
                        try {
                            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                            val json = dataMap.getString("sync_data_json") // Key used in phone app
                            if (json != null) {
                                val typeToken = object : TypeToken<List<MedicationFullSyncItem>>() {}.type
                                val receivedSyncItems: List<MedicationFullSyncItem> = gson.fromJson(json, typeToken)
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
                                            specificTimesJson = scheduleDetail.specificTimes?.let { gson.toJson(it) },
                                            intervalHours = scheduleDetail.intervalHours,
                                            intervalMinutes = scheduleDetail.intervalMinutes,
                                            intervalStartTime = scheduleDetail.intervalStartTime,
                                            intervalEndTime = scheduleDetail.intervalEndTime,
                                            dailyRepetitionDaysJson = scheduleDetail.dailyRepetitionDays?.let { gson.toJson(it) }
                                        )
                                    }
                                }

                                serviceScope.launch {
                                    val dao = WearAppDatabase.getDatabase(applicationContext).medicationSyncDao()
                                    dao.clearAndInsertSyncData(medicationEntities, scheduleEntities)
                                    Log.i(TAG, "Successfully stored ${medicationEntities.size} medications and ${scheduleEntities.size} schedules in Room.")
                                    // TODO: Trigger UI update or ViewModel refresh if needed, now that data is in Room.
                                    // For example, WearViewModel could observe the DAO.
                                }
                            } else {
                                Log.w(TAG, "sync_data_json is null in DataItem for $PATH_FULL_MED_DATA_SYNC.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing data item for $PATH_FULL_MED_DATA_SYNC", e)
                        }
                    }
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                // Handle deletions if necessary, e.g., clear specific data or all data.
                // For full sync, TYPE_CHANGED with new data (even if empty) is often sufficient.
                Log.i(TAG, "Data item deleted: ${event.dataItem.uri}")
                if (event.dataItem.uri.path == PATH_FULL_MED_DATA_SYNC) {
                    Log.i(TAG, "$PATH_FULL_MED_DATA_SYNC deleted. Clearing local synced data.")
                    serviceScope.launch {
                        val dao = WearAppDatabase.getDatabase(applicationContext).medicationSyncDao()
                        dao.clearAllMedications() // This should cascade and clear schedules too
                        // dao.clearAllSchedules() // Explicitly if needed
                        Log.i(TAG, "Cleared all synced medication data from Room.")
                    }
                } else if (event.dataItem.uri.path == TODAY_SCHEDULE_PATH) {
                     Log.i(TAG, "DataItem for $TODAY_SCHEDULE_PATH deleted. Clearing legacy reminders.")
                    serviceScope.launch {
                        GlobalWearReminderRepository.reminders.value = emptyList()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        private const val TAG = "WearDataListenerSvc"
        private const val TODAY_SCHEDULE_PATH = "/today_schedule" // Legacy or specific today view
        private const val PATH_FULL_MED_DATA_SYNC = "/full_medication_data_sync" // New path
    }
}

// Define WearReminder data class (should be in its own file ideally, e.g., data/WearReminder.kt)
// This needs to align with what WearActivity expects and what phone sends.
// Based on phone's simplifiedItems:
// "id" (String - TodayScheduleItem.id), "medicationName" (String), "time" (String HH:mm),
// "isTaken" (Boolean), "underlyingReminderId" (String -> Long), "medicationScheduleId", "takenAt"
// The WearActivity current MedicationReminder has: id (String), name, dosage, time (Long timestamp), isTaken.
// We need to reconcile these. Let's create a new one.
// package com.d4viddf.medicationreminder.wear.data // (Example package)
// data class WearReminder(
//     val id: String, // Unique ID for the list item on Wear
//     val underlyingReminderId: Long, // The actual ID from the phone's database
//     val medicationName: String,
//     val time: String, // Expected HH:mm format from phone
//     val isTaken: Boolean,
//     val dosage: String? = null // Optional, if we want to include it
// )
// Moved WearReminder to its own file.
