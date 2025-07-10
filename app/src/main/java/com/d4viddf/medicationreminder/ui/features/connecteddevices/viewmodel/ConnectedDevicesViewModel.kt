package com.d4viddf.medicationreminder.ui.features.connecteddevices.viewmodel

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.utils.WearConnectivityHelper
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ConnectedDeviceUiItem(
    val id: String,
    val displayName: String,
    val isNearby: Boolean
)

data class ConnectedDevicesScreenState(
    val connectedDevices: List<ConnectedDeviceUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val showSyncSuccessMessage: Boolean = false,
    val showSyncErrorMessage: Boolean = false
)

@HiltViewModel
class ConnectedDevicesViewModel @Inject constructor(
    private val application: Application,
    private val wearConnectivityHelper: WearConnectivityHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectedDevicesScreenState())
    val uiState: StateFlow<ConnectedDevicesScreenState> = _uiState.asStateFlow()

    private val nodeClient = Wearable.getNodeClient(application)
    private val messageClient = Wearable.getMessageClient(application) // For sending sync trigger

    companion object {
        private const val TAG = "ConnDevViewModel"
        private const val REQUEST_MANUAL_SYNC_PATH = "/request_manual_sync" // Path to request sync from watch
    }

    init {
        fetchConnectedDevices()
    }

    fun fetchConnectedDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val nodes = nodeClient.connectedNodes.await()
                _uiState.value = _uiState.value.copy(
                    connectedDevices = nodes.map { ConnectedDeviceUiItem(it.id, it.displayName, it.isNearby) },
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching connected devices", e)
                _uiState.value = _uiState.value.copy(isLoading = false, connectedDevices = emptyList())
            }
        }
    }

    fun triggerSyncWithWatch() {
        viewModelScope.launch {
            // We need a node ID to send the message.
            // If multiple devices, decide which one to sync with (e.g., the first one, or one with capability)
            val targetNodeId = uiState.value.connectedDevices.firstOrNull { it.isNearby }?.id
            // Or, preferably, get the node with the app capability from WearConnectivityHelper if it's the phone app asking watch to sync
            // However, the plan is for the phone app to trigger its *own* sync process that then pushes to the watch.
            // So, we'll call the DataLayerListenerService's full sync method.
            // For now, let's assume this button triggers the phone to re-evaluate its data and push to the watch.
            // This could be done by sending a message to its own DataLayerListenerService or calling a repository method directly.

            // For now, let's re-use the concept of the DataLayerListenerService performing the sync
            // The phone app itself can trigger its DataLayerListenerService to perform a full sync.
            // This is a bit indirect. A more direct way would be to have a shared "SyncManager" or similar.
            // Let's simulate by invoking what the DataLayerListenerService would do.

            // The most straightforward way to re-trigger the phone's sync logic that pushes to the watch
            // is to call the same methods that DataLayerListenerService calls.
            // However, DataLayerListenerService is a service.
            // Let's simplify for now: this button will tell the *watch* to request data again.
            // This keeps the phone's sync logic centralized in DataLayerListenerService upon request.

            if (targetNodeId != null) {
                Log.i(TAG, "Requesting manual data sync from watch node: $targetNodeId")
                messageClient.sendMessage(targetNodeId, REQUEST_MANUAL_SYNC_PATH, null)
                    .addOnSuccessListener {
                        Log.i(TAG, "Manual sync request message sent successfully to $targetNodeId.")
                        _uiState.value = _uiState.value.copy(showSyncSuccessMessage = true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to send manual sync request message to $targetNodeId.", e)
                        _uiState.value = _uiState.value.copy(showSyncErrorMessage = true)
                    }
            } else {
                 Log.w(TAG, "No connected device found to trigger sync with.")
                _uiState.value = _uiState.value.copy(showSyncErrorMessage = true)
                // If no devices, perhaps offer to pair.
            }
        }
    }

    fun openPairingFlow() {
        val wearSetupIntent = Intent("com.google.android.gms.wearable.SETUP_WEARABLE_API")
        wearSetupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            application.startActivity(wearSetupIntent)
            Log.i(TAG, "Attempting to launch Wear OS setup.")
        } catch (e: Exception) {
            Log.w(TAG, "Wear OS setup intent failed, falling back to Bluetooth settings.", e)
            val bluetoothSettingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            bluetoothSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                application.startActivity(bluetoothSettingsIntent)
            } catch (settingsException: Exception) {
                Log.e(TAG, "Could not launch Bluetooth settings.", settingsException)
            }
        }
    }

    fun consumedSyncSuccessMessage() {
        _uiState.value = _uiState.value.copy(showSyncSuccessMessage = false)
    }

    fun consumedSyncErrorMessage() {
        _uiState.value = _uiState.value.copy(showSyncErrorMessage = false)
    }
}
