package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateScreen(
    navController: NavController,
    viewModel: HeartRateViewModel = hiltViewModel(),
    healthDataViewModel: HealthDataViewModel = hiltViewModel()
) {
    val heartRateData by viewModel.heartRateData.collectAsState()

    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        healthDataViewModel.healthConnectManager.requestPermissionsActivityContract()
    ) { }

    LaunchedEffect(Unit) {
        scope.launch {
            if (!healthDataViewModel.healthConnectManager.hasAllPermissions()) {
                permissionLauncher.launch(healthDataViewModel.healthConnectManager.getPermissions())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.heart_rate_tracker)) },
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
            if (heartRateData.isEmpty()) {
                Text(text = stringResource(R.string.no_heart_rate_data))
            } else {
                LazyColumn {
                    items(heartRateData) { record ->
                        Text(text = "BPM: ${record.beatsPerMinute}, Time: ${record.time}")
                    }
                }
            }
        }
    }
}
