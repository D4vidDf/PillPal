package com.d4viddf.medicationreminder.ui.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaludConectadaEnHoyScreen(
    navController: NavController,
    viewModel: SaludConectadaEnHoyViewModel = hiltViewModel()
) {
    val showHealthConnectData by viewModel.showHealthConnectData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.health_connect_today_title)) },
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
        ) {
            OptionCard(
                title = stringResource(id = R.string.health_connect_today_show_all_data_title),
                description = stringResource(id = R.string.health_connect_today_show_all_data_description),
                selected = showHealthConnectData,
                onClick = { viewModel.onShowHealthConnectDataChange(true) }
            )
            OptionCard(
                title = stringResource(id = R.string.health_connect_today_dont_show_data_title),
                description = stringResource(id = R.string.health_connect_today_dont_show_data_description),
                selected = !showHealthConnectData,
                onClick = { viewModel.onShowHealthConnectDataChange(false) }
            )
        }
    }
}

@Composable
fun OptionCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = title)
                Text(text = description)
            }
        }
    }
}
