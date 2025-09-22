package com.d4viddf.medicationreminder.ui.features.settings

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.healthconnect.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HealthConnectSettingsUiState {
    object Loading : HealthConnectSettingsUiState()
    object NotAvailable : HealthConnectSettingsUiState()
    data class Available(
        val isConnected: Boolean,
        val permissions: Map<String, Boolean>
    ) : HealthConnectSettingsUiState()
}

sealed class HealthConnectEvent {
    object RequestPermissions : HealthConnectEvent()
    object Disconnect : HealthConnectEvent()
}

sealed class UiEvent {
    data class LaunchPermissionRequest(val permissions: Set<String>) : UiEvent()
    object OpenHealthConnectSettings : UiEvent()
}

@HiltViewModel
class HealthConnectSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = mutableStateOf<HealthConnectSettingsUiState>(HealthConnectSettingsUiState.Loading)
    val uiState: State<HealthConnectSettingsUiState> = _uiState

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        checkAvailability()
    }

    fun onEvent(event: HealthConnectEvent) {
        when (event) {
            is HealthConnectEvent.RequestPermissions -> {
                viewModelScope.launch {
                    if (healthConnectManager.hasAllPermissions()) {
                        _eventFlow.emit(UiEvent.OpenHealthConnectSettings)
                    } else {
                        _eventFlow.emit(UiEvent.LaunchPermissionRequest(healthConnectManager.getPermissions()))
                    }
                }
            }
            is HealthConnectEvent.Disconnect -> {
                disconnect()
            }
        }
    }

    private fun checkAvailability() {
        viewModelScope.launch {
            if (healthConnectManager.healthConnectCompatible.value) {
                updatePermissionStatus()
            } else {
                _uiState.value = HealthConnectSettingsUiState.NotAvailable
            }
        }
    }

    fun updatePermissionStatus() {
        viewModelScope.launch {
            val isConnected = healthConnectManager.hasAllPermissions()
            val permissions = healthConnectManager.getPermissions().associateWith { isConnected }
            _uiState.value = HealthConnectSettingsUiState.Available(isConnected, permissions)
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            healthConnectManager.revokeAllPermissions()
            updatePermissionStatus()
        }
    }

    fun openHealthConnectDataManagement(context: Context) {
        val intent = Intent(androidx.health.connect.client.HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        context.startActivity(intent)
    }

    fun openHealthConnectFaq(context: Context) {
        val uri = android.net.Uri.parse("https://support.google.com/android/answer/12201227?hl=en")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }
}
