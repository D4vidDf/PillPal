package com.d4viddf.medicationreminder.ui.features.synceddevices.screen

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.synceddevices.ConnectedDevicesViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedDevicesScreen(
    navController: NavController,
    viewModel: ConnectedDevicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.connected_devices_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Main Status Section ---
                    if (uiState.isDeviceConnected) {
                        ConnectedDeviceStatus(
                            deviceInfo = uiState.connectedDevice!!,
                            lastSyncTimestamp = uiState.lastSyncTimestamp
                        )
                    } else {
                        NoDeviceConnectedStatus()
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- Action Buttons Section ---
                    // These buttons will only appear if a device is connected
                    AnimatedVisibility(
                        visible = uiState.isDeviceConnected,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ActionButtons(
                            onSyncData = viewModel::syncData,
                            onRefreshList = viewModel::refreshDeviceStatus
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedDeviceStatus(
    deviceInfo: ConnectedDevicesViewModel.DeviceInfo,
    lastSyncTimestamp: Instant?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_rounded_devices_wearables_24), // A larger, more prominent watch icon
            contentDescription = null,
            modifier = Modifier.size(128.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = deviceInfo.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.device_status_connected),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF008000) // A nice green color for "Connected"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Detailed Info List ---
        InfoRow(
            icon = Icons.Outlined.BatteryChargingFull,
            label = stringResource(R.string.device_info_battery),
            value = "${deviceInfo.batteryPercent}%"
        )
        InfoRow(
            icon = if (deviceInfo.isAppInstalled) Icons.Default.CheckCircle else Icons.Default.Error,
            iconTint = if (deviceInfo.isAppInstalled) Color(0xFF008000) else MaterialTheme.colorScheme.error,
            label = stringResource(R.string.device_info_app_status),
            value = if (deviceInfo.isAppInstalled) stringResource(R.string.app_status_installed) else stringResource(R.string.app_status_not_installed)
        )
        if (lastSyncTimestamp != null) {
            InfoRow(
                icon = Icons.Outlined.CloudSync,
                label = stringResource(R.string.device_info_last_sync),
                value = formatRelativeTime(lastSyncTimestamp.toEpochMilli())
            )
        }
    }
}

@Composable
private fun NoDeviceConnectedStatus() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_rounded_devices_wearables_24), // A dedicated icon for disconnected state
            contentDescription = null,
            modifier = Modifier.size(128.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        )
        Text(
            text = stringResource(R.string.no_device_found_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.no_device_found_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO: Trigger a new device search */ }) {
            Text(stringResource(R.string.search_for_devices_button))
        }
    }
}

@Composable
private fun ActionButtons(onSyncData: () -> Unit, onRefreshList: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSyncData,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.sync_data_button))
        }
        OutlinedButton(
            onClick = onRefreshList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.refresh_device_list_button))
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color = LocalContentColor.current
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * A helper function to format timestamps into a relative string like "5 minutes ago".
 */
@Composable
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        now,
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}
