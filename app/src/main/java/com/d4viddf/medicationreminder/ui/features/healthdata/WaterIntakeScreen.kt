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
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import com.d4viddf.medicationreminder.ui.features.medication.graph.component.BarChartItem
import com.d4viddf.medicationreminder.ui.features.medication.graph.component.SimpleBarChart
import com.d4viddf.medicationreminder.ui.navigation.Screen
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WaterIntakeScreen(
    navController: NavController,
    viewModel: WaterIntakeViewModel = hiltViewModel()
) {
    val waterIntakeRecords by viewModel.waterIntakeRecords.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("d/M H:m").withZone(ZoneId.systemDefault())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.LogWater.route) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_water_intake_record))
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

            val chartData = waterIntakeRecords.map {
                BarChartItem(
                    label = DateTimeFormatter.ofPattern("d/M").withZone(ZoneId.systemDefault()).format(it.time),
                    value = it.volumeMilliliters.toFloat()
                )
            }

            SimpleBarChart(
                data = chartData,
                highlightedBarColor = MaterialTheme.colorScheme.primary,
                normalBarColor = MaterialTheme.colorScheme.secondaryContainer,
                labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                valueTextColor = MaterialTheme.colorScheme.onSurface,
                chartContentDescription = "Water intake chart"
            )

            LazyColumn {
                items(waterIntakeRecords) { record ->
                    Text(
                        text = "Water: ${record.volumeMilliliters} ml at ${formatter.format(record.time)}",
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
