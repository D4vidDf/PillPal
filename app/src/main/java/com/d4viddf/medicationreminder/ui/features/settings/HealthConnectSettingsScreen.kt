package com.d4viddf.medicationreminder.ui.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectSettingsScreen(
    navController: NavController,
    viewModel: HealthConnectSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.health_connect_settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HealthConnectSettingsUiState.Loading -> {
                    Text(text = "Loading...")
                }
                is HealthConnectSettingsUiState.NotAvailable -> {
                    Text(text = "Health Connect is not available on this device.")
                }
                is HealthConnectSettingsUiState.Available -> {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val permissionLauncher = rememberLauncherForActivityResult(
                        viewModel.healthConnectManager.requestPermissionsActivityContract()
                    ) {
                        scope.launch {
                            viewModel.updatePermissionStatus()
                        }
                    }

                    Button(onClick = {
                        if (state.isConnected) {
                            viewModel.disconnect()
                        } else {
                            if (viewModel.healthConnectManager.isHealthConnectAppInstalled()) {
                                permissionLauncher.launch(viewModel.healthConnectManager.getPermissions())
                            } else {
                                Toast.makeText(context, "Please install the Health Connect app", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text(text = if (state.isConnected) "Disconnect" else "Connect")
                    }
                    Text(text = "Permissions:")
                    state.permissions.forEach { (permission, granted) ->
                        Text(text = "$permission: ${if (granted) "Granted" else "Denied"}")
                    }
                }
            }
        }
    }
}
