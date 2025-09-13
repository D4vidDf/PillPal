package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            MoreInfoBottomSheet(
                title = stringResource(id = R.string.about_weight_info_title),
                items = listOf(
                    MoreInfoItem(
                        title = stringResource(id = R.string.what_is_bmi_title),
                        content = stringResource(id = R.string.what_is_bmi_content)
                    ),
                    MoreInfoItem(
                        title = stringResource(id = R.string.what_is_body_fat_title),
                        content = stringResource(id = R.string.what_is_body_fat_content)
                    ),
                    MoreInfoItem(
                        title = stringResource(id = R.string.what_is_lean_mass_title),
                        content = stringResource(id = R.string.what_is_lean_mass_content)
                    )
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weight_tracker)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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

                if (weightUiState.chartData.lineChartData.isEmpty()) {
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
                } else if (timeRange != TimeRange.DAY) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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

                if (timeRange == TimeRange.DAY) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    )
                } else {
                    LineChart(
                        data = weightUiState.chartData.lineChartData,
                        labels = weightUiState.chartData.labels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        showLine = timeRange == TimeRange.YEAR,
                        showPoints = true,
                        showGradient = false,
                        goal = weightGoal,
                        yAxisRange = weightUiState.yAxisRange
                    )
                }
            }

            itemsIndexed(weightUiState.weightLogs) { index, weightEntry ->
                com.d4viddf.medicationreminder.ui.features.healthdata.component.HistoryListItem(
                    index = index,
                    size = weightUiState.weightLogs.size,
                    date = weightEntry.date.toLocalDate(),
                    value = "${String.format("%.1f", weightEntry.weight)} kg",
                    onClick = { /* No-op */ }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                AboutHealthDataItem(
                    title = stringResource(id = R.string.about_weight),
                    description = stringResource(id = R.string.about_weight_description),
                    onMoreInfoClick = {
                        scope.launch {
                            showBottomSheet = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
