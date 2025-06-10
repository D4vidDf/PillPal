package com.d4viddf.medicationreminder.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width // Ensure width is imported for bar width
import androidx.compose.foundation.rememberScrollState // For horizontal and vertical scroll
import androidx.compose.foundation.horizontalScroll // For horizontal scroll
import androidx.compose.foundation.verticalScroll // For vertical scroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Keep this import
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.BorderStroke // Added import
import androidx.compose.material3.Button // Added import
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator // New import
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton // Added import
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.TopAppBar // Ensure this is imported for the preview
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas // New import
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect // New import
import androidx.compose.ui.graphics.Color // Added import
import androidx.compose.ui.graphics.Path // New import
import androidx.compose.ui.input.nestedscroll.nestedScroll // New import
import androidx.compose.ui.platform.LocalDensity // New import
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight // Added import for FontWeight
import androidx.compose.ui.text.style.TextAlign // New import
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R // Moved import to top
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
// Removed ThemedAppBarBackButton import
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme exists
import com.d4viddf.medicationreminder.ui.theme.MedicationSpecificTheme
import com.d4viddf.medicationreminder.viewmodel.MedicationGraphViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth // Added for currentDisplayedMonth
import java.time.format.DateTimeFormatter // Added for month formatting

import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class GraphViewType {
    WEEK, MONTH, YEAR
}

@Composable
fun GraphViewButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val commonModifier = Modifier.padding(horizontal = 4.dp)
    val shape = RoundedCornerShape(8.dp)

    if (isSelected) {
        Button( // Filled button for selected state
            onClick = onClick,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = commonModifier
        ) {
            Text(text)
        }
    } else {
        OutlinedButton( // Outlined button for unselected state
            onClick = onClick,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), // Simpler border
            modifier = commonModifier
        ) {
            Text(text)
        }
    }
}

@Composable
fun DateNavigationControls( // Made non-private
    selectedViewType: GraphViewType,
    currentPeriodDisplayName: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(id = R.string.previous_period_cd)
            )
        }
        Text(
            text = currentPeriodDisplayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.next_period_cd)
            )
        }
    }
}

