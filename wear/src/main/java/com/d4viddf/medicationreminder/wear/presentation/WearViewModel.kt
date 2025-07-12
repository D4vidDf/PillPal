package com.d4viddf.medicationreminder.wear.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.persistence.MedicationSyncDao
import com.d4viddf.medicationreminder.wear.persistence.MedicationWithSchedulesPojo
import com.d4viddf.medicationreminder.wear.persistence.ReminderStateEntity
import com.d4viddf.medicationreminder.wear.persistence.WearAppDatabase
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
// Removed: import androidx.wear.phone.interactions.PhoneDeviceType
import androidx.wear.remote.interactions.RemoteActivityHelper

class WearViewModel(application: Application) : AndroidViewModel(application), CapabilityClient.OnCapabilityChangedListener {

    private val _reminders = MutableStateFlow<List<WearReminder>>(emptyList())
    val reminders: StateFlow<List<WearReminder>> = _reminders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _phoneAppStatus = MutableStateFlow(PhoneAppStatus.UNKNOWN)
    val phoneAppStatus: StateFlow<PhoneAppStatus> = _phoneAppStatus.asStateFlow()

    private val medicationSyncDao: MedicationSyncDao by lazy {
        WearAppDatabase.getDatabase(application).medicationSyncDao()
    }
    private val gson = Gson()

    private val PHONE_APP_CAPABILITY_NAME = "medication_reminder_phone_app_capability"

    companion object {
        private const val TAG = "WearViewModel"
        private const val REQUEST_INITIAL_SYNC_PATH = "/request_initial_sync"
        private const val MARK_AS_TAKEN_PATH = "/mark_as_taken"
        private const val ADHOC_TAKEN_PATH = "/mark_adhoc_taken_on_watch"
        private const val PLAY_STORE_APP_URI = "market://details?id=com.d4viddf.medicationreminder"
    }

    init {
        observeMedicationAndReminderStates()
    }

    private fun observeMedicationAndReminderStates() {
        viewModelScope.launch {
            medicationSyncDao.getAllMedicationsWithSchedules()
                .combine(medicationSyncDao.getAllReminderStates()) { medsWithSchedules, reminderStates ->
                    Log.d(TAG, "Observed ${medsWithSchedules.size} meds & ${reminderStates.size} states from Room.")
                    calculateRemindersFromSyncData(medsWithSchedules, reminderStates)
                }.collect { calculatedReminders ->
                    _reminders.value = calculatedReminders
                    if (_phoneAppStatus.value == PhoneAppStatus.INSTALLED_DATA_REQUESTED || _phoneAppStatus.value == PhoneAppStatus.CHECKING) {
                        _isLoading.value = false
                        _phoneAppStatus.value = if (calculatedReminders.isNotEmpty() || !medsWithSchedules.isNullOrEmpty()) PhoneAppStatus.INSTALLED_WITH_DATA else PhoneAppStatus.INSTALLED_NO_DATA
                    } else if (_phoneAppStatus.value != PhoneAppStatus.NOT_INSTALLED_ANDROID && _phoneAppStatus.value != PhoneAppStatus.UNKNOWN ) { // Removed NOT_INSTALLED_IOS
                         _phoneAppStatus.value = if (calculatedReminders.isNotEmpty() || !medsWithSchedules.isNullOrEmpty()) PhoneAppStatus.INSTALLED_WITH_DATA else PhoneAppStatus.INSTALLED_NO_DATA
                    }
                    Log.d(TAG, "Updated ViewModel reminders: ${calculatedReminders.size} items. Status: ${_phoneAppStatus.value}")
                }
        }
    }

    private fun calculateRemindersFromSyncData(
        medicationsWithSchedules: List<MedicationWithSchedulesPojo>,
        reminderStates: List<ReminderStateEntity>
    ): List<WearReminder> {
        val today = LocalDate.now()
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
                        gson.fromJson<List<String>>(json, typeToken).mapNotNull { runCatching { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() }
                    }.getOrNull()
                }
                val dailyRepetitionDays: List<String>? = scheduleEntity.dailyRepetitionDaysJson?.let { json ->
                     runCatching {
                        val typeToken = object : TypeToken<List<String>>() {}.type
                        gson.fromJson<List<String>>(json, typeToken)
                     }.getOrNull()
                }
                // Using scheduleEntity.scheduleId as a stand-in for underlyingReminderId if a more specific one isn't available.
                // This part is crucial and depends on how the phone app structures its reminder IDs for specific instances.
                val phoneGeneratedReminderIdForSchedule = scheduleEntity.scheduleId

