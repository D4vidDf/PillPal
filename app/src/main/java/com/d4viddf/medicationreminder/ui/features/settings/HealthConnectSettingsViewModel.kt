package com.d4viddf.medicationreminder.ui.features.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.healthconnect.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class HealthConnectSettingsViewModel @Inject constructor(
    val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = mutableStateOf<HealthConnectSettingsUiState>(HealthConnectSettingsUiState.Loading)
    val uiState: State<HealthConnectSettingsUiState> = _uiState

    init {
        checkAvailability()
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

    fun disconnect() {
        viewModelScope.launch {
            healthConnectManager.revokeAllPermissions()
            updatePermissionStatus()
        }
    }

    fun openHealthConnectDataManagement(context: android.content.Context) {
        val intent = android.content.Intent(androidx.health.connect.client.HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        context.startActivity(intent)
    }

    fun openHealthConnectFaq(context: android.content.Context) {
        val uri = android.net.Uri.parse("https://support.google.com/android/answer/12944888")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }
}
