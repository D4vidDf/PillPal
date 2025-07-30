package com.d4viddf.medicationreminder.ui.features.synceddevices.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.synceddevices.viewmodel.ConnectedDevicesScreenState
import com.d4viddf.medicationreminder.ui.features.synceddevices.viewmodel.ConnectedDevicesViewModel
import com.d4viddf.medicationreminder.ui.features.synceddevices.viewmodel.ConnectedDeviceUiItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedDevicesScreen(
    navController: NavController,
    viewModel: ConnectedDevicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = uiState.showSyncSuccessMessage) {
        if (uiState.showSyncSuccessMessage) {
            snackbarHostState.showSnackbar(
                message = "Sync request sent to watch.",
                duration = SnackbarDuration.Short
            )
            viewModel.consumedSyncSuccessMessage()
        }
    }
    LaunchedEffect(key1 = uiState.showSyncErrorMessage) {
        if (uiState.showSyncErrorMessage) {
            snackbarHostState.showSnackbar(
                message = "Failed to send sync request.",
                duration = SnackbarDuration.Short
            )
            viewModel.consumedSyncErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Connected Devices & Sync") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button_cd))
                    }
                }
            )
        }
    ) { paddingValues ->
        ConnectedDevicesScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onRefreshDevices = { viewModel.fetchConnectedDevices() },
            onSyncData = { viewModel.triggerSyncWithWatch() },
            onPairDevice = { viewModel.openPairingFlow() },
            onDownloadApp = { nodeId -> viewModel.openPlayStoreOnWatch(nodeId) } // Pass action to ViewModel
        )
    }
}

@Composable
fun ConnectedDevicesScreenContent(
    modifier: Modifier = Modifier,
    uiState: ConnectedDevicesScreenState,
    onRefreshDevices: () -> Unit,
    onSyncData: () -> Unit,
            onPairDevice: () -> Unit,
            onDownloadApp: (String) -> Unit // Added callback for downloading app
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 20.dp))
        } else {
            if (uiState.connectedDevices.isEmpty()) {
                NoDevicesConnectedView(onPairDevice = onPairDevice, onRefreshDevices = onRefreshDevices)
            } else {
                Text(
                    text = "Connected Wear OS Devices:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.connectedDevices, key = { it.id }) { device ->
                        DeviceItem(device = device, onDownloadApp = { onDownloadApp(device.id) })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSyncData,
            enabled = uiState.connectedDevices.any { it.isNearby && it.isAppInstalled } && !uiState.isLoading // Sync enabled if app installed
        ) {
            Icon(Icons.Filled.Sync, contentDescription = "Sync Icon", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Sync Data with Watch")
        }

        OutlinedButton(onClick = onRefreshDevices, modifier = Modifier.padding(top = 8.dp)) {
            Text("Refresh Device List")
        }
    }
}

@Composable
fun DeviceItem(device: ConnectedDeviceUiItem, onDownloadApp: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Watch, contentDescription = "Watch device", modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = device.displayName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (device.isNearby) "Nearby" else "Not nearby",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!device.isAppInstalled) {
                        Text(
                            text = "App not installed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            if (!device.isAppInstalled && device.isNearby) {
                Button(onClick = onDownloadApp) {
                    Text("Download App")
                }
            }
        }
    }
}

@Composable
fun NoDevicesConnectedView(onPairDevice: () -> Unit, onRefreshDevices: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "No devices connected",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No Wear OS devices connected.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Text(
            text = "Please ensure your watch is paired with your phone and Bluetooth is enabled.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(onClick = onPairDevice) {
            Text("Pair a New Watch")
        }
         OutlinedButton(onClick = onRefreshDevices, modifier = Modifier.padding(top = 8.dp)) {
            Text("Refresh List")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectedDevicesScreenContentPreview_NoDevices() {
    MaterialTheme {
        ConnectedDevicesScreenContent(
            uiState = ConnectedDevicesScreenState(connectedDevices = emptyList(), isLoading = false),
            onRefreshDevices = {},
            onSyncData = {},
            onPairDevice = {},
            onDownloadApp = {} // Added
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectedDevicesScreenContentPreview_WithDevices() {
    MaterialTheme {
        ConnectedDevicesScreenContent(
            uiState = ConnectedDevicesScreenState(
                connectedDevices = listOf(
                    ConnectedDeviceUiItem("node1", "Pixel Watch", true, isAppInstalled = true), // Added isAppInstalled
                    ConnectedDeviceUiItem("node2", "Galaxy Watch", false, isAppInstalled = false) // Added isAppInstalled
                ),
                isLoading = false
            ),
            onRefreshDevices = {},
            onSyncData = {},
            onPairDevice = {},
            onDownloadApp = {} // Added
        )
    }
}
