package com.d4viddf.medicationreminder.ui.screens.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight // Added import
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width // Ensure width is imported for bar width
import androidx.compose.foundation.rememberScrollState // For horizontal and vertical scroll
import androidx.compose.foundation.verticalScroll // For vertical scroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
// Removed: import androidx.compose.foundation.layout.width - If not used by other elements after this refactor
// Removed: import androidx.compose.foundation.shape.RoundedCornerShape - If not used by other elements
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
// Removed: import androidx.compose.foundation.BorderStroke - No longer used by GraphViewButton
// Removed: import androidx.compose.material3.Button - No longer used by GraphViewButton
// Removed: import androidx.compose.material3.ButtonDefaults - No longer used by GraphViewButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
// Removed: import androidx.compose.material3.OutlinedButton - No longer used by GraphViewButton
// Removed: import androidx.compose.material3.TextButton - If it was ever here
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import android.util.Log
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationSpecificTheme
import com.d4viddf.medicationreminder.viewmodel.ChartyGraphEntry
import com.d4viddf.medicationreminder.viewmodel.MedicationGraphViewModel
// Custom SimpleBarChart imports
import com.d4viddf.medicationreminder.ui.components.SimpleBarChart
import com.d4viddf.medicationreminder.ui.components.BarChartItem
// Removed Charty Imports
// import com.himanshoe.charty.charts.BarChart
// import com.himanshoe.charty.common.ChartData // Might be Point
// import com.himanshoe.charty.common.Point // Preferred for clarity
// import com.himanshoe.charty.common.config.ChartColor
// import com.himanshoe.charty.common.config.SolidChartColor
// import com.himanshoe.charty.common.extensions.asSolidChartColor
// import com.himanshoe.charty.bar.config.BarChartConfig - Ensure these are removed if not used
// import com.himanshoe.charty.common.config.LabelConfig - Ensure these are removed if not used
// import com.himanshoe.charty.bar.config.BarChartColorConfig - Ensure these are removed if not used
import java.time.DayOfWeek
import java.time.LocalDate
// Removed: import java.time.YearMonth - No longer used
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class GraphViewType {
    WEEK, YEAR // Month removed
}

