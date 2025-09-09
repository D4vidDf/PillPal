package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.healthdata.component.DateRangeSelector
import com.d4viddf.medicationreminder.ui.features.healthdata.component.LineChart
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import com.d4viddf.medicationreminder.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass,
    viewModel: WeightViewModel = hiltViewModel()
) {
    val weightUiState by viewModel.weightUiState.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val dateRangeText by viewModel.dateRangeText.collectAsState()
    val isNextEnabled by viewModel.isNextEnabled.collectAsState()
    val weightGoal by viewModel.weightGoal.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weight_tracker)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.settings)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.configure_weight)) },
                            onClick = {
                                navController.navigate(Screen.NutritionWeightSettings.route)
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (widthSizeClass == WindowWidthSizeClass.Compact) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.LogWeight.route) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(text = stringResource(id = R.string.log_weight)) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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

            LineChart(
                data = weightUiState.chartData.lineChartData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp),
                showPoints = true,
                goal = weightGoal,
                yAxisRange = weightUiState.yAxisRange
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                itemsIndexed(weightUiState.weightLogs) { index, weightEntry ->
                    com.d4viddf.medicationreminder.ui.features.healthdata.component.HistoryListItem(
                        index = index,
                        size = weightUiState.weightLogs.size,
                        date = weightEntry.date.toLocalDate(),
                        value = "${weightEntry.weight} kg",
                        onClick = { /* No-op */ }
                    )
                }
            }
        }
    }
}
