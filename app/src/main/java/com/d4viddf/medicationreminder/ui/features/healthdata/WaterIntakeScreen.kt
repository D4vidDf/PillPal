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
import com.d4viddf.medicationreminder.ui.features.healthdata.component.DateRangeSelector
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import com.d4viddf.medicationreminder.ui.features.medication.graph.component.BarChartItem
import com.d4viddf.medicationreminder.ui.features.medication.graph.component.SimpleBarChart
import com.d4viddf.medicationreminder.ui.navigation.Screen
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterIntakeScreen(
    navController: NavController,
    viewModel: WaterIntakeViewModel = hiltViewModel()
) {
    val waterIntakeRecords by viewModel.waterIntakeRecords.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val dateRangeText by viewModel.dateRangeText.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("d/M H:m").withZone(ZoneId.systemDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.water_intake_screen_title)) },
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
            DateRangeSelector(
                dateRange = dateRangeText,
                onPreviousClick = viewModel::onPreviousClick,
                onNextClick = viewModel::onNextClick,
                onDateRangeClick = { /* No-op */ }
            )

            val chartData = waterIntakeRecords.map {
                BarChartItem(
                    label = it.time.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d/M")),
                    value = it.volumeMilliliters.toFloat()
                )
            }

            SimpleBarChart(
                data = chartData,
                explicitYAxisTopValue = 5000f,
                goalLineValue = 4000f,
                chartContentDescription = "Water intake chart",
                modifier = Modifier.padding(top = 16.dp)
            )

            LazyColumn {
                items(waterIntakeRecords) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Water: ${record.volumeMilliliters} ml at ${formatter.format(record.time)}",
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
