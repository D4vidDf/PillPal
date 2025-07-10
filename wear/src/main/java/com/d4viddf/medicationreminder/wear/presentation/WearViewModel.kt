package com.d4viddf.medicationreminder.wear.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.services.GlobalWearReminderRepository // Temporary
import com.d4viddf.medicationreminder.wear.services.WearableCommunicationService
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
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
    private val capabilityClient by lazy { Wearable.getCapabilityClient(application) }

    // Define a capability string that the phone app advertises
    private val PHONE_APP_CAPABILITY_NAME = "medication_reminder_phone_app"


    init {
        viewModelScope.launch {
            // Collect from the global repository (placeholder, replace with Hilt/proper DI)
            GlobalWearReminderRepository.reminders.collect { updatedReminders ->
                _reminders.value = updatedReminders
                Log.d("WearViewModel", "Collected and updated reminders: ${updatedReminders.size} items")
            }
        }
        checkConnectionStatus()
        listenForConnectionChanges()
    }

    fun markReminderAsTaken(reminderId: Long) {
        if (reminderId != 0L) {
            communicationService.sendMarkAsTakenMessage(reminderId.toInt()) // Phone service expects Int
            // Optimistically update UI or wait for data sync
            // For now, we rely on the phone to send back the updated list
            Log.i("WearViewModel", "Requested to mark reminder $reminderId as taken.")
        } else {
            Log.w("WearViewModel", "Cannot mark reminder as taken: Invalid reminderId (0).")
        }
    }

    private fun checkConnectionStatus() {
        viewModelScope.launch {
            try {
                val connectedNodes: List<Node> = nodeClient.connectedNodes.await()
                _isConnectedToPhone.value = connectedNodes.any { it.isNearby } // Or just connectedNodes.isNotEmpty()
                Log.d("WearViewModel", "Initial connection status: ${_isConnectedToPhone.value}, Nodes: ${connectedNodes.joinToString { it.displayName }}")
            } catch (e: Exception) {
                Log.e("WearViewModel", "Error checking connection status", e)
                _isConnectedToPhone.value = false
            }
        }
    }

    private fun listenForConnectionChanges() {
        capabilityClient.addListener(this, PHONE_APP_CAPABILITY_NAME)

        // Initial check via capability
        viewModelScope.launch {
            try {
                val capabilityInfo = capabilityClient.getCapability(
                    PHONE_APP_CAPABILITY_NAME,
                    CapabilityClient.FILTER_REACHABLE
                ).await()
                _isConnectedToPhone.value = capabilityInfo.nodes.any { it.isNearby }
                Log.d("WearViewModel", "Initial capability check: ${capabilityInfo.name}, Connected: ${_isConnectedToPhone.value}")
            } catch (e: Exception) {
                Log.e("WearViewModel", "Error fetching initial capability", e)
                 // Fallback to node check if capability fails initially
                checkConnectionStatus()
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        _isConnectedToPhone.value = capabilityInfo.nodes.any { it.isNearby }
        Log.d("WearViewModel", "Capability changed: ${capabilityInfo.name}, Connected: ${_isConnectedToPhone.value}, Nodes: ${capabilityInfo.nodes.joinToString { it.displayName }}")
    }

    // Call this when the ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        capabilityClient.removeListener(this, PHONE_APP_CAPABILITY_NAME)
        Log.d("WearViewModel", "ViewModel cleared, capability listener removed.")
    }
}
