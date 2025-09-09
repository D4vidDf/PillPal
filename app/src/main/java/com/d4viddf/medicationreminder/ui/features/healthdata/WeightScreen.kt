package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val averageWeight by viewModel.averageWeight.collectAsState()

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

            if (timeRange != TimeRange.DAY) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                                append(String.format("%.1f", averageWeight))
                            }
                            withStyle(style = SpanStyle(fontSize = 16.sp)) {
                                append(" kg (average)")
                            }
                        }
                    )
                }
            }

            when (timeRange) {
                TimeRange.DAY -> {
                    DayView(weightUiState = weightUiState, weightGoal = weightGoal)
                }
                TimeRange.WEEK, TimeRange.MONTH -> {
                    LineChart(
                        data = weightUiState.chartData.lineChartData,
                        labels = weightUiState.chartData.labels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp),
                        showLine = false,
                        showPoints = true,
                        goal = weightGoal,
                        yAxisRange = weightUiState.yAxisRange
                    )
                }
                TimeRange.YEAR -> {
                    LineChart(
                        data = weightUiState.chartData.lineChartData,
                        labels = weightUiState.chartData.labels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp),
                        showLine = true,
                        showPoints = true,
                        showGradient = false,
                        goal = weightGoal,
                        yAxisRange = weightUiState.yAxisRange
                    )
                }
            }
        }
    }
}

@Composable
private fun DayView(weightUiState: WeightUiState, weightGoal: Float) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f", weightUiState.currentWeight),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "kg")
            }
            Box(contentAlignment = Alignment.Center) {
                val animatedProgress by animateFloatAsState(
                    targetValue = weightUiState.weightProgress,
                    animationSpec = tween(durationMillis = 1000),
                    label = "WeightProgressAnimation"
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(100.dp),
                    strokeWidth = 8.dp
                )
                Text(
                    text = "${(weightUiState.weightProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f", weightGoal),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Goal")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.history),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(weightUiState.weightLogs) { weightEntry ->
                com.d4viddf.medicationreminder.ui.features.healthdata.component.HistoryListItem(
                    date = weightEntry.date.toLocalDate(),
                    value = "${String.format("%.1f", weightEntry.weight)} kg",
                    onClick = { /* No-op */ }
                )
            }
        }
    }
}
