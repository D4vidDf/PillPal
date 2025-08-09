package com.d4viddf.medicationreminder.ui.features.synceddevices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.utils.WearConnectivityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ConnectedDevicesViewModel @Inject constructor(
    private val wearConnectivityHelper: WearConnectivityHelper
) : ViewModel() {

    // A single state object to hold all UI-related data
    data class UiState(
        val isLoading: Boolean = true,
        val connectedDevice: DeviceInfo? = null,
        val lastSyncTimestamp: Instant? = null
    ) {
        val isDeviceConnected: Boolean get() = connectedDevice != null
    }

    data class DeviceInfo(
        val name: String,
        val batteryPercent: Int,
        val isAppInstalled: Boolean
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshDeviceStatus()
    }

    fun refreshDeviceStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Simulate fetching device info
            // In a real app, these would be your actual wearConnectivityHelper calls
            val isConnected = wearConnectivityHelper.isWatchConnected()

            if (isConnected) {
                val deviceInfo = DeviceInfo(
                    name = "Galaxy Watch6", // Replace with actual device name
                    batteryPercent = 78, // Replace with actual battery level
                    isAppInstalled = wearConnectivityHelper.isWatchAppInstalled()
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        connectedDevice = deviceInfo,
                        // Set the last sync time if it's the first time connecting in this session
                        lastSyncTimestamp = it.lastSyncTimestamp ?: Instant.now()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, connectedDevice = null)
                }
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            // TODO: Implement your actual data sync logic here
            // On successful sync, update the timestamp
            _uiState.update { it.copy(lastSyncTimestamp = Instant.now()) }
        }
    }
}
