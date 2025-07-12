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
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.wear.phone.interactions.PhoneDeviceType
import androidx.wear.remote.interactions.RemoteActivityHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearViewModel(application: Application) : AndroidViewModel(application), CapabilityClient.OnCapabilityChangedListener {

    private val _reminders = MutableStateFlow<List<WearReminder>>(emptyList())
    val reminders: StateFlow<List<WearReminder>> = _reminders.asStateFlow()

    private val _isLoading = MutableStateFlow(false) // Added for UI state
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _phoneAppStatus = MutableStateFlow(PhoneAppStatus.UNKNOWN) // New state for detailed status
    val phoneAppStatus: StateFlow<PhoneAppStatus> = _phoneAppStatus.asStateFlow()

    // private val _isConnectedToPhone = MutableStateFlow(false) // Replaced by phoneAppStatus
    // val isConnectedToPhone: StateFlow<Boolean> = _isConnectedToPhone.asStateFlow() // Replaced

    // private val communicationService = WearableCommunicationService(application) // Direct use of MessageClient
    // private val nodeClient by lazy { Wearable.getNodeClient(application) } // Used within capability checks
    // private val messageClient by lazy { Wearable.getMessageClient(application) } // Passed as parameter or used locally
    // private val capabilityClient by lazy { Wearable.getCapabilityClient(application) } // Passed as parameter

    private val medicationSyncDao: MedicationSyncDao by lazy {
        WearAppDatabase.getDatabase(application).medicationSyncDao()
    }
    private val gson = Gson() // For converting JSON strings in ScheduleDetailSyncEntity

    // Define a capability string that the phone app advertises
    private val PHONE_APP_CAPABILITY_NAME = "medication_reminder_phone_app_capability" // Corrected capability name

    companion object {
        private const val TAG = "WearViewModel"
        private const val REQUEST_INITIAL_SYNC_PATH = "/request_initial_sync"
        // private const val REQUEST_MANUAL_SYNC_PATH = "/request_manual_sync" // Path from phone - not used in this flow
        private const val MARK_AS_TAKEN_PATH = "/mark_as_taken"
        private const val PLAY_STORE_APP_URI = "market://details?id=com.d4viddf.medicationreminder" // Replace with actual package name if different for phone app
    }

    init {
        // Combined observation of medications and their states
        observeMedicationAndReminderStates()
        // checkPhoneAppInstallation is now called from UI (LaunchedEffect)
        // listenForConnectionChanges is implicitly handled by onCapabilityChanged
        // registerMessageListener() // If needed for messages from phone to watch beyond data sync
    }

    private fun observeMedicationAndReminderStates() {
        viewModelScope.launch {
            // Combine medication data with reminder states
            medicationSyncDao.getAllMedicationsWithSchedules()
                .combine(medicationSyncDao.getAllReminderStates()) { medsWithSchedules, reminderStates ->
                    Log.d(TAG, "Observed ${medsWithSchedules.size} meds & ${reminderStates.size} states from Room.")
                    calculateRemindersFromSyncData(medsWithSchedules, reminderStates)
                }.collect { calculatedReminders ->
                    _reminders.value = calculatedReminders
                    _isLoading.value = false // Stop loading once data is processed
                    // Update phoneAppStatus if we have data
                    if (calculatedReminders.isNotEmpty() && _phoneAppStatus.value == PhoneAppStatus.INSTALLED_DATA_REQUESTED || _phoneAppStatus.value == PhoneAppStatus.INSTALLED_NO_DATA) {
                        _phoneAppStatus.value = PhoneAppStatus.INSTALLED_WITH_DATA
                    } else if (calculatedReminders.isEmpty() && _phoneAppStatus.value == PhoneAppStatus.INSTALLED_DATA_REQUESTED) {
                        // Still no reminders after sync, could be no data or just none for today.
                        // If we are sure sync happened and it's empty, then INSTALLED_WITH_DATA (empty list) is fine.
                        // For now, let phoneAppStatus reflect that data was processed.
                         _phoneAppStatus.value = PhoneAppStatus.INSTALLED_WITH_DATA // even if list is empty, data processed
                    }
                    Log.d(TAG, "Updated ViewModel reminders based on Room: ${calculatedReminders.size} items. Status: ${_phoneAppStatus.value}")
                }
        }
    }

    private fun calculateRemindersFromSyncData(
        medicationsWithSchedules: List<MedicationWithSchedulesPojo>,
        reminderStates: List<ReminderStateEntity>
    ): List<WearReminder> {
        // _isLoading.value = true // isLoading is managed by the collect block or calling function
        val today = LocalDate.now()
        // val now = LocalTime.now() // 'now' is not used in the current sorting, can be removed if only sorting by time
        val allCalculatedReminders = mutableListOf<WearReminder>()

        medicationsWithSchedules.forEach { medPojo ->
            val medication = medPojo.medication
            val startDate = medication.startDate?.let { runCatching { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull() }
            val endDate = medication.endDate?.let { runCatching { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull() }

            if (startDate != null && startDate.isAfter(today)) return@forEach
            if (endDate != null && endDate.isBefore(today)) return@forEach

            medPojo.schedules.forEach { scheduleEntity ->
                val specificTimes: List<LocalTime>? = scheduleEntity.specificTimesJson?.let { json ->
                    runCatching {
                        val typeToken = object : TypeToken<List<String>>() {}.type
                        gson.fromJson<List<String>>(json, typeToken).map { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) }
                    }.getOrNull()
                }
                val dailyRepetitionDays: List<String>? = scheduleEntity.dailyRepetitionDaysJson?.let { json ->
                     runCatching {
                        val typeToken = object : TypeToken<List<String>>() {}.type
                        gson.fromJson<List<String>>(json, typeToken)
                     }.getOrNull()
                }

                // Logic to generate reminder instances based on scheduleType
                if (scheduleEntity.scheduleType == "DAILY_SPECIFIC_TIMES" || scheduleEntity.scheduleType == "CUSTOM_ALARMS") { // Assuming CUSTOM_ALARMS also use specificTimes
                    if (dailyRepetitionDays.isNullOrEmpty() || dailyRepetitionDays.contains(today.dayOfWeek.name)) {
                        specificTimes?.forEach { time ->
                            val reminderTimeKey = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                            // Ensure reminderInstanceId is consistent and can be regenerated
                            val reminderInstanceId = "med${medication.medicationId}_sched${scheduleEntity.scheduleId}_day${today.toEpochDay()}_time${reminderTimeKey.replace(":", "")}"

                            val state = reminderStates.find { it.reminderInstanceId == reminderInstanceId }
                            allCalculatedReminders.add(
                                WearReminder(
                                    id = reminderInstanceId, // This is the watch's unique ID for this instance
                                    underlyingReminderId = scheduleEntity.scheduleId, // Default to scheduleId, phone might send a more specific one
                                    medicationName = medication.name,
                                    dosage = medication.dosage,
                                    time = reminderTimeKey,
                                    isTaken = state?.isTaken ?: false,
                                    takenAt = state?.takenAt // Pass this along
                                )
                            )
                        }
                    }
                } else if (scheduleEntity.scheduleType == "INTERVAL") {
                    val intervalStartTimeStr = scheduleEntity.intervalStartTime
                    val intervalHours = scheduleEntity.intervalHours
                    val intervalMinutes = scheduleEntity.intervalMinutes

                    if (intervalStartTimeStr != null && intervalHours != null && intervalMinutes != null) {
                        var currentTimeSlot = runCatching { LocalTime.parse(intervalStartTimeStr, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() ?: return@forEach
                        val intervalEndTime = scheduleEntity.intervalEndTime?.let { runCatching { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() } ?: LocalTime.MAX
                        val intervalDuration = java.time.Duration.ofHours(intervalHours.toLong()).plusMinutes(intervalMinutes.toLong())

                        if (intervalDuration.isZero || intervalDuration.isNegative) return@forEach

                        while (!currentTimeSlot.isAfter(intervalEndTime)) {
                            val reminderTimeKey = currentTimeSlot.format(DateTimeFormatter.ofPattern("HH:mm"))
                            val reminderInstanceId = "med${medication.medicationId}_sched${scheduleEntity.scheduleId}_day${today.toEpochDay()}_time${reminderTimeKey.replace(":", "")}"
                            val state = reminderStates.find { it.reminderInstanceId == reminderInstanceId }

                            allCalculatedReminders.add(
                                WearReminder(
                                    id = reminderInstanceId,
                                    underlyingReminderId = scheduleEntity.scheduleId, // Default to scheduleId
                                    medicationName = medication.name,
                                    dosage = medication.dosage,
                                    time = reminderTimeKey,
                                    isTaken = state?.isTaken ?: false,
                                    takenAt = state?.takenAt
                                )
                            )
                            val nextSlot = currentTimeSlot.plus(intervalDuration)
                            if (nextSlot.isBefore(currentTimeSlot) || nextSlot == currentTimeSlot) break // Avoid infinite loop or wrapping issues
                            currentTimeSlot = nextSlot
                        }
                    }
                }
            }
        }
        _isLoading.value = false
        // Sort by time, then by whether it's taken (untaken first)
        return allCalculatedReminders.sortedWith(
            compareBy<WearReminder> { LocalTime.parse(it.time, DateTimeFormatter.ofPattern("HH:mm")) }
            .thenBy { it.isTaken }
        )
    }

    /**
     * Called when a reminder is marked as taken directly on the watch.
     * This updates the local Room database and then triggers a message to the phone.
     * The phoneDbReminderId is the ID that the phone app uses for this reminder instance.
     */
    fun markReminderAsTakenOnWatch(
        reminderInstanceId: String, // Watch's unique ID for this slot
        medicationId: Int,
        scheduleId: Long, // This is the scheduleId from the watch's perspective
        reminderTimeKey: String, // HH:mm format of the reminder time
        phoneDbReminderId: Long // The ID from the phone's database, if this reminder originated from phone sync
    ) {
        viewModelScope.launch {
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val newState = ReminderStateEntity(
                reminderInstanceId = reminderInstanceId, // Watch's unique ID for this slot
                medicationId = medicationId,
                scheduleId = scheduleId,
                reminderTimeKey = reminderTimeKey,
                isTaken = true,
                takenAt = now
            )
            medicationSyncDao.insertOrUpdateReminderState(newState)
            Log.i(TAG, "Marked reminder $reminderInstanceId as taken locally on watch. State: $newState")

            // Now, inform the phone using the phoneDbReminderId
            if (phoneDbReminderId != 0L) { // A non-zero ID means it likely originated from the phone or has a known mapping
                markReminderAsTakenOnPhone(phoneDbReminderId.toString(), Wearable.getMessageClient(getApplication()))
            } else {
                // This case means the reminder instance was likely generated purely on the watch
                // (e.g., an interval slot that phone didn't explicitly create a reminder row for yet)
                val adhocTakenData = AdhocTakenPayload(medicationId, scheduleId, reminderTimeKey, now)
                val jsonData = gson.toJson(adhocTakenData)
                Log.w(TAG, "No direct phone DB ID for $reminderInstanceId. Sending ad-hoc taken message to phone with payload: $jsonData")

                try {
                     val capabilityInfo = Wearable.getCapabilityClient(getApplication<Application>())
                        .getCapability(PHONE_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                        .await()
                    val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }
                    if (phoneNode != null) {
                        Wearable.getMessageClient(getApplication()).sendMessage(
                            phoneNode.id,
                            "/mark_adhoc_taken_on_watch", // New path for these cases
                            jsonData.toByteArray(Charsets.UTF_8)
                        )
                        .addOnSuccessListener { Log.i(TAG, "Sent ad-hoc taken message to phone for $reminderInstanceId") }
                        .addOnFailureListener { e -> Log.e(TAG, "Failed ad-hoc taken message for $reminderInstanceId", e) }
                    } else {
                         Log.e(TAG, "Failed to send ad-hoc taken message: No connected phone node.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception sending ad-hoc taken message", e)
                }
            }
        }
    }
    // Payload for ad-hoc "taken" events originating from watch for non-explicitly synced reminder instances
    private data class AdhocTakenPayload(val medicationId: Int, val scheduleId: Long, val reminderTimeKey: String, val takenAt: String)

    fun checkPhoneAppInstallation(
        capabilityClient: CapabilityClient,
        remoteActivityHelper: RemoteActivityHelper, // Keep for opening Play Store
        messageClient: MessageClient
    ) {
        _phoneAppStatus.value = PhoneAppStatus.CHECKING
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val capabilityInfo = capabilityClient
                    .getCapability(PHONE_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                    .await()
                val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }

                if (phoneNode != null) {
                    Log.i(TAG, "Phone app capability found on node: ${phoneNode.displayName}. Requesting initial sync.")
                    _phoneAppStatus.value = PhoneAppStatus.INSTALLED_DATA_REQUESTED
                    requestInitialSyncData(messageClient, phoneNode.id)
                    // _isLoading will be set to false by the data observation flow
                } else {
                    Log.w(TAG, "Phone app capability '$PHONE_APP_CAPABILITY_NAME' not found on any reachable node.")
                    val deviceType = PhoneDeviceType.getPhoneDeviceType(getApplication<Application>().applicationContext)
                    _phoneAppStatus.value = when (deviceType) {
                        PhoneDeviceType.DEVICE_TYPE_ANDROID -> PhoneAppStatus.NOT_INSTALLED_ANDROID
                        PhoneDeviceType.DEVICE_TYPE_IOS -> PhoneAppStatus.NOT_INSTALLED_IOS
                        else -> PhoneAppStatus.UNKNOWN
                    }
                    _isLoading.value = false // Stop loading if no app found
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking phone app installation/capability", e)
                _phoneAppStatus.value = PhoneAppStatus.UNKNOWN
                _isLoading.value = false // Stop loading on error
            }
        }
        capabilityClient.removeListener(this, PHONE_APP_CAPABILITY_NAME) // Ensure no duplicates
        capabilityClient.addListener(this, PHONE_APP_CAPABILITY_NAME)
    }

    private fun requestInitialSyncData(messageClient: MessageClient, nodeId: String) { // Corrected nodeId parameter name
        viewModelScope.launch {
            // _isLoading is already true if status is CHECKING or DATA_REQUESTED
            try {
                messageClient.sendMessage(nodeId, REQUEST_INITIAL_SYNC_PATH, ByteArray(0))
                    .addOnSuccessListener {
                        Log.i(TAG, "Initial sync request sent to node $nodeId. Current status: ${_phoneAppStatus.value}")
                        // UI will continue showing loading until data arrives or fails, then observer updates status/isLoading.
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to send initial sync request to node $nodeId", e)
                        if (_phoneAppStatus.value == PhoneAppStatus.INSTALLED_DATA_REQUESTED) {
                           _phoneAppStatus.value = PhoneAppStatus.INSTALLED_NO_DATA
                        }
                        _isLoading.value = false // Explicitly stop loading on failure to send
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception sending initial sync request to node $nodeId", e)
                 if (_phoneAppStatus.value == PhoneAppStatus.INSTALLED_DATA_REQUESTED) {
                    _phoneAppStatus.value = PhoneAppStatus.INSTALLED_NO_DATA
                 }
                _isLoading.value = false // Explicitly stop loading on exception
            }
        }
    }

    /**
     * Called by UI elements when a reminder is marked as taken.
     * It first calls `markReminderAsTakenOnWatch` to update local state,
     * which then calls this `markReminderAsTakenOnPhone` if a phoneDbReminderId exists.
     */
    fun markReminderAsTakenOnPhone(phoneDbReminderId: String, messageClient: MessageClient) {
        val id = phoneDbReminderId.toLongOrNull()
        if (id != null && id != 0L) { // Ensure it's a valid ID that likely came from the phone.
            viewModelScope.launch {
                try {
                    val capabilityInfo = Wearable.getCapabilityClient(getApplication<Application>())
                        .getCapability(PHONE_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                        .await()
                    val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }
                    if (phoneNode != null) {
                        messageClient.sendMessage(phoneNode.id, MARK_AS_TAKEN_PATH, phoneDbReminderId.toByteArray(Charsets.UTF_8))
                            .addOnSuccessListener { Log.i(TAG,"MarkAsTaken message sent to phone for DB ID $phoneDbReminderId to ${phoneNode.displayName}") }
                            .addOnFailureListener { e -> Log.e(TAG, "Failed to send MarkAsTaken message for phone DB ID $phoneDbReminderId", e) }
                    } else {
                        Log.w(TAG, "Cannot send MarkAsTaken to phone: No connected phone node with capability.")
                        // TODO: Implement queuing mechanism for offline actions for more robust sync.
                        // For now, local state is updated, but phone sync will fail.
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending MarkAsTaken message to phone", e)
                }
            }
        } else {
            // This log should ideally not be hit if `markReminderAsTakenOnWatch` correctly routes ad-hoc cases.
            Log.w(TAG, "markReminderAsTakenOnPhone called with invalid or zero phoneDbReminderId ($phoneDbReminderId). Should be handled by ad-hoc path if watch-originated without phone ID.")
        }
    }


    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged: ${capabilityInfo.name}, Nodes: ${capabilityInfo.nodes.joinToString { it.displayName }}")
        if (capabilityInfo.name == PHONE_APP_CAPABILITY_NAME) {
            val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }
            if (phoneNode != null) {
                if (_phoneAppStatus.value != PhoneAppStatus.INSTALLED_WITH_DATA && _phoneAppStatus.value != PhoneAppStatus.INSTALLED_DATA_REQUESTED) {
                    Log.i(TAG, "Phone app connected/reconnected: ${phoneNode.displayName}. Requesting initial sync.")
                    _phoneAppStatus.value = PhoneAppStatus.INSTALLED_DATA_REQUESTED
                    requestInitialSyncData(Wearable.getMessageClient(getApplication()), phoneNode.id)
                } else {
                     Log.i(TAG, "Phone app capability detected, status is already ${_phoneAppStatus.value}")
                }
            } else {
                Log.w(TAG, "Phone app capability lost or no reachable node.")
                // Update status, could be NOT_INSTALLED_ANDROID if we re-check phone type, or just UNKNOWN/ERROR
                 val currentContext = getApplication<Application>().applicationContext
                 val deviceType = PhoneDeviceType.getPhoneDeviceType(currentContext)
                _phoneAppStatus.value = when (deviceType) {
                    PhoneDeviceType.DEVICE_TYPE_ANDROID -> PhoneAppStatus.NOT_INSTALLED_ANDROID // Or some "disconnected" state
                    PhoneDeviceType.DEVICE_TYPE_IOS -> PhoneAppStatus.NOT_INSTALLED_IOS
                    else -> PhoneAppStatus.UNKNOWN
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Wearable.getCapabilityClient(getApplication<Application>()).removeListener(this, PHONE_APP_CAPABILITY_NAME)
        Log.d(TAG, "ViewModel cleared, capability listener removed.")
    }

    fun openPlayStoreOnPhone(remoteActivityHelper: RemoteActivityHelper) {
        viewModelScope.launch {
            try {
                // No need to find node, RemoteActivityHelper handles it if phone is Android
                remoteActivityHelper.startRemoteActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW)
                        .addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                        .setData(android.net.Uri.parse(PLAY_STORE_APP_URI))
                ).await() // Using Tasks.await() from kotlinx-coroutines-play-services
                Log.i(TAG, "Attempted to open Play Store on phone.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open Play Store on phone", e)
                // Optionally update UI state to reflect failure
            }
        }
    }
}

enum class PhoneAppStatus {
    UNKNOWN,
    CHECKING,
    INSTALLED_WITH_DATA,
    INSTALLED_DATA_REQUESTED,
    INSTALLED_NO_DATA, // Installed but initial sync failed or no data yet
    NOT_INSTALLED_ANDROID,
    NOT_INSTALLED_IOS
}
