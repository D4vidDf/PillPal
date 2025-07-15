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
import com.d4viddf.medicationreminder.wear.persistence.ReminderStateEntity
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Removed GlobalWearReminderRepository object

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
                    // Legacy path - to be removed or refactored if still needed for a specific purpose.
                    // For now, commenting out to focus on PATH_FULL_MED_DATA_SYNC
                    /*
                    TODAY_SCHEDULE_PATH -> {
                        Log.i(TAG, "Received data update for $TODAY_SCHEDULE_PATH (legacy). Processing skipped.")
                        // try {
                        //     val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        //     val json = dataMap.getString("schedule_json")
                        //     if (json != null) {
                        //         val typeToken = object : TypeToken<List<Map<String, Any?>>>() {}.type
                        //         val receivedMaps: List<Map<String, Any?>> = gson.fromJson(json, typeToken)
                        //         val reminders = receivedMaps.mapNotNull { map ->
                        //             try {
                        //                 WearReminder(
                        //                     id = map["id"] as? String ?: System.currentTimeMillis().toString(),
                        //                     underlyingReminderId = (map["underlyingReminderId"] as? String)?.toLongOrNull() ?: 0L,
                        //                     medicationName = map["medicationName"] as? String ?: "Unknown",
                        //                     time = map["time"] as? String ?: "00:00",
                        //                     isTaken = map["isTaken"] as? Boolean ?: false,
                        //                     dosage = map["dosage"] as? String ?: ""
                        //                 )
                        //             } catch (e: Exception) {
                        //                 Log.e(TAG, "Error parsing individual reminder map for legacy schedule: $map", e)
                        //                 null
                        //             }
                        //         }
                        //         // serviceScope.launch {
                        //         //     GlobalWearReminderRepository.reminders.value = reminders // No longer used
                        //         //     Log.i(TAG, "Updated global reminder state (legacy $TODAY_SCHEDULE_PATH) with ${reminders.size} items.")
                        //         // }
                        //     } else {
                        //         Log.w(TAG, "schedule_json is null in DataItem for $TODAY_SCHEDULE_PATH.")
                        //     }
                        // } catch (e: Exception) {
                        //     Log.e(TAG, "Error processing data item for $TODAY_SCHEDULE_PATH", e)
                        // }
                    }
                    */
                    TODAY_SCHEDULE_PATH -> {
                        Log.i(TAG, "Received data update for $TODAY_SCHEDULE_PATH.")
                        try {
                            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                            val json = dataMap.getString("schedule_json")
                            if (json != null) {
                                val typeToken = object : TypeToken<List<Map<String, Any?>>>() {}.type
                                val receivedMaps: List<Map<String, Any?>> = gson.fromJson(json, typeToken)
                                val reminders = receivedMaps.mapNotNull { map ->
                                    try {
                                        WearReminder(
                                            id = map["id"] as? String ?: System.currentTimeMillis().toString(),
                                            medicationId = (map["medicationId"] as? Double)?.toInt() ?: 0,
                                            scheduleId = (map["medicationScheduleId"] as? Double)?.toLong() ?: 0L,
                                            underlyingReminderId = (map["underlyingReminderId"] as? String)?.toLongOrNull() ?: 0L,
                                            medicationName = map["medicationName"] as? String ?: "Unknown",
                                            time = map["time"] as? String ?: "00:00",
                                            isTaken = map["isTaken"] as? Boolean ?: false,
                                            dosage = map["dosage"] as? String ?: "",
                                            takenAt = map["takenAt"] as? String
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing individual reminder map for legacy schedule: $map", e)
                                        null
                                    }
                                }
                                serviceScope.launch {
                                    val dao = WearAppDatabase.getDatabase(applicationContext).medicationSyncDao()
                                    reminders.forEach { reminder ->
                                        val state = ReminderStateEntity(
                                            reminderInstanceId = reminder.id,
                                            medicationId = reminder.medicationId,
                                            scheduleId = reminder.scheduleId,
                                            reminderTimeKey = reminder.time,
                                            isTaken = reminder.isTaken,
                                            takenAt = reminder.takenAt
                                        )
                                        dao.insertOrUpdateReminderState(state)
                                    }
                                    Log.i(TAG, "Updated ${reminders.size} reminder states from $TODAY_SCHEDULE_PATH.")
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

                                serviceScope.launch {
                                    val dao = WearAppDatabase.getDatabase(applicationContext).medicationSyncDao()
                                    dao.clearAndInsertFullSyncData(
                                        medicationEntities,
                                        scheduleEntities,
                                        medicationInfoEntities,
                                        medicationTypeEntities,
                                        reminderEntities
                                    )
                                    Log.i(TAG, "Successfully stored data in Room via $PATH_FULL_MED_DATA_SYNC.")
                                }
                            } else {
                                Log.w(TAG, "sync_data_json is null in DataItem for $PATH_FULL_MED_DATA_SYNC.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing data item for $PATH_FULL_MED_DATA_SYNC", e)
                        }
                    }
                    else -> {
                        Log.w(TAG, "Received data for unknown path: ${dataItem.uri.path}")
                    }
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                Log.i(TAG, "Data item deleted: ${event.dataItem.uri}")
                if (event.dataItem.uri.path == PATH_FULL_MED_DATA_SYNC) {
                    Log.i(TAG, "$PATH_FULL_MED_DATA_SYNC deleted. Clearing local synced medication data from Room.")
                    serviceScope.launch {
                        val dao = WearAppDatabase.getDatabase(applicationContext).medicationSyncDao()
                        dao.clearAllMedications()
                        dao.clearAllSchedules() // Explicitly clear schedules
                        dao.clearAllReminderStates() // Also clear reminder states
                        Log.i(TAG, "Cleared all synced medication, schedule, and reminder state data from Room.")
                    }
                }
                // else if (event.dataItem.uri.path == TODAY_SCHEDULE_PATH) { // Legacy
                //     Log.i(TAG, "DataItem for $TODAY_SCHEDULE_PATH deleted. Clearing legacy GlobalWearReminderRepository.")
                //     serviceScope.launch {
                //         // GlobalWearReminderRepository.reminders.value = emptyList() // No longer used
                //     }
                // }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        private const val TAG = "WearDataListenerSvc"
        private const val TODAY_SCHEDULE_PATH = "/today_schedule"
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
