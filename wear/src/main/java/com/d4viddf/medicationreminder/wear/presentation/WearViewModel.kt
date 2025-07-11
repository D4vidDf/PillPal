package com.d4viddf.medicationreminder.wear.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.wear.data.MedicationScheduleDetailSyncItem
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.persistence.MedicationSyncDao
import com.d4viddf.medicationreminder.wear.persistence.MedicationWithSchedulesPojo
import com.d4viddf.medicationreminder.wear.persistence.ScheduleDetailSyncEntity
import com.d4viddf.medicationreminder.wear.persistence.WearAppDatabase
// import com.d4viddf.medicationreminder.wear.services.GlobalWearReminderRepository // To be replaced
import com.d4viddf.medicationreminder.wear.services.WearableCommunicationService
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearViewModel(application: Application) : AndroidViewModel(application), CapabilityClient.OnCapabilityChangedListener {

    private val _reminders = MutableStateFlow<List<WearReminder>>(emptyList())
    val reminders: StateFlow<List<WearReminder>> = _reminders.asStateFlow()

    private val _isConnectedToPhone = MutableStateFlow(false)
    val isConnectedToPhone: StateFlow<Boolean> = _isConnectedToPhone.asStateFlow()

    private val communicationService = WearableCommunicationService(application)
    private val nodeClient by lazy { Wearable.getNodeClient(application) }
    private val messageClient by lazy { Wearable.getMessageClient(application) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(application) }
    private val medicationSyncDao: MedicationSyncDao by lazy {
        WearAppDatabase.getDatabase(application).medicationSyncDao()
    }
    private val gson = Gson() // For converting JSON strings in ScheduleDetailSyncEntity

    // Define a capability string that the phone app advertises
    private val PHONE_APP_CAPABILITY_NAME = "medication_reminder_phone_app"

    companion object {
        private const val TAG = "WearViewModel"
        private const val REQUEST_INITIAL_SYNC_PATH = "/request_initial_sync"
        private const val REQUEST_MANUAL_SYNC_PATH = "/request_manual_sync" // Path from phone to request sync
    }

    init {
        observeSyncedMedications()
        listenForConnectionChanges()
        registerMessageListener()
        // Request initial sync will be triggered by listenForConnectionChanges or if already connected
    }

    private fun observeSyncedMedications() {
        viewModelScope.launch {
            medicationSyncDao.getAllMedicationsWithSchedules().collect { medicationsWithSchedules ->
                Log.d(TAG, "Observed ${medicationsWithSchedules.size} medications from Room.")
                // TODO: Implement reminder calculation logic here
                // For now, let's create placeholder WearReminder items
                val calculatedReminders = calculateRemindersFromSyncData(medicationsWithSchedules)
                _reminders.value = calculatedReminders
                Log.d(TAG, "Updated ViewModel reminders based on Room: ${calculatedReminders.size} items")
            }
        }
    }

    // Placeholder for reminder calculation logic
    private fun calculateRemindersFromSyncData(medicationsWithSchedules: List<MedicationWithSchedulesPojo>): List<WearReminder> {
        val today = LocalDate.now()
        val now = LocalTime.now()
        val allCalculatedReminders = mutableListOf<WearReminder>()

        medicationsWithSchedules.forEach { medPojo ->
            val medication = medPojo.medication
            // Check start and end dates
            val startDate = medication.startDate?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
            val endDate = medication.endDate?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }

            if (startDate != null && startDate.isAfter(today)) return@forEach // Medication hasn't started
            if (endDate != null && endDate.isBefore(today)) return@forEach // Medication has ended

            medPojo.schedules.forEach { scheduleEntity ->
                // Convert JSON strings back to lists
                val specificTimes: List<LocalTime>? = scheduleEntity.specificTimesJson?.let { json ->
                    val typeToken = object : TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(json, typeToken).map { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) }
                }
                val dailyRepetitionDays: List<String>? = scheduleEntity.dailyRepetitionDaysJson?.let { json ->
                    val typeToken = object : TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(json, typeToken)
                }

                when (scheduleEntity.scheduleType) {
                    "DAILY_SPECIFIC_TIMES" -> {
                        if (dailyRepetitionDays?.contains(today.dayOfWeek.name) == true || dailyRepetitionDays.isNullOrEmpty()) { // isNullOrEmpty means everyday
                            specificTimes?.forEach { time ->
                                // Create a unique ID for this reminder instance
                                val reminderInstanceId = "med_${medication.medicationId}_sched_${scheduleEntity.scheduleId}_time_${time.toNanoOfDay()}"
                                allCalculatedReminders.add(
                                    WearReminder(
                                        id = reminderInstanceId,
                                        underlyingReminderId = 0L, // This is locally generated, no direct phone DB ID
                                        medicationName = medication.name,
                                        dosage = medication.dosage,
                                        time = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                                        isTaken = false // TODO: Implement taken status tracking on watch
                                    )
                                )
                            }
                        }
                    }
                    "INTERVAL" -> {
                        val intervalStartTimeStr = scheduleEntity.intervalStartTime
                        val intervalEndTimeStr = scheduleEntity.intervalEndTime
                        val intervalHours = scheduleEntity.intervalHours
                        val intervalMinutes = scheduleEntity.intervalMinutes

                        if (intervalStartTimeStr != null && intervalHours != null && intervalMinutes != null) {
                            var currentTimeSlot = LocalTime.parse(intervalStartTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
                            val intervalEndTime = intervalEndTimeStr?.let { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) } ?: LocalTime.MAX

                            val intervalDuration = java.time.Duration.ofHours(intervalHours.toLong()).plusMinutes(intervalMinutes.toLong())

                            while (!currentTimeSlot.isAfter(intervalEndTime) && intervalDuration.seconds > 0) {
                                if (currentTimeSlot.isAfter(LocalTime.MIDNIGHT) || currentTimeSlot == LocalTime.MIDNIGHT) { // Ensure it's within the same day or handle跨天
                                     val reminderInstanceId = "med_${medication.medicationId}_sched_${scheduleEntity.scheduleId}_interval_${currentTimeSlot.toNanoOfDay()}"
                                    allCalculatedReminders.add(
                                        WearReminder(
                                            id = reminderInstanceId,
                                            underlyingReminderId = 0L,
                                            medicationName = medication.name,
                                            dosage = medication.dosage,
                                            time = currentTimeSlot.format(DateTimeFormatter.ofPattern("HH:mm")),
                                            isTaken = false // TODO: Implement taken status tracking
                                        )
                                    )
                                }
                                val nextTimeSlot = currentTimeSlot.plus(intervalDuration)
                                if (nextTimeSlot == currentTimeSlot) break // Avoid infinite loop if interval is zero
                                currentTimeSlot = nextTimeSlot
                                if (currentTimeSlot.isBefore(LocalTime.parse(intervalStartTimeStr, DateTimeFormatter.ofPattern("HH:mm")))) break // wrapped around midnight
                            }
                        }
                    }
                    // TODO: Add other schedule types if necessary
                }
            }
        }
        // Filter for today and sort
        return allCalculatedReminders
            .filter { LocalTime.parse(it.time, DateTimeFormatter.ofPattern("HH:mm")).isAfter(now) } // Only upcoming for "now"
            .sortedBy { it.time }
            // This is a simplified list for UI. Actual alarm scheduling would need more robust logic.
    }


    private fun registerMessageListener() {
        messageClient.addListener { messageEvent ->
            Log.d(TAG, "Message received on watch: ${messageEvent.path}")
            if (messageEvent.path == REQUEST_MANUAL_SYNC_PATH) {
                Log.i(TAG, "Received manual sync request from phone. Triggering initial sync request.")
                requestInitialSync()
            }
        }
    }

    internal fun requestInitialSync() {
        viewModelScope.launch {
            try {
                val capabilityInfo = capabilityClient
                    .getCapability(PHONE_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                    .await()
                val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }

                if (phoneNode != null) {
                    messageClient.sendMessage(phoneNode.id, REQUEST_INITIAL_SYNC_PATH, ByteArray(0))
                        .addOnSuccessListener {
                            Log.i(TAG, "Initial sync request sent to ${phoneNode.displayName}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send initial sync request", e)
                        }
                } else {
                    Log.w(TAG, "Cannot request initial sync: No connected phone node found with capability $PHONE_APP_CAPABILITY_NAME.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while requesting initial sync", e)
            }
        }
    }

    fun markReminderAsTaken(reminderId: Long) {
        if (reminderId != 0L) {
            communicationService.sendMarkAsTakenMessage(reminderId.toInt())
            Log.i(TAG, "Requested to mark reminder $reminderId as taken.")
        } else {
            Log.w(TAG, "Cannot mark reminder as taken: Invalid reminderId (0).")
        }
    }

    private fun checkConnectionStatus() {
        viewModelScope.launch {
            try {
                val connectedNodes: List<Node> = nodeClient.connectedNodes.await()
                _isConnectedToPhone.value = connectedNodes.any { it.isNearby }
                Log.d(TAG, "Initial connection status: ${_isConnectedToPhone.value}, Nodes: ${connectedNodes.joinToString { it.displayName }}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking connection status", e)
                _isConnectedToPhone.value = false
            }
        }
    }

    private fun listenForConnectionChanges() {
        capabilityClient.addListener(this, PHONE_APP_CAPABILITY_NAME)
        viewModelScope.launch {
            try {
                val capabilityInfo = capabilityClient.getCapability(
                    PHONE_APP_CAPABILITY_NAME,
                    CapabilityClient.FILTER_REACHABLE
                ).await()
                val currentlyConnected = capabilityInfo.nodes.any { it.isNearby }
                _isConnectedToPhone.value = currentlyConnected
                Log.d(TAG, "Initial capability check: ${capabilityInfo.name}, Connected: $currentlyConnected")
                if (currentlyConnected && _reminders.value.isEmpty()) {
                    Log.i(TAG, "Connected on init and reminders are empty, requesting initial sync.")
                    requestInitialSync()
                } else if (!currentlyConnected) {
                    // Fallback to node client check if capability is not immediately available but nodes might be
                    checkConnectionStatus()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching initial capability", e)
                checkConnectionStatus() // Check nodes as a fallback
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        val previouslyConnected = _isConnectedToPhone.value
        val currentlyConnected = capabilityInfo.nodes.any { it.isNearby }
        _isConnectedToPhone.value = currentlyConnected

        Log.d(TAG, "Capability changed: ${capabilityInfo.name}, WasConnected: $previouslyConnected, NowConnected: $currentlyConnected, Nodes: ${capabilityInfo.nodes.joinToString { it.displayName }}")

        if (currentlyConnected && (!previouslyConnected || _reminders.value.isEmpty())) {
            Log.i(TAG, "Connection established or re-established, or reminders empty. Requesting initial sync.")
            requestInitialSync()
        } else if (!currentlyConnected && previouslyConnected) {
            Log.i(TAG, "Disconnected from phone.")
            // Optionally, clear reminders or show a disconnected state more prominently
            // _reminders.value = emptyList() // Or rely on cached data if implementing that
        }
    }

    override fun onCleared() {
        super.onCleared()
        capabilityClient.removeListener(this, PHONE_APP_CAPABILITY_NAME)
        // For simple lambda listeners with MessageClient, explicit removal isn't always straightforward without listener instances.
        // The listener will simply stop being active when the ViewModel's scope is cleared.
        Log.d(TAG, "ViewModel cleared, capability listener removed.")
    }

    fun requestPhoneToOpenPlayStore() {
        viewModelScope.launch {
            try {
                val capabilityInfo = capabilityClient
                    .getCapability(PHONE_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                    .await()
                val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }

                if (phoneNode != null) {
                    messageClient.sendMessage(phoneNode.id, "/open_play_store", ByteArray(0)) // Using the path defined in phone app's DataLayerListenerService
                        .addOnSuccessListener {
                            Log.i(TAG, "Request to open Play Store on phone sent to ${phoneNode.displayName}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send request to open Play Store on phone", e)
                        }
                } else {
                    Log.w(TAG, "Cannot request phone to open Play Store: No connected phone node found with capability $PHONE_APP_CAPABILITY_NAME.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while requesting phone to open Play Store", e)
            }
        }
    }
}
