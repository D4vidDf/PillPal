package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.ui.features.healthdata.component.DateRangeSelector
import com.d4viddf.medicationreminder.ui.features.healthdata.component.HealthChart
import com.d4viddf.medicationreminder.ui.features.healthdata.util.ChartType
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import com.d4viddf.medicationreminder.ui.navigation.Screen
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyTemperatureScreen(
    navController: NavController,
    viewModel: BodyTemperatureViewModel = hiltViewModel()
) {
    val aggregatedBodyTemperatureRecords by viewModel.aggregatedBodyTemperatureRecords.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val dateRangeText by viewModel.dateRangeText.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val endTime by viewModel.endTime.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("d/M H:m").withZone(ZoneId.systemDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.body_temperature_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_arrow_back_ios_24),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.LogTemperature.route) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(text = stringResource(id = R.string.log_temperature)) }
            )
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
            DateRangeSelector(
                dateRange = dateRangeText,
                onPreviousClick = viewModel::onPreviousClick,
                onNextClick = viewModel::onNextClick,
                onDateRangeClick = { /* No-op */ }
            )

            HealthChart(
                data = aggregatedBodyTemperatureRecords,
                chartType = ChartType.POINT,
                timeRange = timeRange,
                startTime = startTime,
                endTime = endTime,
                yAxisRange = 35.0..40.0
            )

            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(aggregatedBodyTemperatureRecords) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                val newTimeRange = when (timeRange) {
                                    TimeRange.YEAR -> TimeRange.MONTH
                                    TimeRange.MONTH -> TimeRange.WEEK
                                    TimeRange.WEEK -> TimeRange.DAY
                                    else -> null
                                }
                                if (newTimeRange != null) {
                                    viewModel.onHistoryItemClick(newTimeRange, record.first.atZone(ZoneId.systemDefault()).toLocalDate())
                                }
                            }
                    ) {
                        Text(
                            text = "Temperature: ${record.second} °C at ${formatter.format(record.first)}",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.health_data_disclaimer),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun getMeasurementSite(location: Int?): String {
    return when (location) {
        BodyTemperature.LOCATION_ARMPIT -> "Armpit"
        BodyTemperature.LOCATION_EAR -> "Ear"
        BodyTemperature.LOCATION_FINGER -> "Finger"
        BodyTemperature.LOCATION_FOREHEAD -> "Forehead"
        BodyTemperature.LOCATION_MOUTH -> "Mouth"
        BodyTemperature.LOCATION_RECTUM -> "Rectum"
        BodyTemperature.LOCATION_TEMPORAL_ARTERY -> "Temporal Artery"
        BodyTemperature.LOCATION_TOE -> "Toe"
        BodyTemperature.LOCATION_WRIST -> "Wrist"
        else -> "Unknown"
    }
}
