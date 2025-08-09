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

    data class UiState(
        val isLoading: Boolean = true,
        val connectedDevices: List<DeviceInfo> = emptyList(),
        val lastSyncTimestamp: Instant? = null
    ) {
        val isDeviceConnected: Boolean get() = connectedDevices.isNotEmpty()
    }

    data class DeviceInfo(
        val id: String,
        val name: String,
        val batteryPercent: Int,
        val isAppInstalled: Boolean,
        val isExpanded: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshDeviceStatus()
    }

    fun onDeviceClicked(deviceId: String) {
        _uiState.update { currentState ->
            val updatedDevices = currentState.connectedDevices.map { device ->
                if (device.id == deviceId) {
                    device.copy(isExpanded = !device.isExpanded)
                } else {
                    device
                }
            }
            currentState.copy(connectedDevices = updatedDevices)
        }
    }

    fun refreshDeviceStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val isConnected = wearConnectivityHelper.isWatchConnected()

            if (isConnected) {
                // Simulate a list of devices. In a real app, you would get this from the connectivity helper.
                val device = DeviceInfo(
                    id = "galaxy_watch_6",
                    name = "Galaxy Watch6",
                    batteryPercent = 78,
                    isAppInstalled = wearConnectivityHelper.isWatchAppInstalled()
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        connectedDevices = listOf(device),
                        lastSyncTimestamp = it.lastSyncTimestamp ?: Instant.now()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, connectedDevices = emptyList())
                }
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            // TODO: Implement your actual data sync logic here
            _uiState.update { it.copy(lastSyncTimestamp = Instant.now()) }
        }
    }
}
