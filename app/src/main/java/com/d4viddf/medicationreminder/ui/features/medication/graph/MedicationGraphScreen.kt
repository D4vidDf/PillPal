package com.d4viddf.medicationreminder.ui.features.medication.graph

import android.util.Log
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.theme.MedicationSpecificTheme
import com.d4viddf.medicationreminder.ui.features.medication.graph.component.BarChartItem
import com.d4viddf.medicationreminder.ui.features.medication.graph.component.SimpleBarChart
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

@Composable
private fun StyledNavigationArrow(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null, // Made nullable
    iconPainter: Painter? = null, // Added painter
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .background(
                color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), // Adjusted disabled color
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        if (iconPainter != null) {
            Icon(
                painter = iconPainter,
                contentDescription = contentDescription,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // Adjust disabled tint
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // Adjust disabled tint
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationGraphScreen(
    medicationId: Int,
    colorName: String,
    onNavigateBack: () -> Unit,
    viewModel: MedicationGraphViewModel? = null,
    widthSizeClass: WindowWidthSizeClass, // Added parameter
    onNavigateToHistoryForDate: (medicationId: Int, colorName: String, date: LocalDate) -> Unit,
    onNavigateToHistoryForMonth: (medicationId: Int, colorName: String, yearMonth: YearMonth) -> Unit
) {
    Log.d("GraphScreenEntry", "MedicationGraphScreen composed. medicationId: $medicationId, viewModel is null: ${viewModel == null}, colorName: $colorName")

    val medicationColor = remember(colorName) {
        try { MedicationColor.valueOf(colorName) } catch (e: IllegalArgumentException) { MedicationColor.LIGHT_ORANGE }
    }

    val today = remember { LocalDate.now() }
    val currentCalendarWeekMonday = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) } // The Monday of the current actual week
    val minYear = remember { today.year - 5 }
    // The Monday of the week containing January 1st of the earliest allowed year.
    val minWeekOverallLimitMonday = remember { LocalDate.of(minYear, 1, 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }


    var currentWeekMonday by remember { mutableStateOf(currentCalendarWeekMonday) }
    var currentDisplayedYear by remember { mutableIntStateOf(today.year) }

    // State for Date Pickers
    var showWeekPickerDialog by remember { mutableStateOf(false) }
    // Year Picker might be a simpler custom dialog or NumberPicker if available/desired
    var showYearPickerDialog by remember { mutableStateOf(false) }


    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState()) // Changed scroll behavior
    // val medicationName by viewModel?.medicationName?.collectAsState() ?: remember { mutableStateOf("Sample Medication (Preview)") } // Potentially use in AppBar

    // Collect separate data flows
    val weeklyChartEntries by viewModel?.weeklyChartData?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val yearlyChartEntries by viewModel?.yearlyChartData?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    val isLoadingWeekly by viewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) } // Assuming one isLoading for now
    val errorWeekly by viewModel?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }   // Assuming one error for now
    // TODO: ViewModel might need separate isLoading/error states for weekly/yearly if they load independently and can fail independently.

    val weeklyMaxYForChart by (viewModel?.weeklyMaxYValue?.collectAsState() ?: remember { mutableStateOf(5f) }) // Collect weekly max
    val yearlyMaxYForChart by (viewModel?.yearlyMaxYValue?.collectAsState() ?: remember { mutableStateOf(30f) }) // Collect yearly max

    // Data Loading LaunchedEffects
    LaunchedEffect(medicationId, currentWeekMonday, viewModel) {
        Log.d("MedGraphScreenLog", "Weekly LaunchedEffect triggered. MedId: $medicationId, WeekMonday: $currentWeekMonday, VM: ${viewModel != null}")
        if (medicationId > 0 && viewModel != null) {
            val currentWeekDays = List(7) { i -> currentWeekMonday.plusDays(i.toLong()) }
            Log.d("MedGraphScreenLog", "Before calling loadWeeklyGraphData. MedId: $medicationId, Days: $currentWeekDays")
            viewModel.loadWeeklyGraphData(medicationId, currentWeekDays)
            Log.d("MedGraphScreenLog", "After calling loadWeeklyGraphData.")
        } else if (viewModel != null) {
            viewModel.clearGraphData() // Or specific clear for weekly
            Log.d("MedGraphScreenLog", "Invalid medicationId ($medicationId) or null ViewModel, clearing weekly graph data.")
        }
    }

    LaunchedEffect(weeklyChartEntries) {
        Log.d("MedGraphScreenLog", "Collected weeklyChartEntries. Count: ${weeklyChartEntries.size}")
        if (weeklyChartEntries.isNotEmpty()) {
            Log.d("MedGraphScreenLog", "First weekly entry: ${weeklyChartEntries.firstOrNull()}")
        }
    }

    LaunchedEffect(medicationId, currentDisplayedYear, viewModel) {
        Log.d("MedGraphScreenLog", "Yearly LaunchedEffect triggered. MedId: $medicationId, Year: $currentDisplayedYear, VM: ${viewModel != null}")
        if (medicationId > 0 && viewModel != null) {
            Log.d("MedGraphScreenLog", "Before calling loadYearlyGraphData. MedId: $medicationId, Year: $currentDisplayedYear")
            viewModel.loadYearlyGraphData(medicationId, currentDisplayedYear)
            Log.d("MedGraphScreenLog", "After calling loadYearlyGraphData.")
        } else if (viewModel != null) {
            viewModel.clearGraphData() // Or specific clear for yearly
            Log.d("MedGraphScreenLog", "Invalid medicationId ($medicationId) or null ViewModel, clearing yearly graph data.")
        }
    }

    LaunchedEffect(yearlyChartEntries) {
        Log.d("MedGraphScreenLog", "Collected yearlyChartEntries. Count: ${yearlyChartEntries.size}")
        if (yearlyChartEntries.isNotEmpty()) {
            Log.d("MedGraphScreenLog", "First yearly entry: ${yearlyChartEntries.firstOrNull()}")
        }
    }

    MedicationSpecificTheme(medicationColor = medicationColor) {
        Scaffold(
            topBar = {
                TopAppBar( // Changed from MediumTopAppBar
                    title = { Text(stringResource(R.string.medication_statistics_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_arrow_back_ios_new_24),
                                contentDescription = stringResource(id = R.string.back_button_cd)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior, // Passed scrollBehavior
                    colors = TopAppBarDefaults.topAppBarColors(// Changed to largeTopAppBarColors
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface // Kept as onSurface for consistency
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
                // Determine layout based on widthSizeClass
                when (widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WeeklyChartCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                currentWeekMondayInternal = currentWeekMonday,
                                onCurrentWeekMondayChange = { newDate: LocalDate -> currentWeekMonday = newDate },
                                onShowWeekPickerDialogChange = { shouldShow: Boolean -> showWeekPickerDialog = shouldShow },
                                medicationColor = medicationColor, // Pass medicationColor
                                weeklyChartEntries = weeklyChartEntries,
                                isLoading = isLoadingWeekly,
                                error = errorWeekly,
                                today = today,
                                currentCalendarWeekMonday = currentCalendarWeekMonday,
                                minWeekOverallLimitMonday = minWeekOverallLimitMonday,
                                maxYValue = weeklyMaxYForChart, // Pass weekly max
                                medicationId = medicationId, // Pass medicationId
                                onNavigateToHistoryForDate = onNavigateToHistoryForDate // Pass callback
                            )
                            YearlyChartCard(
                                modifier = Modifier.fillMaxWidth(),
                                currentDisplayedYearInternal = currentDisplayedYear,
                                onCurrentDisplayedYearChange = { newYear: Int -> currentDisplayedYear = newYear },
                                onShowYearPickerDialogChange = { shouldShow: Boolean -> showYearPickerDialog = shouldShow },
                                medicationColor = medicationColor, // Pass medicationColor
                                yearlyChartEntries = yearlyChartEntries,
                                isLoading = isLoadingWeekly,
                                error = errorWeekly,
                                today = today,
                                minYear = minYear,
                                maxYValue = yearlyMaxYForChart, // Pass yearly max
                                medicationId = medicationId, // Pass medicationId
                                onNavigateToHistoryForMonth = onNavigateToHistoryForMonth // Pass callback
                            )
                        }
                    }
                    else -> { // Medium, Expanded
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            WeeklyChartCard(
                                modifier = Modifier.weight(1f),
                                currentWeekMondayInternal = currentWeekMonday,
                                onCurrentWeekMondayChange = { newDate: LocalDate -> currentWeekMonday = newDate },
                                onShowWeekPickerDialogChange = { shouldShow: Boolean -> showWeekPickerDialog = shouldShow },
                                medicationColor = medicationColor, // Pass medicationColor
                                weeklyChartEntries = weeklyChartEntries,
                                isLoading = isLoadingWeekly,
                                error = errorWeekly,
                                today = today,
                                currentCalendarWeekMonday = currentCalendarWeekMonday,
                                minWeekOverallLimitMonday = minWeekOverallLimitMonday,
                                maxYValue = weeklyMaxYForChart, // Pass weekly max
                                medicationId = medicationId, // Pass medicationId
                                onNavigateToHistoryForDate = onNavigateToHistoryForDate // Pass callback
                            )
                            YearlyChartCard(
                                modifier = Modifier.weight(1f),
                                currentDisplayedYearInternal = currentDisplayedYear,
                                onCurrentDisplayedYearChange = { newYear: Int -> currentDisplayedYear = newYear },
                                onShowYearPickerDialogChange = { shouldShow: Boolean -> showYearPickerDialog = shouldShow },
                                medicationColor = medicationColor, // Pass medicationColor
                                yearlyChartEntries = yearlyChartEntries,
                                isLoading = isLoadingWeekly,
                                error = errorWeekly,
                                today = today, // Added missing parameter
                                minYear = minYear,   // Added missing parameter
                                maxYValue = yearlyMaxYForChart, // Pass yearly max
                                medicationId = medicationId, // Pass medicationId
                                onNavigateToHistoryForMonth = onNavigateToHistoryForMonth // Pass callback
                            )
                        }
                    }
                }

                // Week Picker Dialog
                if (showWeekPickerDialog) {
                    // val today = LocalDate.now() // 'today' is already available in this scope
                    val datePickerState = rememberDatePickerState( // Only one declaration
                        initialSelectedDateMillis = currentWeekMonday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        yearRange = IntRange(minYear, today.year), // Use minYear and today from outer scope
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                val selectedDate = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                                // Allow selecting any day in the minWeekOverallLimitMonday's week by checking against start of that week
                                val startOfWeekForMinOverallLimit = minWeekOverallLimitMonday
                                return !selectedDate.isAfter(today) && !selectedDate.isBefore(startOfWeekForMinOverallLimit)
                            }
                            override fun isSelectableYear(year: Int): Boolean {
                                return year <= today.year && year >= minYear
                            }
                        }
                    )
                    DatePickerDialog(
                        onDismissRequest = { showWeekPickerDialog = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                                        // Ensure the selected Monday is not before the overall limit or after current view limit
                                        var newMonday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                        if (newMonday.isBefore(minWeekOverallLimitMonday)) newMonday = minWeekOverallLimitMonday
                                        if (newMonday.isAfter(currentCalendarWeekMonday)) newMonday = currentCalendarWeekMonday
                                        currentWeekMonday = newMonday // Directly update state
                                    }
                                    showWeekPickerDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = medicationColor.onBackgroundColor,
                                    contentColor = medicationColor.cardColor
                                )
                            ) { Text(stringResource(android.R.string.ok)) }
                        },
                        dismissButton = {
                            Button(
                                onClick = { showWeekPickerDialog = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = medicationColor.onBackgroundColor,
                                    contentColor = medicationColor.cardColor
                                )
                            ) { Text(stringResource(android.R.string.cancel)) }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // Year Picker Dialog
                if (showYearPickerDialog) {
                    val years = remember { (minYear..today.year).toList().reversed() }
                    AlertDialog(
                        onDismissRequest = { showYearPickerDialog = false },
                        title = { Text("Select Year") }, // Reverted to hardcoded
                        text = {
                            LazyColumn {
                                items(years) { year ->
                                    Text(
                                        text = year.toString(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentDisplayedYear = year // Directly update state
                                                showYearPickerDialog = false
                                            }
                                            .padding(vertical = 12.dp), // Make list items taller
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showYearPickerDialog = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = medicationColor.onBackgroundColor,
                                    contentColor = medicationColor.cardColor
                                )
                            ) {
                                Text(stringResource(android.R.string.cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}

// Internal Composable for Weekly Chart Card
@Composable
private fun WeeklyChartCard(
    modifier: Modifier = Modifier,
    currentWeekMondayInternal: LocalDate,
    onCurrentWeekMondayChange: (LocalDate) -> Unit,
    // Added
    onShowWeekPickerDialogChange: (Boolean) -> Unit, // Added
    weeklyChartEntries: List<ChartyGraphEntry>,
    isLoading: Boolean,
    error: String?,
    // Date limit parameters
    today: LocalDate,
    currentCalendarWeekMonday: LocalDate,
    minWeekOverallLimitMonday: LocalDate,
    medicationColor: MedicationColor, // Added parameter
    maxYValue: Float, // New parameter
    medicationId: Int,
    onNavigateToHistoryForDate: (medicationId: Int, colorName: String, date: LocalDate) -> Unit
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.weekly_doses_taken_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Removed redundant date limit declarations, use passed-in parameters

                StyledNavigationArrow(
                    iconPainter = painterResource(id = R.drawable.rounded_arrow_back_ios_new_24),
                    contentDescription = "Previous week", // Reverted to hardcoded
                    onClick = {
                        val prevWeek = currentWeekMondayInternal.minusWeeks(1)
                        if (!prevWeek.isBefore(minWeekOverallLimitMonday)) {
                            onCurrentWeekMondayChange(prevWeek)
                        }
                    },
                    enabled = !currentWeekMondayInternal.minusWeeks(1).isBefore(minWeekOverallLimitMonday)
                )

                val weekDayMonthFormatter = remember { DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()) }
                val displayedWeekRange = "${currentWeekMondayInternal.format(weekDayMonthFormatter)} - ${currentWeekMondayInternal.plusDays(6).format(weekDayMonthFormatter)}"
                // val changeWeekContentDesc = stringResource(R.string.change_week_cd_prefix) + " " + displayedWeekRange // Reverted
                Button(
                    onClick = { onShowWeekPickerDialogChange(true) },
                    modifier = Modifier.semantics { contentDescription = "Change week, current period: $displayedWeekRange" }, // Reverted
                    colors = ButtonDefaults.buttonColors(
                        containerColor = medicationColor.onBackgroundColor,
                        contentColor = medicationColor.cardColor
                    )
                ) {
                    Text(
                        text = displayedWeekRange,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                StyledNavigationArrow(
                    iconPainter = painterResource(id = R.drawable.rounded_arrow_forward_ios_24),
                    contentDescription = "Next week", // Reverted to hardcoded
                    onClick = {
                        val nextWeek = currentWeekMondayInternal.plusWeeks(1)
                        if (!nextWeek.isAfter(currentCalendarWeekMonday)) {
                            onCurrentWeekMondayChange(nextWeek)
                        }
                    },
                    enabled = !currentWeekMondayInternal.plusWeeks(1).isAfter(currentCalendarWeekMonday)
                )
            }

            // Prepare target items (always 7 for weekly)
            val targetItems = remember(weeklyChartEntries, currentWeekMondayInternal, today) { // Added today to key for highlight
                if (weeklyChartEntries.isEmpty()) {
                    // Generate 7 placeholder ChartyGraphEntry items for the current week with 0f values
                    val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                    List(7) { i ->
                        val day = currentWeekMondayInternal.plusDays(i.toLong())
                        ChartyGraphEntry(
                            xValue = day.format(dayFormatter),
                            yValue = 0f,
                            isHighlighted = day.isEqual(today) // Use 'today' from parameter
                        )
                    }
                } else {
                    weeklyChartEntries
                }
            }

            // Animate each item
            val animatedDisplayableItems = targetItems.map { chartEntry ->
                val animatedYValue by animateFloatAsState(
                    targetValue = chartEntry.yValue,
                    animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
                    label = "weeklyBarValue-${chartEntry.xValue}"
                )
                BarChartItem(
                    label = chartEntry.xValue,
                    value = animatedYValue,
                    isHighlighted = chartEntry.isHighlighted
                )
            }

            // Content display logic
            if (isLoading && weeklyChartEntries.isEmpty() && error == null) { // Show loader only if loading AND no data at all (initial load)
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 80.dp).align(Alignment.CenterHorizontally))
            } else if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 80.dp).align(Alignment.CenterHorizontally))
            } else {
                // Chart Box (always present if not initial loading or error)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(currentWeekMondayInternal) {
                            var dragConsumed = false
                            detectHorizontalDragGestures( // No changes needed here, logic seems fine
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (abs(dragAmount) > 40 && !dragConsumed) {
                                        dragConsumed = true
                                        // Use passed-in limits
                                        if (dragAmount > 0) { // Swiped Left (older dates)
                                            val prevWeek = currentWeekMondayInternal.minusWeeks(1)
                                            if (!prevWeek.isBefore(minWeekOverallLimitMonday)) {
                                                onCurrentWeekMondayChange(prevWeek)
                                            }
                                        } else { // Swiped Right (newer dates)
                                            val nextWeek = currentWeekMondayInternal.plusWeeks(1)
                                            if (!nextWeek.isAfter(currentCalendarWeekMonday)) { // Use passed-in currentCalendarWeekMonday
                                                onCurrentWeekMondayChange(nextWeek)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = { dragConsumed = false }
                            )
                        }
                ) {
                    val medGraphChartDesSuffix= stringResource(R.string.medGraph_chart_doses_suffix)
                    val weeklyChartDesc = stringResource(R.string.medGraph_weekly_chart_description_prefix) + " " + animatedDisplayableItems.joinToString { item -> "${item.label}: ${item.value.toInt()} $medGraphChartDesSuffix" }
                    SimpleBarChart(
                        data = animatedDisplayableItems, // Use animated items
                        modifier = Modifier.fillMaxSize(),
                        highlightedBarColor = MaterialTheme.colorScheme.primary,
                        normalBarColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        valueTextColor = MaterialTheme.colorScheme.onSurface,
                        chartContentDescription = weeklyChartDesc, // Added
                        explicitYAxisTopValue = maxYValue, // Pass to SimpleBarChart
                        onBarClick = { dayLabel ->
                            val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                            var clickedDate: LocalDate? = null
                            for (i in 0..6) {
                                val day = currentWeekMondayInternal.plusDays(i.toLong())
                                if (day.format(dayFormatter) == dayLabel) {
                                    clickedDate = day
                                    break
                                }
                            }
                            clickedDate?.let {
                                onNavigateToHistoryForDate(medicationId, medicationColor.name, it)
                            }
                        }
                    )
                }
            }
        }
    }
}

// Internal Composable for Yearly Chart Card
@Composable
private fun YearlyChartCard(
    modifier: Modifier = Modifier,
    currentDisplayedYearInternal: Int,
    onCurrentDisplayedYearChange: (Int) -> Unit,
    // Added
    onShowYearPickerDialogChange: (Boolean) -> Unit, // Added
    yearlyChartEntries: List<ChartyGraphEntry>,
    isLoading: Boolean,
    error: String?,
    // Date limit parameters
    today: LocalDate,
    minYear: Int,
    medicationColor: MedicationColor, // Added parameter
    maxYValue: Float, // New parameter
    medicationId: Int,
    onNavigateToHistoryForMonth: (medicationId: Int, colorName: String, yearMonth: YearMonth) -> Unit
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.yearly_doses_taken_title_template, currentDisplayedYearInternal), // Corrected to use internal
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Removed redundant date limit declarations, use passed-in parameters

                StyledNavigationArrow(
                    iconPainter = painterResource(id = R.drawable.rounded_arrow_back_ios_new_24),
                    contentDescription = "Previous year", // Reverted to hardcoded
                    onClick = {
                        if (currentDisplayedYearInternal - 1 >= minYear) { // Use passed-in minYear
                            onCurrentDisplayedYearChange(currentDisplayedYearInternal - 1)
                        }
                    },
                    enabled = currentDisplayedYearInternal > minYear // Use passed-in minYear
                )
                Button(
                    onClick = { onShowYearPickerDialogChange(true) },
                    modifier = Modifier.semantics { contentDescription = "Change year, current year: ${currentDisplayedYearInternal.toString()}" }, // Reverted
                    colors = ButtonDefaults.buttonColors(
                        containerColor = medicationColor.onBackgroundColor,
                        contentColor = medicationColor.cardColor
                    )
                ) {
                    Text(
                        text = currentDisplayedYearInternal.toString(),
                        style = MaterialTheme.typography.titleMedium // Changed from labelMedium
                    )
                }
                StyledNavigationArrow(
                    iconPainter = painterResource(id = R.drawable.rounded_arrow_forward_ios_24),
                    contentDescription = "Next year", // Reverted to hardcoded
                    onClick = {
                        if (currentDisplayedYearInternal + 1 <= today.year) { // Use passed-in today
                            onCurrentDisplayedYearChange(currentDisplayedYearInternal + 1)
                        }
                    },
                    enabled = currentDisplayedYearInternal < today.year // Use passed-in today
                )
            }

            // Prepare target items (always 12 for yearly)
            val targetItems = remember(yearlyChartEntries, currentDisplayedYearInternal, today) { // Added today to key
                if (yearlyChartEntries.isEmpty()) {
                    List(12) { i ->
                        val month = Month.of(i + 1)
                        ChartyGraphEntry(
                            xValue = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            yValue = 0f,
                            isHighlighted = (currentDisplayedYearInternal == today.year && month == today.month)
                        )
                    }
                } else {
                    yearlyChartEntries
                }
            }

            // Animate each item
            val animatedDisplayableItems = targetItems.map { chartEntry ->
                val animatedYValue by animateFloatAsState(
                    targetValue = chartEntry.yValue,
                    animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
                    label = "yearlyBarValue-${chartEntry.xValue}"
                )
                BarChartItem(
                    label = chartEntry.xValue,
                    value = animatedYValue,
                    isHighlighted = chartEntry.isHighlighted
                )
            }

            // Content display logic
            if (isLoading && yearlyChartEntries.isEmpty() && error == null) { // Show loader only if loading AND no data at all (initial load)
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 80.dp).align(Alignment.CenterHorizontally))
            } else if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 80.dp).align(Alignment.CenterHorizontally))
            } else {
                // Chart Box (always present if not initial loading or error)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(currentDisplayedYearInternal) {
                            var dragConsumed = false
                            detectHorizontalDragGestures( // No changes needed here
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (abs(dragAmount) > 40 && !dragConsumed) {
                                        dragConsumed = true
                                        // Use limits from card's scope
                                        if (dragAmount > 0) { // Swiped Left (older dates)
                                            if (currentDisplayedYearInternal - 1 >= minYear) { // Use minYear from card scope
                                                onCurrentDisplayedYearChange(currentDisplayedYearInternal - 1)
                                            }
                                        } else { // Swiped Right (newer dates)
                                            if (currentDisplayedYearInternal + 1 <= today.year) { // Use today from card scope
                                                onCurrentDisplayedYearChange(currentDisplayedYearInternal + 1)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = { dragConsumed = false }
                            )
                        }
                ) {
                    val yearlyChartDesc = "Yearly doses taken. ${animatedDisplayableItems.joinToString { item -> "${item.label}: ${item.value.toInt()} doses" }}"
                    SimpleBarChart(
                        data = animatedDisplayableItems, // Use animated items
                        modifier = Modifier.fillMaxSize(),
                        highlightedBarColor = MaterialTheme.colorScheme.primary,
                        normalBarColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        valueTextColor = MaterialTheme.colorScheme.onSurface,
                        chartContentDescription = yearlyChartDesc, // Added
                        explicitYAxisTopValue = maxYValue, // Pass to SimpleBarChart
                        onBarClick = { monthLabel ->
                            // Convert month label (e.g., "Jan") to Month enum, then to YearMonth
                            val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
                            try {
                                val month = Month.from(monthFormatter.parse(monthLabel))
                                val yearMonth = YearMonth.of(currentDisplayedYearInternal, month)
                                onNavigateToHistoryForMonth(medicationId, medicationColor.name, yearMonth)
                            } catch (e: Exception) {
                                Log.e("YearlyChartCard", "Error parsing month label: $monthLabel", e)
                            }
                        }
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
            viewModel = null, // ViewModel is nullable for preview
            widthSizeClass = WindowWidthSizeClass.Compact, // Example
            onNavigateToHistoryForDate = { _, _, _ -> }, // Dummy lambda for preview
            onNavigateToHistoryForMonth = { _, _, _ -> } // Dummy lambda for preview
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Medication Graph Screen - Year View") // Renamed
@Composable
fun MedicationGraphScreenPreviewYear() { // Renamed
    AppTheme {
        val currentYear = LocalDate.now().year

        // Sample data for SimpleBarChart - Year View
        val sampleBarChartItems = remember {
            List(12) { i ->
                val month = Month.of(i + 1)
                BarChartItem(
                    label = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    value = (0..25).random().toFloat(), // Example values for yearly data
                    isHighlighted = month == LocalDate.now().month && currentYear == LocalDate.now().year
                )
            }
        }
        MedicationGraphScreen(
            medicationId = 1,
            colorName = "LIGHT_BLUE",
            onNavigateBack = {},
            viewModel = null,
            widthSizeClass = WindowWidthSizeClass.Compact, // Example
            onNavigateToHistoryForDate = { _, _, _ -> }, // Dummy lambda for preview
            onNavigateToHistoryForMonth = { _, _, _ -> } // Dummy lambda for preview
        )
    }
}
