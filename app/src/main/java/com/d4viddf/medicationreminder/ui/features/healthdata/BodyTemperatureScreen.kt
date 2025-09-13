package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.healthdata.component.AboutHealthDataItem
import com.d4viddf.medicationreminder.ui.features.healthdata.component.DateRangeSelector
import com.d4viddf.medicationreminder.ui.features.healthdata.component.LineChart
import com.d4viddf.medicationreminder.ui.features.healthdata.component.MoreInfoBottomSheet
import com.d4viddf.medicationreminder.ui.features.healthdata.component.MoreInfoItem
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import com.d4viddf.medicationreminder.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyTemperatureScreen(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass,
    viewModel: BodyTemperatureViewModel = hiltViewModel()
) {
    val temperatureUiState by viewModel.temperatureUiState.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val dateRangeText by viewModel.dateRangeText.collectAsState()
    val isNextEnabled by viewModel.isNextEnabled.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            MoreInfoBottomSheet(
                title = stringResource(id = R.string.about_body_temperature),
                items = listOf(
                    MoreInfoItem(
                        title = stringResource(id = R.string.what_does_body_temp_mean_title),
                        content = stringResource(id = R.string.what_does_body_temp_mean_content)
                    ),
                    MoreInfoItem(
                        title = stringResource(id = R.string.what_can_affect_body_temp_title),
                        content = stringResource(id = R.string.what_can_affect_body_temp_content)
                    ),
                    MoreInfoItem(
                        title = stringResource(id = R.string.differences_between_body_and_skin_temp_title),
                        content = stringResource(id = R.string.differences_between_body_and_skin_temp_content)
                    ),
                    MoreInfoItem(
                        title = stringResource(id = R.string.how_is_body_temp_measured_title),
                        content = stringResource(id = R.string.how_is_body_temp_measured_content)
                    )
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.body_temperature_tracker)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (widthSizeClass == WindowWidthSizeClass.Compact) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.LogTemperature.route) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(text = stringResource(id = R.string.log_temperature)) }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                PrimaryTabRow(selectedTabIndex = timeRange.ordinal) {
                    TimeRange.values().forEach { range ->
                        Tab(
                            selected = timeRange == range,
                            onClick = { viewModel.setTimeRange(range) },
                            text = { Text(stringResource(id = range.titleResId)) }
                        )
                    }
                }
                DateRangeSelector(
                    dateRange = dateRangeText,
                    onPreviousClick = viewModel::onPreviousClick,
                    onNextClick = viewModel::onNextClick,
                    isNextEnabled = isNextEnabled,
                    onDateRangeClick = { /* No-op */ },
                    widthSizeClass = widthSizeClass
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (temperatureUiState.chartData.lineChartData.isEmpty() && temperatureUiState.chartData.rangeChartData.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_data),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(id = R.string.no_temperatures_recorded),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    when (timeRange) {
                        TimeRange.DAY -> {
                            LineChart(
                                data = temperatureUiState.chartData.lineChartData,
                                labels = temperatureUiState.chartData.labels,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                showPoints = true,
                                showGradient = false,
                                yAxisRange = temperatureUiState.yAxisRange
                            )
                        }

                        else -> {
                            com.d4viddf.medicationreminder.ui.features.healthdata.component.RangeBarChart(
                                data = temperatureUiState.chartData.rangeChartData,
                                labels = temperatureUiState.chartData.labels,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                yAxisRange = temperatureUiState.yAxisRange,
                                onBarSelected = { viewModel.onBarSelected(it) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.history),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            itemsIndexed(temperatureUiState.temperatureLogs) { index, tempEntry ->
                com.d4viddf.medicationreminder.ui.features.healthdata.component.HistoryListItem(
                    index = index,
                    size = temperatureUiState.temperatureLogs.size,
                    date = tempEntry.date.toLocalDate(),
                    value = "${tempEntry.temperature}°C",
                    onClick = { /* No-op */ }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                AboutHealthDataItem(
                    title = stringResource(id = R.string.about_body_temperature),
                    description = stringResource(id = R.string.about_body_temperature_description),
                    onMoreInfoClick = {
                        scope.launch {
                            showBottomSheet = true
                        }
                    }
                )
                HorizontalDivider()
                Text(
                    text = stringResource(id = R.string.disclaimer_body_temp),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
