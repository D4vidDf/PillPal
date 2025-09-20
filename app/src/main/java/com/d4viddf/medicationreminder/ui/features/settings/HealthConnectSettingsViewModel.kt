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
    private val healthConnectManager: HealthConnectManager
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
}
