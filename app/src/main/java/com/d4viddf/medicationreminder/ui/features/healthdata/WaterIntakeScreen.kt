package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.d4viddf.medicationreminder.ui.common.util.formatNumber
import com.d4viddf.medicationreminder.ui.features.common.charts.HealthDataChart
import com.d4viddf.medicationreminder.ui.features.common.charts.YAxisPosition
import com.d4viddf.medicationreminder.ui.features.healthdata.component.DateRangeSelector
import com.d4viddf.medicationreminder.ui.features.healthdata.component.HealthChart
import com.d4viddf.medicationreminder.ui.features.healthdata.util.ChartType
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.ui.theme.Dimensions
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WaterIntakeScreen(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass,
    viewModel: WaterIntakeViewModel = hiltViewModel()
) {
    val chartData by viewModel.chartData.collectAsState()
    val aggregatedWaterIntakeRecords by viewModel.aggregatedWaterIntakeRecords.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val dateRangeText by viewModel.dateRangeText.collectAsState()
    val isNextEnabled by viewModel.isNextEnabled.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val endTime by viewModel.endTime.collectAsState()
    val totalWaterIntake by viewModel.totalWaterIntake.collectAsState()
    val numberOfDaysInRange by viewModel.numberOfDaysInRange.collectAsState()
    val waterIntakeGoal by viewModel.dailyGoal.collectAsState()
    val waterIntakeByType by viewModel.waterIntakeByType.collectAsState()
    val yAxisMax by viewModel.yAxisMax.collectAsState()
    val selectedBar by viewModel.selectedBar.collectAsState()
    val selectedChartBar by viewModel.selectedChartBar.collectAsState()
    val headerAverage by viewModel.headerAverage.collectAsState()
    val headerDaysGoalReached by viewModel.headerDaysGoalReached.collectAsState()
    val headerTotalIntake by viewModel.headerTotalIntake.collectAsState()
    val weekFields = WeekFields.of(Locale.getDefault())
    val today = LocalDate.now()
    val dateText = when (dateRangeText) {
        "this_week" -> stringResource(id = R.string.this_week)
        "this_month" -> stringResource(id = R.string.this_month)
        "today" -> stringResource(id = R.string.today)
        "yesterday" -> stringResource(id = R.string.yesterday)
        else -> dateRangeText
    }
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
            if (widthSizeClass == WindowWidthSizeClass.Compact) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.LogWater.route) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(text = stringResource(id = R.string.log_water)) }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = Dimensions.PaddingMedium)
        ) {
            stickyHeader {
                Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                    PrimaryTabRow(selectedTabIndex = timeRange.ordinal) {
                        TimeRange.entries.forEach { range ->
                            Tab(
                                selected = timeRange == range,
                                onClick = { viewModel.setTimeRange(range) },
                                text = { Text(stringResource(id = range.titleResId)) }
                            )
                        }
                    }
                    DateRangeSelector(
                        dateRange = dateText,
                        onPreviousClick = viewModel::onPreviousClick,
                        onNextClick = viewModel::onNextClick,
                        isNextEnabled = isNextEnabled,
                        onDateRangeClick = { /* No-op */ },
                        widthSizeClass = widthSizeClass
                    )
                }
            }

            if (timeRange == TimeRange.DAY) {
                item {
                    Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.PaddingLarge),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${formatNumber(totalWaterIntake.toInt())} ml",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(id = R.string.water_intake_remaining_goal, formatNumber((waterIntakeGoal - totalWaterIntake).toInt())),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(contentAlignment = Alignment.Center) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = (totalWaterIntake / waterIntakeGoal).toFloat(),
                                animationSpec = tween(durationMillis = 1000),
                                label = "WaterIntakeProgressAnimation"
                            )
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(100.dp),
                                strokeWidth = Dimensions.PaddingMedium
                            )
                            Text(
                                text = "${(totalWaterIntake / waterIntakeGoal * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    if (waterIntakeByType.isNotEmpty()) {
                        Text(
                            text = dateRangeText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                start = Dimensions.PaddingLarge,
                                top = Dimensions.PaddingLarge,
                                bottom = Dimensions.PaddingLarge
                            )
                        )
                    }
                }

                val items = waterIntakeByType.entries.toList()
                itemsIndexed(items, key = { index, (type, _) -> (type ?: "custom") + index }) { index, (type, records) ->
                    val shape = when {
                        items.size == 1 -> RoundedCornerShape(Dimensions.PaddingMedium)
                        index == 0 -> RoundedCornerShape(topStart = Dimensions.PaddingMedium, topEnd = Dimensions.PaddingMedium)
                        index == items.size - 1 -> RoundedCornerShape(
                            bottomStart = Dimensions.PaddingMedium,
                            bottomEnd = Dimensions.PaddingMedium
                        )

                        else -> RoundedCornerShape(0.dp)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        shape = shape
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimensions.PaddingLarge),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${records.size} ${type ?: stringResource(id = R.string.water_intake_custom_quantity)} ")
                            Text(text = "${formatNumber(records.sumOf { it.volumeMilliliters }.toInt())} ml")
                        }
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier.padding(
                            start = Dimensions.PaddingLarge,
                            end = Dimensions.PaddingLarge,
                            top = Dimensions.PaddingLarge
                        )
                    ) {
                        if (selectedChartBar != null) {
                            val valueText = if (timeRange == TimeRange.YEAR) {
                                "${formatNumber(selectedChartBar!!.value.roundToInt())} ml (average)"
                            } else {
                                "${formatNumber(selectedChartBar!!.value.roundToInt())} ml"
                            }
                            Text(
                                text = valueText,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedChartBar!!.fullLabel,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.headlineLarge.fontSize)) {
                                        append("${formatNumber(headerAverage.roundToInt())} ml")
                                    }
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal, fontSize = MaterialTheme.typography.titleLarge.fontSize)) {
                                        append(stringResource(R.string.water_intake_average_day))
                                    }
                                }
                            )
                            Text(
                                text = stringResource(id = R.string.water_intake_goal_reached_days, headerDaysGoalReached, formatNumber(headerTotalIntake.toInt())),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

                        HealthDataChart(
                            data = chartData,
                            barColor = MaterialTheme.colorScheme.primary,
                            onBarSelected = { viewModel.onChartBarSelected(it) },
                            showTooltip = false,
                            showGoalLine = true,
                            goalLineValue = waterIntakeGoal.toFloat(),
                            yAxisMax = yAxisMax.toFloat(),
                            yAxisLabelFormatter = { value ->
                                if (value >= 1000) {
                                    "${formatNumber(value.toInt() / 1000)}k"
                                } else {
                                    formatNumber(value.roundToInt())
                                }
                            }
                        )
                    }
                }



                item {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            start = Dimensions.PaddingMedium,
                            top = Dimensions.PaddingLarge,
                            bottom = Dimensions.PaddingLarge
                        )
                    )
                }

                val recordsToShow = if (timeRange == TimeRange.YEAR) {
                    aggregatedWaterIntakeRecords.filter {
                        it.first.atZone(ZoneId.systemDefault()).toLocalDate().isBefore(today.withDayOfMonth(1).plusMonths(1))
                    }
                } else {
                    aggregatedWaterIntakeRecords
                }
                itemsIndexed(
                    recordsToShow.sortedByDescending { it.first },
                    key = { index, record -> record.first.toEpochMilli() + index }
                ) { index, record ->
                    val shape = when {
                        recordsToShow.size == 1 -> RoundedCornerShape(Dimensions.PaddingMedium)
                        index == 0 -> RoundedCornerShape(topStart = Dimensions.PaddingMedium, topEnd = Dimensions.PaddingMedium)
                        index == recordsToShow.size - 1 -> RoundedCornerShape(
                            bottomStart = Dimensions.PaddingMedium,
                            bottomEnd = Dimensions.PaddingMedium
                        )

                        else -> RoundedCornerShape(0.dp)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                            .clickable {
                                viewModel.onHistoryItemClick(
                                    when (timeRange) {
                                        TimeRange.WEEK -> TimeRange.DAY
                                        TimeRange.MONTH -> TimeRange.WEEK
                                        TimeRange.YEAR -> TimeRange.MONTH
                                        else -> timeRange
                                    },
                                    record.first
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                )
                            },
                        shape = shape
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimensions.PaddingLarge),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val recordDate = record.first.atZone(ZoneId.systemDefault()).toLocalDate()
                            val startOfWeek = recordDate.with(weekFields.dayOfWeek(), 1)
                            val yesterday = today.minusDays(1)

                            RecordDateText(timeRange = timeRange, recordDate = recordDate, today = today, weekFields = weekFields)
                            Text(text = "${formatNumber(record.second.toInt())} ml")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Dimensions.BottomSpacerHeight))
            }
        }
    }
}

@Composable
private fun RecordDateText(timeRange: TimeRange, recordDate: LocalDate, today: LocalDate, weekFields: WeekFields) {
    val text = when (timeRange) {
        TimeRange.DAY, TimeRange.WEEK -> when (recordDate) {
            today -> stringResource(R.string.today)
            today.minusDays(1) -> stringResource(R.string.yesterday)
            else -> recordDate.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
        }
        TimeRange.MONTH -> {
            val startOfWeek = recordDate.with(weekFields.dayOfWeek(), 1)
            val endOfWeek = startOfWeek.plusDays(6)
            val startMonth = startOfWeek.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
            val endMonth = endOfWeek.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))

            if (startMonth == endMonth) {
                "${startOfWeek.dayOfMonth} - ${endOfWeek.dayOfMonth} ${endMonth.replace(".", "")}"
            } else {
                "${startOfWeek.dayOfMonth} ${startMonth.replace(".", "")} - ${endOfWeek.dayOfMonth} ${endMonth.replace(".", "")}"
            }
        }
        TimeRange.YEAR -> {
            if (recordDate.month == today.month && recordDate.year == today.year) {
                stringResource(R.string.current_month)
            } else {
                recordDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
            }
        }
        else -> ""
    }
    Text(text = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
}