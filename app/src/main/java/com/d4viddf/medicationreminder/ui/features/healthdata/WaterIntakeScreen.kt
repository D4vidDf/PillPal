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
import com.d4viddf.medicationreminder.ui.features.healthdata.component.HealthChart
import com.d4viddf.medicationreminder.ui.features.healthdata.util.ChartType
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import com.d4viddf.medicationreminder.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterIntakeScreen(
    navController: NavController,
    viewModel: WaterIntakeViewModel = hiltViewModel()
) {
    val aggregatedWaterIntakeRecords by viewModel.aggregatedWaterIntakeRecords.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val dateRangeText by viewModel.dateRangeText.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val endTime by viewModel.endTime.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("d/M H:m").withZone(ZoneId.systemDefault())
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

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
            ExtendedFloatingActionButton(
                onClick = { showBottomSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(text = stringResource(id = R.string.log_water)) }
            )
        }
    ) { paddingValues ->
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                LogWaterScreen(onNavigateBack = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                })
            }
        }
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
                data = aggregatedWaterIntakeRecords,
                chartType = ChartType.BAR,
                timeRange = timeRange,
                startTime = startTime,
                endTime = endTime,
                yAxisRange = 0.0..5000.0,
                goalLineValue = 4000f,
                yAxisLabelFormatter = { "${(it / 1000).toInt()}k" },
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(aggregatedWaterIntakeRecords) { record ->
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
                            text = "Water: ${record.second} ml at ${formatter.format(record.first)}",
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