@Composable
fun BarChartDisplay( // Made non-private
    graphData: Map<String, Int>,
    selectedViewType: GraphViewType,
    currentDisplayedMonth: YearMonth,
    currentDisplayedYear: Int,
    modifier: Modifier = Modifier
) {
    val maxCount = graphData.values.maxOrNull() ?: 1
    val today = LocalDate.now()
    val todayShortName = today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val currentDayOfMonthForHighlight = if (currentDisplayedMonth.year == today.year && currentDisplayedMonth.month == today.month) {
        today.dayOfMonth.toString()
    } else {
        null
    }
    val currentMonthShortNameForHighlight = if (currentDisplayedYear == today.year) {
        today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } else {
        null
    }
    val barMaxHeight = 160.dp
    val rowModifier = if (selectedViewType == GraphViewType.WEEK) {
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(vertical = 16.dp)
    } else {
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    }
    val rowArrangement = if (selectedViewType == GraphViewType.WEEK) {
        Arrangement.SpaceEvenly
    } else {
        Arrangement.spacedBy(8.dp)
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = rowArrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        graphData.forEach { (label, count) ->
            val highlight = when(selectedViewType) {
                GraphViewType.WEEK -> label.equals(todayShortName, ignoreCase = true)
                GraphViewType.MONTH -> label == currentDayOfMonthForHighlight
                GraphViewType.YEAR -> label.equals(currentMonthShortNameForHighlight, ignoreCase = true) && currentDisplayedYear == today.year
            }
            val columnModifier = if (selectedViewType == GraphViewType.WEEK) {
                Modifier.weight(1f)
            } else {
                Modifier
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = columnModifier
            ) {
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(MaterialTheme.typography.labelSmall.lineHeight.value.dp + 1.dp))
                }

                val barColor = if (highlight) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.secondaryContainer
                val currentDensity = LocalDensity.current
                val barHeightPx = with(currentDensity) {
                    if (maxCount > 0) ((count.toFloat() / maxCount.toFloat()) * barMaxHeight.value).dp.toPx() else 0f
                }
                val barWidthDp = if (selectedViewType == GraphViewType.WEEK) 30.dp else 20.dp
                val barWidthPx = with(currentDensity) { barWidthDp.toPx() }
                val topRadiusPx = with(currentDensity) { 4.dp.toPx() }

                Canvas(
                    modifier = Modifier
                        .width(barWidthDp)
                        .height(if (maxCount > 0) ((count.toFloat() / maxCount.toFloat()) * barMaxHeight.value).dp else 0.dp)
                ) {
                    if (barHeightPx > 0f) {
                        val path = Path().apply {
                            val actualRadius = minOf(topRadiusPx, barWidthPx / 2, barHeightPx / 2)
                            // Start from bottom-left, go up to top-left curve start
                            moveTo(0f, barHeightPx)
                            lineTo(0f, actualRadius)
                            // Top-left arc
                            arcTo(
                                rect = Rect(0f, 0f, 2 * actualRadius, 2 * actualRadius),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false
                            )
                            lineTo(barWidthPx - actualRadius, 0f)
                            // Top-right arc
                            arcTo(
                                rect = Rect(barWidthPx - 2 * actualRadius, 0f, barWidthPx, 2 * actualRadius),
                                startAngleDegrees = 270f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false
                            )
                            lineTo(barWidthPx, barHeightPx)
                            close()
                        }
                        drawPath(path = path, color = barColor)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationGraphScreen(
    medicationId: Int,
    colorName: String,
    onNavigateBack: () -> Unit,
    viewModel: MedicationGraphViewModel? = null // Made nullable for preview
) {
    val medicationColor = remember(colorName) {
        try {
            MedicationColor.valueOf(colorName)
        } catch (e: IllegalArgumentException) {
            MedicationColor.LIGHT_ORANGE // Fallback
        }
    }
    var selectedViewType by remember { mutableStateOf(GraphViewType.WEEK) }
    var currentDisplayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var currentDisplayedYear by remember { mutableStateOf(LocalDate.now().year) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val medicationName by viewModel?.medicationName?.collectAsState() ?: remember { mutableStateOf("Sample Medication (Preview)") }
    val graphData by viewModel?.graphData?.collectAsState() ?: remember {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val shortDayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        mutableStateOf(
            mapOf(
                monday.format(shortDayFormatter) to 2,
                monday.plusDays(1).format(shortDayFormatter) to 3,
                monday.plusDays(2).format(shortDayFormatter) to 1,
                monday.plusDays(3).format(shortDayFormatter) to 4,
                monday.plusDays(4).format(shortDayFormatter) to 2,
                monday.plusDays(5).format(shortDayFormatter) to 0,
                monday.plusDays(6).format(shortDayFormatter) to 1
            )
        )
    }
    val isLoading by viewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val error by viewModel?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

    LaunchedEffect(medicationId, selectedViewType, currentDisplayedMonth, currentDisplayedYear, viewModel) {
        if (viewModel != null) {
            when (selectedViewType) {
                GraphViewType.WEEK -> {
                    val today = LocalDate.now()
                    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val currentWeekDays = List(7) { i -> monday.plusDays(i.toLong()) }
                    viewModel.loadWeeklyGraphData(medicationId, currentWeekDays)
                }
                GraphViewType.MONTH -> {
                    viewModel.loadMonthlyGraphData(medicationId, currentDisplayedMonth)
                }
                GraphViewType.YEAR -> {
                    viewModel.loadYearlyGraphData(medicationId, currentDisplayedYear)
                }
            }
        }
    }

    MedicationSpecificTheme(medicationColor = medicationColor) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), // Added
            topBar = {
                MediumTopAppBar( // Changed
                    title = { Text(stringResource(id = R.string.medication_statistics_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back_button_cd)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior, // Added
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp) // Existing content padding
                    .verticalScroll(rememberScrollState()), // Make content column scrollable
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GraphViewButton(
                        text = stringResource(id = R.string.graph_view_weekly),
                        isSelected = selectedViewType == GraphViewType.WEEK,
                        onClick = { selectedViewType = GraphViewType.WEEK }
                    )
                    GraphViewButton(
                        text = stringResource(id = R.string.graph_view_monthly),
                        isSelected = selectedViewType == GraphViewType.MONTH,
                        onClick = { selectedViewType = GraphViewType.MONTH }
                    )
                    GraphViewButton(
                        text = stringResource(id = R.string.graph_view_yearly),
                        isSelected = selectedViewType == GraphViewType.YEAR,
                        onClick = { selectedViewType = GraphViewType.YEAR }
                    )
                }

                if (selectedViewType == GraphViewType.MONTH || selectedViewType == GraphViewType.YEAR) {
                    val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
                    val currentDisplayPeriodText = when (selectedViewType) {
                        GraphViewType.MONTH -> currentDisplayedMonth.format(monthYearFormatter)
                        GraphViewType.YEAR -> currentDisplayedYear.toString()
                        else -> ""
                    }
                    DateNavigationControls(
                        selectedViewType = selectedViewType,
                        currentPeriodDisplayName = currentDisplayPeriodText,
                        onPrevious = {
                            if (selectedViewType == GraphViewType.MONTH) {
                                currentDisplayedMonth = currentDisplayedMonth.minusMonths(1)
                            } else if (selectedViewType == GraphViewType.YEAR) {
                                currentDisplayedYear--
                            }
                        },
                        onNext = {
                            if (selectedViewType == GraphViewType.MONTH) {
                                currentDisplayedMonth = currentDisplayedMonth.plusMonths(1)
                            } else if (selectedViewType == GraphViewType.YEAR) {
                                currentDisplayedYear++
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
                val chartTitle = when (selectedViewType) {
                    GraphViewType.WEEK -> stringResource(R.string.weekly_doses_taken_title)
                    GraphViewType.MONTH -> stringResource(R.string.monthly_doses_taken_title_template, currentDisplayedMonth.format(monthFormatter))
                    GraphViewType.YEAR -> stringResource(R.string.yearly_doses_taken_title_template, currentDisplayedYear)
                }
                Text(
                    text = chartTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                )

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp) // Same height as BarChartDisplay area
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp) // Same height
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error ?: stringResource(id = R.string.med_graph_unknown_error), // Display the error message
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    graphData.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(id = R.string.med_graph_no_data_found)) // New string for clarity
                        }
                    }
                    else -> {
                        BarChartDisplay(
                            graphData = graphData,
                            selectedViewType = selectedViewType,
                        currentDisplayedMonth = currentDisplayedMonth,
                        currentDisplayedYear = currentDisplayedYear
                     )
                }
            }
        }
    }
}


@Preview(showBackground = true, name = "Medication Graph Screen - Week View")
@Composable
fun MedicationGraphScreenPreviewWeek() {
    AppTheme {
        MedicationGraphScreen(
            medicationId = 1,
            colorName = "LIGHT_GREEN",
            onNavigateBack = {},
            viewModel = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Medication Graph Screen - Month View")
@Composable
fun MedicationGraphScreenPreviewMonth() {
    AppTheme {
        var selectedViewType by remember { mutableStateOf(GraphViewType.MONTH) }
        val medicationName by remember { mutableStateOf("Sample Medication (Preview)") }
        val currentMonth = YearMonth.now()
        val currentYearForPreview = currentMonth.year
        val sampleMonthlyData = remember {
            (1..currentMonth.lengthOfMonth()).associate { day ->
                day.toString() to (0..5).random()
            }
        }
         val graphData by remember { mutableStateOf(if (selectedViewType == GraphViewType.MONTH) sampleMonthlyData else emptyMap<String,Int>()) }

        Scaffold(
            topBar = {
                TopAppBar( // Use R.string.back_button_cd for consistency if it's generic enough
                    title = { Text(medicationName) },
                    navigationIcon = {
                        IconButton(onClick = { /* Preview no-op */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back_button_cd)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer, // Adjusted for preview
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GraphViewButton(stringResource(id = R.string.graph_view_weekly), selectedViewType == GraphViewType.WEEK) { selectedViewType = GraphViewType.WEEK }
                    GraphViewButton(stringResource(id = R.string.graph_view_monthly), selectedViewType == GraphViewType.MONTH) { selectedViewType = GraphViewType.MONTH }
                    GraphViewButton(stringResource(id = R.string.graph_view_yearly), selectedViewType == GraphViewType.YEAR) { selectedViewType = GraphViewType.YEAR }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
                // val currentYearForPreview = currentMonth.year // Already defined
                val chartTitle = when (selectedViewType) {
                    GraphViewType.WEEK -> stringResource(R.string.weekly_doses_taken_title)
                    GraphViewType.MONTH -> stringResource(R.string.monthly_doses_taken_title_template, currentMonth.format(monthFormatter))
                    GraphViewType.YEAR -> stringResource(R.string.yearly_doses_taken_title_template, currentYearForPreview)
                }
                Text(
                    text = chartTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                )

                if (graphData.isEmpty() && selectedViewType != GraphViewType.WEEK ) {
                     Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(id = R.string.loading_graph_data))
                    }
                } else {
                    BarChartDisplay(
                        graphData = graphData,
                        selectedViewType = selectedViewType,
                        currentDisplayedMonth = currentMonth,
                        currentDisplayedYear = currentYearForPreview
                    )
                }
            }
        }
    }
}
