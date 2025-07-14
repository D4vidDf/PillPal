package com.d4viddf.medicationreminder.wear.presentation

import android.app.Application
import android.util.Log
import androidx.concurrent.futures.await
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.data.WearRepository
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // For GMS Tasks
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import androidx.wear.remote.interactions.RemoteActivityHelper
// No import for asListenableFuture needed; .await() is an extension from concurrent-futures-ktx

@HiltViewModel
class WearViewModel @Inject constructor(
    application: Application,
    private val wearRepository: WearRepository
) : AndroidViewModel(application), CapabilityClient.OnCapabilityChangedListener {

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
    private val capabilityClient: CapabilityClient by lazy { Wearable.getCapabilityClient(getApplication<Application>()) }

    private val PHONE_APP_CAPABILITY_NAME = "medication_reminder_phone_app"

    companion object {
        private const val TAG = "WearViewModel"
        private const val REQUEST_INITIAL_SYNC_PATH = "/request_initial_sync"
        private const val MARK_AS_TAKEN_PATH = "/mark_as_taken"
        private const val ADHOC_TAKEN_PATH = "/mark_adhoc_taken_on_watch"
        private const val PLAY_STORE_APP_URI = "market://details?id=com.d4viddf.medicationreminder"
    }

    init {
        observeMedicationAndReminderStates()
        registerCapabilityListener()
        checkPhoneAppInstallationInternal()
    }

    private fun registerCapabilityListener() {
        capabilityClient.addListener(this, PHONE_APP_CAPABILITY_NAME)
        Log.d(TAG, "Capability listener registered.")
    }

    private fun observeMedicationAndReminderStates() {
        viewModelScope.launch {
            medicationSyncDao.getAllMedicationsWithSchedules()
                .combine(medicationSyncDao.getAllReminderStates()) { medsFromDaoLambda, statesFromDaoLambda ->
                    Log.d(TAG, "Observed ${medsFromDaoLambda.size} meds & ${statesFromDaoLambda.size} states from Room.")
                    calculateRemindersFromSyncData(medsFromDaoLambda, statesFromDaoLambda)
                }.collect { calculatedReminders ->
                    _reminders.value = calculatedReminders
                    val currentStatus = _phoneAppStatus.value

                    // This logic needs to correctly reflect the source data.
                    // The `combine` means we get the latest from both flows.
                    // We need to check the `medsFromDaoLambda` that was used for the current `calculatedReminders`.
                    // This is tricky because `collect` only sees the result.
                    // A more robust way is to pass the source list size or emptiness along with calculatedReminders.
                    // For now, we'll assume if INSTALLED_DATA_REQUESTED, the status updates based on calculatedReminders.

                    if (currentStatus == PhoneAppStatus.INSTALLED_DATA_REQUESTED || currentStatus == PhoneAppStatus.CHECKING) {
                        _isLoading.value = false
                        // If we were waiting for data, and we got an empty list of *calculated* reminders,
                        // it could be because there are no meds defined, or no meds for *today*.
                        // To distinguish "no meds configured" from "no meds for today", we'd need the source list (medsFromDaoLambda).
                        // For now, if calculated is empty, we'll show INSTALLED_NO_DATA.
                        _phoneAppStatus.value = if (calculatedReminders.isNotEmpty()) PhoneAppStatus.INSTALLED_WITH_DATA else PhoneAppStatus.INSTALLED_NO_DATA
                    } else if (currentStatus == PhoneAppStatus.INSTALLED_WITH_DATA && calculatedReminders.isEmpty()) {
                         _phoneAppStatus.value = PhoneAppStatus.INSTALLED_NO_DATA
                    }
                    // Avoid changing status if it's already NOT_INSTALLED or UNKNOWN from just a data update
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
                val phoneGeneratedReminderIdForSchedule = scheduleEntity.scheduleId

                if (scheduleEntity.scheduleType == "DAILY_SPECIFIC_TIMES" || scheduleEntity.scheduleType == "CUSTOM_ALARMS") {
                    if (dailyRepetitionDays.isNullOrEmpty() || dailyRepetitionDays.contains(today.dayOfWeek.name)) {
                        specificTimes?.forEach { time ->
                            val reminderTimeKey = time.format(DateTimeFormatter.ofPattern("HH:mm"))
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

            // Extract the reminder ID from the composite key
            val reminderId = reminder.id.split("_").getOrNull(0)?.replace("med", "")?.toIntOrNull()

            if (reminderId != null) {
                wearRepository.sendReminderTakenMessage(reminderId)
            } else {
                val adhocTakenData = AdhocTakenPayload(reminder.medicationId, reminder.scheduleId, reminder.time, now)
                val jsonData = gson.toJson(adhocTakenData)
                Log.w(TAG, "No specific phone DB ID for ${reminder.id}. Sending ad-hoc taken message: $jsonData")
                sendMessageToPhone(ADHOC_TAKEN_PATH, jsonData.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private data class AdhocTakenPayload(val medicationId: Int, val scheduleId: Long, val reminderTimeKey: String, val takenAt: String)

    private fun checkPhoneAppInstallationInternal() {
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
                    Log.i(TAG, "Phone app capability found during initial check: ${phoneNode.displayName}.")
                    _phoneAppStatus.value = PhoneAppStatus.INSTALLED_DATA_REQUESTED
                    requestInitialSyncData(Wearable.getMessageClient(getApplication()), phoneNode.id)
                } else {
                    Log.w(TAG, "Phone app capability '$PHONE_APP_CAPABILITY_NAME' not found during initial check.")
                    _phoneAppStatus.value = PhoneAppStatus.NOT_INSTALLED_ANDROID
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during initial check for phone app installation: ${e.message}", e)
                _phoneAppStatus.value = PhoneAppStatus.UNKNOWN
                _isLoading.value = false
            }
        }
    }

    fun triggerPhoneAppCheckAndSync(remoteActivityHelper: RemoteActivityHelper, messageClient: MessageClient) {
         checkPhoneAppInstallation(this.capabilityClient, remoteActivityHelper, messageClient)
    }

    private fun checkPhoneAppInstallation(
        capClient: CapabilityClient,
        remoteHelper: RemoteActivityHelper,
        msgClient: MessageClient
    ) {
        if (_phoneAppStatus.value == PhoneAppStatus.CHECKING && _isLoading.value) return
        _phoneAppStatus.value = PhoneAppStatus.CHECKING
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val capabilityInfo = capClient
                    .getCapability(PHONE_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                    .await()
                val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }

                if (phoneNode != null) {
                    Log.i(TAG, "Phone app capability found: ${phoneNode.displayName}. Requesting initial sync.")
                    _phoneAppStatus.value = PhoneAppStatus.INSTALLED_DATA_REQUESTED
                    requestInitialSyncData(msgClient, phoneNode.id)
                } else {
                    Log.w(TAG, "Phone app capability '$PHONE_APP_CAPABILITY_NAME' not found.")
                    _phoneAppStatus.value = PhoneAppStatus.NOT_INSTALLED_ANDROID
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking phone app installation: ${e.message}", e)
                _phoneAppStatus.value = PhoneAppStatus.UNKNOWN
                _isLoading.value = false
            }
        }
    }

    private fun requestInitialSyncData(messageClient: MessageClient, nodeId: String) {
        _isLoading.value = true
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

    private fun markReminderAsTakenOnPhone(phoneDbReminderId: String, specificMessageClient: MessageClient? = null) {
        viewModelScope.launch {
            sendMessageToPhone(MARK_AS_TAKEN_PATH, phoneDbReminderId.toByteArray(Charsets.UTF_8), specificMessageClient)
        }
    }

    private suspend fun sendMessageToNode(messageClient: MessageClient, nodeId: String, path: String, data: ByteArray): com.google.android.gms.tasks.Task<Int> {
        return messageClient.sendMessage(nodeId, path, data)
    }

    private suspend fun sendMessageToPhone(path: String, data: ByteArray, specificMessageClient: MessageClient? = null): com.google.android.gms.tasks.Task<Int>? {
        val client = specificMessageClient ?: Wearable.getMessageClient(getApplication<Application>())
        try {
            val capabilityInfo = capabilityClient
                .getCapability(PHONE_APP_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                .await()
            val phoneNode = capabilityInfo.nodes.firstOrNull { it.isNearby }
            if (phoneNode != null) {
                return sendMessageToNode(client, phoneNode.id, path, data)
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
                Log.i(TAG, "Phone app capability detected on node: ${phoneNode.displayName}.")
                if (_phoneAppStatus.value == PhoneAppStatus.NOT_INSTALLED_ANDROID ||
                    _phoneAppStatus.value == PhoneAppStatus.UNKNOWN ||
                    _phoneAppStatus.value == PhoneAppStatus.CHECKING ||
                    _phoneAppStatus.value == PhoneAppStatus.INSTALLED_NO_DATA) {
                    Log.i(TAG, "Updating status to INSTALLED_DATA_REQUESTED and requesting initial sync.")
                    _phoneAppStatus.value = PhoneAppStatus.INSTALLED_DATA_REQUESTED
                    _isLoading.value = true
                    requestInitialSyncData(Wearable.getMessageClient(getApplication()), phoneNode.id)
                } else {
                     Log.i(TAG, "Phone app capability present, current status is ${_phoneAppStatus.value}. No immediate sync action unless data was missing.")
                }
            } else {
                Log.w(TAG, "Phone app capability lost or no reachable node.")
                _phoneAppStatus.value = PhoneAppStatus.NOT_INSTALLED_ANDROID
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        capabilityClient.removeListener(this, PHONE_APP_CAPABILITY_NAME)
        Log.d(TAG, "ViewModel cleared, capability listener removed.")
    }

    fun openPlayStoreOnPhone(remoteActivityHelper: RemoteActivityHelper) {
        viewModelScope.launch {
            try {
                remoteActivityHelper.startRemoteActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW)
                        .addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                        .setData(android.net.Uri.parse(PLAY_STORE_APP_URI))
                ).await() // Corrected: direct await on ListenableFuture
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
}
