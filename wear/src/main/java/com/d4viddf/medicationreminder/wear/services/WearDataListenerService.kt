package com.d4viddf.medicationreminder.wear.services

import android.util.Log
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.google.android.gms.wearable.DataEvent
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
                if (dataItem.uri.path == TODAY_SCHEDULE_PATH) {
                    Log.i(TAG, "Received data update for $TODAY_SCHEDULE_PATH")
                    try {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        val json = dataMap.getString("schedule_json")
                        if (json != null) {
                            // Define a type token for List<Map<String, Any?>>
                            val typeToken = object : TypeToken<List<Map<String, Any?>>>() {}.type
                            val receivedMaps: List<Map<String, Any?>> = gson.fromJson(json, typeToken)

                            val reminders = receivedMaps.mapNotNull { map ->
                                try {
                                    WearReminder(
                                        // Ensure IDs are handled correctly (String vs Long)
                                        // The phone sends underlyingReminderId as String, id (TodayScheduleItem.id) as String
                                        id = map["id"] as? String ?: System.currentTimeMillis().toString(), // Fallback ID
                                        underlyingReminderId = (map["underlyingReminderId"] as? String)?.toLongOrNull() ?: 0L,
                                        medicationName = map["medicationName"] as? String ?: "Unknown",
                                        time = map["time"] as? String ?: "00:00", // HH:mm format
                                        isTaken = map["isTaken"] as? Boolean ?: false,
                                        dosage = map["dosage"] as? String ?: "" // Assuming dosage might be added later
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing individual reminder map: $map", e)
                                    null
                                }
                            }
                            Log.d(TAG, "Successfully deserialized ${reminders.size} reminders.")
                            serviceScope.launch {
                                // Update a ViewModel or a Repository with the new list
                                // For now, using a simple global state flow (placeholder)
                                GlobalWearReminderRepository.reminders.value = reminders
                                Log.i(TAG, "Updated global reminder state with ${reminders.size} items.")
                            }
                        } else {
                            Log.w(TAG, "schedule_json is null in DataItem.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing data item for $TODAY_SCHEDULE_PATH", e)
                    }
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                if (event.dataItem.uri.path == TODAY_SCHEDULE_PATH) {
                    Log.i(TAG, "DataItem for $TODAY_SCHEDULE_PATH deleted. Clearing reminders.")
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
        private const val TODAY_SCHEDULE_PATH = "/today_schedule"
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
