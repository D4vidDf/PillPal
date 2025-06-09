package com.d4viddf.medicationreminder.ui.screens

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
import androidx.compose.foundation.rememberScrollState // For horizontal scroll
import androidx.compose.foundation.horizontalScroll // For horizontal scroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight // Added import for FontWeight

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R // Moved import to top
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme exists
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationGraphScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MedicationGraphViewModel? = null // Made nullable for preview
) {
    var selectedViewType by remember { mutableStateOf(GraphViewType.WEEK) }
    var currentDisplayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var currentDisplayedYear by remember { mutableStateOf(LocalDate.now().year) } // Added state for year

    // Provide sample data for preview if viewModel is null
    val medicationName by viewModel?.medicationName?.collectAsState() ?: remember { mutableStateOf("Sample Medication (Preview)") }
    val graphData by viewModel?.graphData?.collectAsState() ?: remember {
        // Sample data for weekly view in preview
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

    LaunchedEffect(medicationId, selectedViewType, currentDisplayedMonth, currentDisplayedYear, viewModel) { // Added currentDisplayedYear
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(medicationName.ifEmpty { stringResource(id = R.string.medication_statistics_title) }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.med_stats_navigate_back_cd)
                        )
                    }
                }
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

            // Date Navigation Controls - Placed here
            if (selectedViewType == GraphViewType.MONTH || selectedViewType == GraphViewType.YEAR) {
                val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
                val currentDisplayPeriodText = when (selectedViewType) {
                    GraphViewType.MONTH -> currentDisplayedMonth.format(monthYearFormatter)
                    GraphViewType.YEAR -> currentDisplayedYear.toString()
                    else -> "" // Should not happen as it's guarded by the if
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

            Spacer(modifier = Modifier.height(8.dp)) // Adjusted spacer

            // Chart Title
            val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
            val chartTitle = when (selectedViewType) {
                GraphViewType.WEEK -> stringResource(R.string.weekly_doses_taken_title)
                GraphViewType.MONTH -> stringResource(R.string.monthly_doses_taken_title_template, currentDisplayedMonth.format(monthFormatter))
                GraphViewType.YEAR -> "Yearly Doses - $currentDisplayedYear" // Updated to use currentDisplayedYear
            }
            Text(
                text = chartTitle,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
            )

            // Bar Chart or Placeholder
            if (graphData.isEmpty()) {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(id = R.string.loading_graph_data)) // Or "No data for this view"
                }
            } else {
                 BarChartDisplay(
                    graphData = graphData,
                    selectedViewType = selectedViewType,
                    currentDisplayedMonth = currentDisplayedMonth,
                    currentDisplayedYear = currentDisplayedYear // Pass currentDisplayedYear
                 )
            }
        }
    }
}

@Composable
private fun DateNavigationControls(
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
private fun BarChartDisplay(
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
    val barMaxHeight = 160.dp // Reduced to make space for count text

    val rowModifier = if (selectedViewType == GraphViewType.WEEK) {
        modifier
            .fillMaxWidth() // For WEEK, fill width
            .height(220.dp)
            .padding(vertical = 16.dp)
    } else { // For MONTH, YEAR
        modifier
            .fillMaxWidth() // Still fill width for the scrollable area
            .height(220.dp)
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    }

    val rowArrangement = if (selectedViewType == GraphViewType.WEEK) {
        Arrangement.SpaceEvenly // Distribute weekly bars evenly
    } else {
        Arrangement.spacedBy(8.dp) // More spacing for scrollable views
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
                Modifier.weight(1f) // Weekly bars take equal weighted space
            } else {
                Modifier // Monthly/Yearly bars have fixed width defined on the Box
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
                        modifier = Modifier.padding(bottom = 1.dp) // Small space between count and bar
                    )
                } else {
                    // Leave space for alignment even if count is 0, or adjust layout not to need this.
                    Spacer(modifier = Modifier.height(MaterialTheme.typography.labelSmall.lineHeight.value.dp + 1.dp))
                }
                // Spacer(Modifier.height(2.dp)) // Original position, moved or replaced by padding in Text
                Box(
                    modifier = Modifier
                        .width(if (selectedViewType == GraphViewType.WEEK) 30.dp else 20.dp)
                        .height(if (maxCount > 0) ((count.toFloat() / maxCount.toFloat()) * barMaxHeight.value).dp else 0.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (highlight) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondaryContainer
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


=======
@Composable
private fun GraphViewButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (isSelected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    }

    val border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder

    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = colors,
        border = border,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text)
    }
}


@Preview(showBackground = true, name = "Medication Graph Screen - Week View")
@Composable
fun MedicationGraphScreenPreviewWeek() {
    AppTheme {
        MedicationGraphScreen(
            medicationId = 1,
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
        // Simulate selecting MONTH view for preview
        var selectedViewType by remember { mutableStateOf(GraphViewType.MONTH) }
        val medicationName by remember { mutableStateOf("Sample Medication (Preview)") }
        // Sample data for monthly view in preview
        val currentMonth = YearMonth.now()
        val currentYearForPreview = currentMonth.year // Define for use in title
        val sampleMonthlyData = remember {
            (1..currentMonth.lengthOfMonth()).associate { day ->
                day.toString() to (0..5).random() // Random counts for preview
            }
        }
         val graphData by remember { mutableStateOf(if (selectedViewType == GraphViewType.MONTH) sampleMonthlyData else emptyMap<String,Int>()) }



        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(medicationName) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.med_stats_navigate_back_cd))
                        }
                    }
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
                val currentYearForPreview = currentMonth.year // Assuming currentMonth is still in scope for preview
                val chartTitle = when (selectedViewType) {
                    GraphViewType.WEEK -> stringResource(R.string.weekly_doses_taken_title)
                    GraphViewType.MONTH -> stringResource(R.string.monthly_doses_taken_title_template, currentMonth.format(monthFormatter))
                    GraphViewType.YEAR -> stringResource(R.string.yearly_doses_taken_title_template, currentYearForPreview) // Use defined currentYearForPreview
                }
                Text(
                    text = chartTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                )

                if (graphData.isEmpty() && selectedViewType != GraphViewType.WEEK /* Handled by default data in main composable for WEEK */) {
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
                        currentDisplayedYear = currentYearForPreview // Pass current year for preview
                    )
                }
            }
        }
    }
}
