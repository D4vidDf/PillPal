package com.d4viddf.medicationreminder.ui.features.synceddevices.viewmodel

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.utils.WearConnectivityHelper
import com.d4viddf.medicationreminder.utils.constants.IntentActionConstants
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
    val isNearby: Boolean,
    val isAppInstalled: Boolean // Added app installed status
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
                val deviceItems = nodes.map { node ->
                    // For each node, check if the app is installed
                    val isAppInstalled = wearConnectivityHelper.isAppInstalledOnNode(node.id) // Use new method
                    ConnectedDeviceUiItem(
                        id = node.id,
                        displayName = node.displayName,
                        isNearby = node.isNearby,
                        isAppInstalled = isAppInstalled
                    )
                }
                _uiState.value = _uiState.value.copy(
                    connectedDevices = deviceItems,
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
            try {
                val intent = Intent(IntentActionConstants.ACTION_DATA_CHANGED)
                application.sendBroadcast(intent)
                _uiState.value = _uiState.value.copy(showSyncSuccessMessage = true)
                Log.i(TAG, "Manual sync broadcast sent.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send manual sync broadcast.", e)
                _uiState.value = _uiState.value.copy(showSyncErrorMessage = true)
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

    fun openPlayStoreOnWatch(nodeId: String) {
        viewModelScope.launch {
            wearConnectivityHelper.openPlayStoreOnWatch(nodeId)
        }
    }
}
