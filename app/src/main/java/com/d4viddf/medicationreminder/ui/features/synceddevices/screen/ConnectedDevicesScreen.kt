package com.d4viddf.medicationreminder.ui.features.synceddevices.screen

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.synceddevices.ConnectedDevicesViewModel
import com.d4viddf.medicationreminder.ui.theme.MedicationTheme
import java.time.Instant

@Composable
fun ConnectedDevicesScreen(
    navController: NavController,
    viewModel: ConnectedDevicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ConnectedDevicesScreenContent(
        uiState = uiState,
        onSyncData = viewModel::syncData,
        onRefreshList = viewModel::refreshDeviceStatus,
        onDeviceClicked = viewModel::onDeviceClicked,
        onInstallAppOnWatch = viewModel::onInstallAppOnWatch,
        onNavigateBack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedDevicesScreenContent(
    uiState: ConnectedDevicesViewModel.UiState,
    onSyncData: () -> Unit,
    onRefreshList: () -> Unit,
    onDeviceClicked: (String) -> Unit,
    onInstallAppOnWatch: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.connected_devices_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (uiState.isRefreshing && uiState.connectedDevices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.connectedDevices.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NoDeviceConnectedStatus(
                        onRefreshList = onRefreshList,
                        isRefreshing = uiState.isRefreshing
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.connectedDevices, key = { it.id }) { device ->
                        DeviceItem(
                            device = device,
                            onDeviceClicked = { onDeviceClicked(device.id) },
                            onInstallApp = { onInstallAppOnWatch(device.id) },
                            lastSyncTimestamp = uiState.lastSyncTimestamp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isDeviceConnected) {
                ActionButtons(
                    onSyncData = onSyncData,
                    onRefreshList = onRefreshList,
                    isRefreshing = uiState.isRefreshing,
                    isSyncing = uiState.isSyncing
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceItem(
    device: ConnectedDevicesViewModel.DeviceInfo,
    onDeviceClicked: () -> Unit,
    onInstallApp: () -> Unit,
    lastSyncTimestamp: Instant?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        onClick = onDeviceClicked
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_rounded_devices_wearables_24),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(R.string.device_status_connected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (device.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand or collapse device details"
                )
            }

            AnimatedVisibility(visible = device.isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(modifier = Modifier.padding(bottom = 8.dp))
                    if (device.batteryPercent != -1) {
                        InfoRow(
                            icon = Icons.Outlined.BatteryChargingFull,
                            label = stringResource(R.string.device_info_battery),
                            value = "${device.batteryPercent}%"
                        )
                    }
                    InfoRow(
                        icon = if (device.isAppInstalled) Icons.Default.CheckCircle else Icons.Default.Error,
                        iconTint = if (device.isAppInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        label = stringResource(R.string.device_info_app_status),
                        value = if (device.isAppInstalled) stringResource(R.string.app_status_installed) else stringResource(R.string.app_status_not_installed)
                    )
                    if (lastSyncTimestamp != null) {
                        InfoRow(
                            icon = Icons.Outlined.CloudSync,
                            label = stringResource(R.string.device_info_last_sync),
                            value = formatRelativeTime(lastSyncTimestamp.toEpochMilli())
                        )
                    }
                    if (!device.isAppInstalled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onInstallApp, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.install_on_watch_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoDeviceConnectedStatus(onRefreshList: () -> Unit, isRefreshing: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_rounded_devices_wearables_24),
            contentDescription = null,
            modifier = Modifier.size(128.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
        Button(onClick = onRefreshList, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize))
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.refresh_device_list_button))
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onSyncData: () -> Unit,
    onRefreshList: () -> Unit,
    isRefreshing: Boolean,
    isSyncing: Boolean
) {
    val isAnyLoading = isRefreshing || isSyncing
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSyncData,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnyLoading
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize))
            } else {
                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.sync_data_button))
            }
        }
        OutlinedButton(
            onClick = onRefreshList,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnyLoading
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize))
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.refresh_device_list_button))
            }
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

@Composable
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        now,
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

@Preview(name = "List with one item expanded", showBackground = true)
@Composable
fun ConnectedDevicesScreenPreview_List_Expanded() {
    val devices = listOf(
        ConnectedDevicesViewModel.DeviceInfo(
            id = "1",
            name = "Galaxy Watch6",
            batteryPercent = 78,
            isAppInstalled = true,
            isExpanded = true
        ),
        ConnectedDevicesViewModel.DeviceInfo(
            id = "2",
            name = "Pixel Watch",
            batteryPercent = 55,
            isAppInstalled = false,
            isExpanded = false
        )
    )
    val uiState = ConnectedDevicesViewModel.UiState(
        isLoading = false,
        connectedDevices = devices,
        lastSyncTimestamp = Instant.now().minusSeconds(300)
    )
    MedicationTheme {
        ConnectedDevicesScreenContent(
            uiState = uiState,
            onSyncData = {},
            onRefreshList = {},
            onDeviceClicked = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Disconnected State", showBackground = true)
@Composable
fun ConnectedDevicesScreenPreview_Disconnected() {
    val uiState = ConnectedDevicesViewModel.UiState(
        isLoading = false,
        connectedDevices = emptyList()
    )
    MedicationTheme {
        ConnectedDevicesScreenContent(
            uiState = uiState,
            onSyncData = {},
            onRefreshList = {},
            onDeviceClicked = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Loading State", showBackground = true)
@Composable
fun ConnectedDevicesScreenPreview_Loading() {
    val uiState = ConnectedDevicesViewModel.UiState(
        isLoading = true,
        connectedDevices = emptyList()
    )
    MedicationTheme {
        ConnectedDevicesScreenContent(
            uiState = uiState,
            onSyncData = {},
            onRefreshList = {},
            onDeviceClicked = {},
            onNavigateBack = {}
        )
    }
}