                if (scheduleEntity.scheduleType == "DAILY_SPECIFIC_TIMES" || scheduleEntity.scheduleType == "CUSTOM_ALARMS") {
                    if (dailyRepetitionDays.isNullOrEmpty() || dailyRepetitionDays.contains(today.dayOfWeek.name.toString())) {
                        specificTimes?.forEach { time ->
                            val reminderTimeKey = time.format(DateTimeFormatter.ofPattern("HH:mm"))
                            val reminderInstanceId = "med${medication.medicationId}_sched${scheduleEntity.scheduleId}_day${today.toEpochDay()}_time${reminderTimeKey.replace(":", "")}"
                            val state: ReminderStateEntity? = reminderStates.find { it.reminderInstanceId == reminderInstanceId }
                            allCalculatedReminders.add(
                                WearReminder(
                                    id = reminderInstanceId,
                                    medicationId = medication.medicationId,
                                    scheduleId = scheduleEntity.scheduleId,
                                    underlyingReminderId = phoneGeneratedReminderIdForSchedule, // This should be the actual specific instance ID from phone if available
                                    medicationName = medication.name,
                                    dosage = medication.dosage,
                                    time = reminderTimeKey,
                                    isTaken = state?.isTaken ?: false,
                                    takenAt = state?.takenAt
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
                            val state: ReminderStateEntity? = reminderStates.find { it.reminderInstanceId == reminderInstanceId }
                            allCalculatedReminders.add(
                                WearReminder(
                                    id = reminderInstanceId,
                                    medicationId = medication.medicationId,
                                    scheduleId = scheduleEntity.scheduleId,
                                    underlyingReminderId = phoneGeneratedReminderIdForSchedule,
                                    medicationName = medication.name,
                                    dosage = medication.dosage,
                                    time = reminderTimeKey,
                                    isTaken = state?.isTaken ?: false,
                                    takenAt = state?.takenAt
                                )
                            )
                            val nextSlot = currentTimeSlot.plus(intervalDuration)
                            if (nextSlot.isBefore(currentTimeSlot) || nextSlot == currentTimeSlot) break
                            currentTimeSlot = nextSlot
                        }
                    }
                }
            }
        }
        return allCalculatedReminders.sortedWith(
            compareBy<WearReminder> { LocalTime.parse(it.time, DateTimeFormatter.ofPattern("HH:mm")) }
            .thenBy { it.isTaken }
        )
    }

    fun markReminderAsTakenOnWatch(reminder: WearReminder) {
        viewModelScope.launch {
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val newState = ReminderStateEntity(
                reminderInstanceId = reminder.id,
                medicationId = reminder.medicationId,
                scheduleId = reminder.scheduleId,
                reminderTimeKey = reminder.time,
                isTaken = true,
                takenAt = now
            )
            medicationSyncDao.insertOrUpdateReminderState(newState)
            Log.i(TAG, "Marked reminder ${reminder.id} as taken locally on watch.")

            // Use reminder.underlyingReminderId which should be the phone's DB ID for this specific instance
            if (reminder.underlyingReminderId != 0L) {
                markReminderAsTakenOnPhone(reminder.underlyingReminderId.toString(), Wearable.getMessageClient(getApplication()))
            } else {
                // This case is for when the watch generates a reminder instance not directly tied to a phone DB reminder ID
                val adhocTakenData = AdhocTakenPayload(reminder.medicationId, reminder.scheduleId, reminder.time, now)
                val jsonData = gson.toJson(adhocTakenData)
                Log.w(TAG, "No specific phone DB ID for ${reminder.id}. Sending ad-hoc taken message: $jsonData")
                sendMessageToPhone(ADHOC_TAKEN_PATH, jsonData.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private data class AdhocTakenPayload(val medicationId: Int, val scheduleId: Long, val reminderTimeKey: String, val takenAt: String)

    fun checkPhoneAppInstallation(
        capabilityClient: CapabilityClient,
        remoteActivityHelper: RemoteActivityHelper,
        messageClient: MessageClient
    ) {
        if (_phoneAppStatus.value == PhoneAppStatus.CHECKING && _isLoading.value) return
        _phoneAppStatus.value = PhoneAppStatus.CHECKING
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val capabilityInfo = capabilityClient
                    .getCapability(PHONE_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                    .await()
                val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }

                if (phoneNode != null) {
                    Log.i(TAG, "Phone app capability found: ${phoneNode.displayName}. Requesting initial sync.")
                    _phoneAppStatus.value = PhoneAppStatus.INSTALLED_DATA_REQUESTED
                    requestInitialSyncData(messageClient, phoneNode.id)
                } else {
                    Log.w(TAG, "Phone app capability '$PHONE_APP_CAPABILITY_NAME' not found.")
                    // Assume Android if capability not found, as per user feedback.
                    // No need to distinguish iOS for opening store if PhoneDeviceType is removed.
                    _phoneAppStatus.value = PhoneAppStatus.NOT_INSTALLED_ANDROID
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking phone app installation: ${e.message}", e)
                _phoneAppStatus.value = PhoneAppStatus.UNKNOWN
                _isLoading.value = false
            }
        }
        capabilityClient.removeListener(this, PHONE_APP_CAPABILITY_NAME)
        capabilityClient.addListener(this, PHONE_APP_CAPABILITY_NAME)
    }

    private fun requestInitialSyncData(messageClient: MessageClient, nodeId: String) {
        viewModelScope.launch {
            sendMessageToNode(messageClient, nodeId, REQUEST_INITIAL_SYNC_PATH, ByteArray(0))
                .addOnSuccessListener {
                    Log.i(TAG, "Initial sync request sent to node $nodeId. Status: ${_phoneAppStatus.value}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send initial sync request to node $nodeId", e)
                    if (_phoneAppStatus.value == PhoneAppStatus.INSTALLED_DATA_REQUESTED) {
                       _phoneAppStatus.value = PhoneAppStatus.INSTALLED_NO_DATA
                    }
                    _isLoading.value = false
                }
        }
    }

    private fun markReminderAsTakenOnPhone(phoneDbReminderId: String, messageClient: MessageClient) {
        viewModelScope.launch {
            sendMessageToPhone(MARK_AS_TAKEN_PATH, phoneDbReminderId.toByteArray(Charsets.UTF_8), messageClient)
        }
    }

    private suspend fun sendMessageToNode(messageClient: MessageClient, nodeId: String, path: String, data: ByteArray): com.google.android.gms.tasks.Task<Int> {
        return messageClient.sendMessage(nodeId, path, data)
    }

    private suspend fun sendMessageToPhone(path: String, data: ByteArray, specificMessageClient: MessageClient? = null): com.google.android.gms.tasks.Task<Int>? {
        val client = specificMessageClient ?: Wearable.getMessageClient(getApplication<Application>())
        try {
            val capabilityInfo = Wearable.getCapabilityClient(getApplication<Application>())
                .getCapability(PHONE_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                .await()
            val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }
            if (phoneNode != null) {
                return sendMessageToNode(client, phoneNode.id, path, data)
                    // .addOnSuccessListener { Log.i(TAG,"Message $path sent to ${phoneNode.displayName}") } // Logging done in sendMessageToNode
                    // .addOnFailureListener { e -> Log.e(TAG, "Failed to send message $path to ${phoneNode.displayName}", e) }
            } else {
                Log.w(TAG, "Cannot send message $path: No connected phone node with capability.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message $path", e)
        }
        return null
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged: ${capabilityInfo.name}, Nodes: ${capabilityInfo.nodes.joinToString { it.displayName }}")
        if (capabilityInfo.name == PHONE_APP_CAPABILITY_NAME) {
            val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }
            if (phoneNode != null) {
                if (_phoneAppStatus.value == PhoneAppStatus.NOT_INSTALLED_ANDROID ||
                    _phoneAppStatus.value == PhoneAppStatus.UNKNOWN || // Removed NOT_INSTALLED_IOS
                    _phoneAppStatus.value == PhoneAppStatus.CHECKING ||
                    _phoneAppStatus.value == PhoneAppStatus.INSTALLED_NO_DATA) {
                    Log.i(TAG, "Phone app connected/reconnected or was missing data: ${phoneNode.displayName}. Updating status and requesting initial sync.")
                    _phoneAppStatus.value = PhoneAppStatus.INSTALLED_DATA_REQUESTED
                    _isLoading.value = true
                    requestInitialSyncData(Wearable.getMessageClient(getApplication()), phoneNode.id)
                } else {
                     Log.i(TAG, "Phone app capability detected, status is already ${_phoneAppStatus.value}. No immediate sync action.")
                }
            } else {
                Log.w(TAG, "Phone app capability lost or no reachable node.")
                // Assume Android and not installed if capability is lost, as per user feedback
                _phoneAppStatus.value = PhoneAppStatus.NOT_INSTALLED_ANDROID
                _isLoading.value = false
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
                remoteActivityHelper.startRemoteActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW)
                        .addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                        .setData(android.net.Uri.parse(PLAY_STORE_APP_URI))
                ).await()
                Log.i(TAG, "Attempted to open Play Store on phone.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open Play Store on phone", e)
            }
        }
    }
}

enum class PhoneAppStatus {
    UNKNOWN,
    CHECKING,
    INSTALLED_WITH_DATA,
    INSTALLED_DATA_REQUESTED,
    INSTALLED_NO_DATA,
    NOT_INSTALLED_ANDROID,
    // NOT_INSTALLED_IOS // Removed as per user feedback
}
