package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.healthdata.component.LineChart
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import com.d4viddf.medicationreminder.ui.navigation.Screen
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BodyTemperatureScreen(
    navController: NavController,
    viewModel: BodyTemperatureViewModel = hiltViewModel()
) {
    val bodyTemperatureRecords by viewModel.bodyTemperatureRecords.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("d/M H:m").withZone(ZoneId.systemDefault())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.LogTemperature.route) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_body_temperature_record))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = timeRange.ordinal) {
                TimeRange.values().forEach { range ->
                    Tab(
                        selected = timeRange == range,
                        onClick = { viewModel.setTimeRange(range) },
                        text = { Text(range.name) }
                    )
                }
            }

            LineChart(data = bodyTemperatureRecords.map { it.time to it.temperatureCelsius })

            LazyColumn {
                items(bodyTemperatureRecords) { record ->
                    Text(
                        text = "Temperature: ${record.temperatureCelsius} °C at ${formatter.format(record.time)}",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.health_data_disclaimer),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
