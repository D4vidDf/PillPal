package com.d4viddf.medicationreminder.ui.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectSettingsScreen(
    navController: NavController,
    viewModel: HealthConnectSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState
    var showInfoCard by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        viewModel.healthConnectManager.requestPermissionsActivityContract()
    ) {
        scope.launch {
            viewModel.updatePermissionStatus()
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.LaunchPermissionRequest -> {
                    permissionLauncher.launch(event.permissions)
                }
                is UiEvent.OpenHealthConnectSettings -> {
                    viewModel.openHealthConnectDataManagement(context)
                }
            }
        }
    }

    if (uiState is HealthConnectSettingsUiState.Available && (uiState as HealthConnectSettingsUiState.Available).showDisconnectDialog) {
        DisconnectDialog(
            onConfirm = {
                viewModel.onEvent(HealthConnectEvent.OpenHealthConnectSettings)
                viewModel.onEvent(HealthConnectEvent.DismissDisconnectDialog)
            },
            onDismiss = { viewModel.onEvent(HealthConnectEvent.DismissDisconnectDialog) }
        )
    }

    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.health_connect_settings)) },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is HealthConnectSettingsUiState.Loading -> {
                    Text(text = stringResource(R.string.loading))
                }
                is HealthConnectSettingsUiState.NotAvailable -> {
                    Text(text = "Health Connect is not available on this device.")
                }
                is HealthConnectSettingsUiState.Available -> {
                    Image(
                        painter = painterResource(id = R.drawable.health_connect_logo),
                        contentDescription = "Health Connect Logo",
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (showInfoCard) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.padding(start = 16.dp)) {
                                    Text(
                                        text = stringResource(id = R.string.health_connect_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(id = R.string.health_connect_description),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showInfoCard = false }) {
                                            Text(text = stringResource(id = R.string.close))
                                        }
                                        TextButton(onClick = {
                                            viewModel.onEvent(HealthConnectEvent.RequestPermissions)
                                        }) {
                                            Text(text = stringResource(id = R.string.health_connect_review_permissions))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(id = R.string.health_connect_information_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.health_connect_information_description_1),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.health_connect_information_description_2),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(id = R.string.health_connect_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingItem(
                        icon = Icons.Default.WbSunny,
                        title = stringResource(id = R.string.health_connect_today_title),
                        description = stringResource(id = R.string.health_connect_today_description),
                        onClick = { navController.navigate("salud_conectada_en_hoy") }
                    )
                    Divider()
                    SettingItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(id = R.string.health_connect_manage_data_title),
                        description = stringResource(id = R.string.health_connect_manage_data_description),
                        onClick = { viewModel.openHealthConnectDataManagement(context) }
                    )
                    Divider()
                    SettingItem(
                        icon = Icons.Default.HelpOutline,
                        title = stringResource(id = R.string.health_connect_get_help_title),
                        description = stringResource(id = R.string.health_connect_get_help_description),
                        onClick = { viewModel.openHealthConnectFaq(context) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { viewModel.onEvent(HealthConnectEvent.ShowDisconnectDialog) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.health_connect_disconnect))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
fun DisconnectDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.health_connect_disconnect_dialog_title)) },
        text = { Text(text = stringResource(id = R.string.health_connect_disconnect_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(id = R.string.health_connect_disconnect_dialog_open_settings_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.health_connect_disconnect_dialog_go_back_button))
            }
        }
    )
}
