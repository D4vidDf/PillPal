package com.d4viddf.medicationreminder.ui.features.synceddevices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.utils.BatteryStateHolder
import com.d4viddf.medicationreminder.utils.WearConnectivityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ConnectedDevicesViewModel @Inject constructor(
    private val wearConnectivityHelper: WearConnectivityHelper,
    private val batteryStateHolder: BatteryStateHolder
) : ViewModel() {

    data class UiState(
        val isRefreshing: Boolean = true,
        val isSyncing: Boolean = false,
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
        listenForBatteryUpdates()
        refreshDeviceStatus()
    }

    private fun listenForBatteryUpdates() {
        batteryStateHolder.batteryLevels
            .onEach { (nodeId, level) ->
                _uiState.update { currentState ->
                    val updatedDevices = currentState.connectedDevices.map { device ->
                        if (device.id == nodeId) {
                            device.copy(batteryPercent = level)
                        } else {
                            device
                        }
                    }
                    currentState.copy(connectedDevices = updatedDevices)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onDeviceClicked(deviceId: String) {
        _uiState.update { currentState ->
            val updatedDevices = currentState.connectedDevices.map { device ->
                if (device.id == deviceId) {
                    device.copy(isExpanded = !device.isExpanded)
                } else {
                    device.copy(isExpanded = false)
                }
            }
            currentState.copy(connectedDevices = updatedDevices)
        }
    }

    fun onInstallAppOnWatch(nodeId: String) {
        wearConnectivityHelper.openPlayStoreOnWatch(nodeId)
    }

    fun refreshDeviceStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(1000) // Artificial delay for UX

            val nodes = wearConnectivityHelper.getConnectedNodes()

            nodes.forEach { node ->
                wearConnectivityHelper.requestBatteryLevel(node.id)
            }

            val devices = nodes.map { node ->
                async {
                    DeviceInfo(
                        id = node.id,
                        name = node.displayName,
                        isAppInstalled = wearConnectivityHelper.isAppInstalledOnNode(node.id),
                        batteryPercent = -1
                    )
                }
            }.awaitAll()

            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    connectedDevices = devices,
                    lastSyncTimestamp = if (devices.isNotEmpty() && it.lastSyncTimestamp == null) Instant.now() else it.lastSyncTimestamp
                )
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            delay(1000) // Artificial delay for UX
            // TODO: Implement your actual data sync logic here
            _uiState.update { it.copy(lastSyncTimestamp = Instant.now(), isSyncing = false) }
        }
    }
}