// GraphViewButton and DateNavigationControls composables are confirmed removed from previous step.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationGraphScreen(
    medicationId: Int,
    colorName: String,
    onNavigateBack: () -> Unit,
    viewModel: MedicationGraphViewModel? = null
) {
    Log.d("GraphScreenEntry", "MedicationGraphScreen composed. medicationId: $medicationId, viewModel is null: ${viewModel == null}, colorName: $colorName")

    val medicationColor = remember(colorName) {
        try { MedicationColor.valueOf(colorName) } catch (e: IllegalArgumentException) { MedicationColor.LIGHT_ORANGE }
    }

    var selectedViewType by remember { mutableStateOf(GraphViewType.WEEK) }
    var currentWeekMonday by remember { mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) }
    var currentDisplayedYear by remember { mutableStateOf(LocalDate.now().year) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val medicationName by viewModel?.medicationName?.collectAsState() ?: remember { mutableStateOf("Sample Medication (Preview)") }
    val chartEntries by viewModel?.chartyGraphData?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val isLoading by viewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val error by viewModel?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

    LaunchedEffect(medicationId, selectedViewType, currentWeekMonday, currentDisplayedYear, viewModel) {
        if (medicationId > 0 && viewModel != null) {
            when (selectedViewType) {
                GraphViewType.WEEK -> {
                    val currentWeekDays = List(7) { i -> currentWeekMonday.plusDays(i.toLong()) }
                    viewModel.loadWeeklyGraphData(medicationId, currentWeekDays)
                }
                GraphViewType.YEAR -> {
                    viewModel.loadYearlyGraphData(medicationId, currentDisplayedYear)
                }
            }
        } else if (viewModel != null) {
            viewModel.clearGraphData() // Assuming such a function exists or can be added to clear data
            Log.d("MedicationGraphScreen", "Invalid medicationId ($medicationId), clearing graph data.")
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
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Controls Row: Previous, Dropdown, Next
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                when (selectedViewType) {
                                    GraphViewType.WEEK -> currentWeekMonday = currentWeekMonday.minusWeeks(1)
                                    GraphViewType.YEAR -> currentDisplayedYear--
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(id = R.string.previous_period_cd))
                            }

                            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                ExposedDropdownMenuBox(
                                    expanded = dropdownExpanded,
                                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                                ) {
                                    TextField(
                                        value = if (selectedViewType == GraphViewType.WEEK) stringResource(R.string.graph_view_weekly) else stringResource(R.string.graph_view_yearly),
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        textStyle = MaterialTheme.typography.titleSmall.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.graph_view_weekly)) },
                                            onClick = {
                                                selectedViewType = GraphViewType.WEEK
                                                dropdownExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.graph_view_yearly)) },
                                            onClick = {
                                                selectedViewType = GraphViewType.YEAR
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = {
                                when (selectedViewType) {
                                    GraphViewType.WEEK -> currentWeekMonday = currentWeekMonday.plusWeeks(1)
                                    GraphViewType.YEAR -> currentDisplayedYear++
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(id = R.string.next_period_cd))
                            }
                        }

                        // Display Current Period
                        val weekDayMonthFormatter = remember { DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()) }
                        val currentPeriodDisplayName = when (selectedViewType) {
                            GraphViewType.WEEK -> {
                                val weekEndDate = currentWeekMonday.plusDays(6)
                                "${currentWeekMonday.format(weekDayMonthFormatter)} - ${weekEndDate.format(weekDayMonthFormatter)}"
                            }
                            GraphViewType.YEAR -> currentDisplayedYear.toString()
                        }
                        Text(
                            text = currentPeriodDisplayName,
                            style = MaterialTheme.typography.labelMedium, // Smaller text for period display
                            modifier = Modifier.padding(bottom = 16.dp)
                        )


                        val chartTitle = when (selectedViewType) {
                            GraphViewType.WEEK -> stringResource(R.string.weekly_doses_taken_title)
                            GraphViewType.YEAR -> stringResource(R.string.yearly_doses_taken_title_template, currentDisplayedYear)
                        }
                        Text(
                            text = chartTitle,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                        )

                        val displayableBarChartItems = remember(chartEntries, isLoading, error, selectedViewType, currentWeekMonday, currentDisplayedYear) {
                            if (!isLoading && error == null && chartEntries.isEmpty()) {
                                when (selectedViewType) {
                                    GraphViewType.WEEK -> {
                                        val today = LocalDate.now()
                                        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                                        List(7) { i ->
                                            val day = currentWeekMonday.plusDays(i.toLong())
                                            BarChartItem(label = day.format(dayFormatter), value = 0f, isHighlighted = day.isEqual(today))
                                        }
                                    }
                                    GraphViewType.YEAR -> {
                                        val today = LocalDate.now()
                                        List(12) { i ->
                                            val month = java.time.Month.of(i + 1)
                                            BarChartItem(label = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), value = 0f, isHighlighted = currentDisplayedYear == today.year && month == today.month)
                                        }
                                    }
                                }
                            } else {
                        chartEntries.map { entry ->
                            BarChartItem(
                                label = entry.xValue,
                                value = entry.yValue,
                                isHighlighted = entry.isHighlighted
                            )
                        }
                    }
                }

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp) // Keep consistent height
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
                                .height(220.dp) // Keep consistent height
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error ?: stringResource(id = R.string.med_graph_unknown_error),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    // Removed barChartItems.isEmpty() case, chart is always displayed if not loading/error
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth() // This Box defines the viewport width
                                .height(220.dp) // And height
                                // Removed: .horizontalScroll(rememberScrollState())
                                .pointerInput(selectedViewType) { // Key by selectedViewType to re-evaluate swipe logic if view changes
                                    var dragConsumed = false
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            if (abs(dragAmount) > 50 && !dragConsumed) { // Threshold and debounce
                                                dragConsumed = true
                                                if (dragAmount > 0) { // Swiped Left
                                                    when (selectedViewType) {
                                                        GraphViewType.WEEK -> currentWeekMonday = currentWeekMonday.minusWeeks(1)
                                                        GraphViewType.YEAR -> currentDisplayedYear--
                                                    }
                                                } else { // Swiped Right
                                                    when (selectedViewType) {
                                                        GraphViewType.WEEK -> currentWeekMonday = currentWeekMonday.plusWeeks(1)
                                                        GraphViewType.YEAR -> currentDisplayedYear++
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = { dragConsumed = false }
                                    )
                                }
                        ) {
                            SimpleBarChart(
                                data = displayableBarChartItems, // Use the new list
                                modifier = Modifier.fillMaxSize(), // Chart fills the Box, which has defined height
                                highlightedBarColor = MaterialTheme.colorScheme.primary,
                                normalBarColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                // barWidthDp and spaceAroundBarsDp are now handled dynamically by SimpleBarChart
                                valueTextColor = MaterialTheme.colorScheme.onSurface // Or specific color
                            )
                        }
                    }
                } // Closes Column inside ElevatedCard
              } // Closes ElevatedCard
            } // Closes Column inside Scaffold
        } // Closes Scaffold
    } // Closes MedicationSpecificTheme
}


@Preview(showBackground = true, name = "Medication Graph Screen - Week View")
@Composable
fun MedicationGraphScreenPreviewWeek() {
    AppTheme {
        MedicationGraphScreen(
            medicationId = 1,
            colorName = "LIGHT_GREEN",
            onNavigateBack = {},
            viewModel = null // ViewModel is nullable for preview
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Medication Graph Screen - Year View") // Renamed
@Composable
fun MedicationGraphScreenPreviewYear() { // Renamed
    AppTheme {
        // val selectedViewType = GraphViewType.YEAR // Set selectedViewType for preview if needed, though it's internal state
        val medicationName by remember { mutableStateOf("Sample Medication (Preview)") }
        val currentYear = LocalDate.now().year

        // Sample data for SimpleBarChart - Year View
        val sampleBarChartItems = remember {
            List(12) { i ->
                val month = java.time.Month.of(i + 1)
                BarChartItem(
                    label = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    value = (0..25).random().toFloat(), // Example values for yearly data
                    isHighlighted = month == LocalDate.now().month && currentYear == LocalDate.now().year
                )
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
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
                // Simplified controls for preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* No-op */ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                    Text("Yearly") // Simplified display for preview
                    IconButton(onClick = { /* No-op */ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val chartTitle = stringResource(R.string.yearly_doses_taken_title_template, currentYear)

                Text(
                    text = chartTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    SimpleBarChart(
                        data = sampleBarChartItems,
                        modifier = Modifier.fillMaxSize(),
                        highlightedBarColor = MaterialTheme.colorScheme.primary,
                        normalBarColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
