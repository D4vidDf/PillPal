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
// import androidx.compose.foundation.layout.width // Assuming not directly used elsewhere after card setup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape // Added for StyledNavigationArrow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.SelectableDates // Added for DatePicker
import androidx.compose.material3.DropdownMenuItem // Will be removed
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox // Will be removed
import androidx.compose.material3.ExposedDropdownMenuDefaults // Will be removed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField // Will be removed
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.foundation.lazy.LazyColumn // Added for Year Picker
import androidx.compose.foundation.lazy.items // Added for Year Picker
import androidx.compose.foundation.clickable // Added for clickable text in year picker
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
import java.time.Instant
import java.time.ZoneId
import androidx.compose.ui.graphics.vector.ImageVector // Added for StyledNavigationArrow

// GraphViewType enum can be removed if no longer used internally after this refactor
// enum class GraphViewType {
//     WEEK, YEAR
// }

@Composable
private fun StyledNavigationArrow(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .background(
                color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), // Adjusted disabled color
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // Adjust disabled tint
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationGraphScreen(
    medicationId: Int,
    colorName: String,
    onNavigateBack: () -> Unit,
    viewModel: MedicationGraphViewModel? = null,
    widthSizeClass: WindowWidthSizeClass // Added parameter
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
    var currentDisplayedYear by remember { mutableStateOf(today.year) }

    // State for Date Pickers
    var showWeekPickerDialog by remember { mutableStateOf(false) }
    // Year Picker might be a simpler custom dialog or NumberPicker if available/desired
    var showYearPickerDialog by remember { mutableStateOf(false) }


    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    // val medicationName by viewModel?.medicationName?.collectAsState() ?: remember { mutableStateOf("Sample Medication (Preview)") } // Potentially use in AppBar

    // Collect separate data flows
    val weeklyChartEntries by viewModel?.weeklyChartData?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val yearlyChartEntries by viewModel?.yearlyChartData?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    val isLoadingWeekly by viewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) } // Assuming one isLoading for now
    val errorWeekly by viewModel?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }   // Assuming one error for now
    // TODO: ViewModel might need separate isLoading/error states for weekly/yearly if they load independently and can fail independently.

    // Data Loading LaunchedEffects
    LaunchedEffect(medicationId, currentWeekMonday, viewModel) {
        if (medicationId > 0 && viewModel != null) {
            val currentWeekDays = List(7) { i -> currentWeekMonday.plusDays(i.toLong()) }
            viewModel.loadWeeklyGraphData(medicationId, currentWeekDays)
        } else if (viewModel != null) {
            viewModel.clearGraphData() // Or specific clear for weekly
            Log.d("MedicationGraphScreen", "Invalid medicationId ($medicationId) or null ViewModel, clearing weekly graph data.")
        }
    }

    LaunchedEffect(medicationId, currentDisplayedYear, viewModel) {
        if (medicationId > 0 && viewModel != null) {
            viewModel.loadYearlyGraphData(medicationId, currentDisplayedYear)
        } else if (viewModel != null) {
            viewModel.clearGraphData() // Or specific clear for yearly
            Log.d("MedicationGraphScreen", "Invalid medicationId ($medicationId) or null ViewModel, clearing yearly graph data.")
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
                // Determine layout based on widthSizeClass
                when (widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WeeklyChartCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                viewModel = viewModel,
                                currentWeekMonday = currentWeekMonday,
                                onUpdateWeekMonday = { newMonday -> currentWeekMonday = newMonday },
                                onShowWeekPicker = { showWeekPickerDialog = true },
                                weeklyChartEntries = weeklyChartEntries, // Pass collected data
                                isLoading = isLoadingWeekly, // Pass loading state
                                error = errorWeekly // Pass error state
                            )
                            YearlyChartCard(
                                modifier = Modifier.fillMaxWidth(),
                                viewModel = viewModel,
                                currentDisplayedYear = currentDisplayedYear,
                                onUpdateYear = { newYear -> currentDisplayedYear = newYear },
                                onShowYearPicker = { showYearPickerDialog = true },
                                yearlyChartEntries = yearlyChartEntries, // Pass collected data
                                isLoading = isLoadingWeekly, // Pass loading state (assuming shared for now)
                                error = errorWeekly // Pass error state (assuming shared for now)
                            )
                        }
                    }
                    else -> { // Medium, Expanded
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            WeeklyChartCard(
                                modifier = Modifier.weight(1f),
                                viewModel = viewModel,
                                currentWeekMonday = currentWeekMonday,
                                onUpdateWeekMonday = { newMonday -> currentWeekMonday = newMonday },
                                onShowWeekPicker = { showWeekPickerDialog = true },
                                weeklyChartEntries = weeklyChartEntries,
                                isLoading = isLoadingWeekly,
                                error = errorWeekly
                            )
                            YearlyChartCard(
                                modifier = Modifier.weight(1f),
                                viewModel = viewModel,
                                currentDisplayedYear = currentDisplayedYear,
                                onUpdateYear = { newYear -> currentDisplayedYear = newYear },
                                onShowYearPicker = { showYearPickerDialog = true },
                                yearlyChartEntries = yearlyChartEntries,
                                isLoading = isLoadingWeekly,
                                error = errorWeekly
                            )
                        }
                    }
                }

                // Week Picker Dialog
                if (showWeekPickerDialog) {
                    val today = LocalDate.now()
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = currentWeekMonday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        yearRange = IntRange(today.year - 5, today.year),
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                val selectedDate = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                                return !selectedDate.isAfter(today)
                            }
                            override fun isSelectableYear(year: Int): Boolean {
                                return year <= today.year && year >= today.year - 5
                            }
                        }
                    )
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = currentWeekMonday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        yearRange = IntRange(minYear, today.year),
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                val selectedDate = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                                return !selectedDate.isAfter(today) && !selectedDate.isBefore(minWeekOverallLimitMonday.minusDays(6)) // Allow selecting any day in the min week
                            }
                            override fun isSelectableYear(year: Int): Boolean {
                                return year <= today.year && year >= minYear
                            }
                        }
                    )
                    DatePickerDialog(
                        onDismissRequest = { showWeekPickerDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    var selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                                    // Ensure the selected Monday is not before the overall limit or after current view limit
                                    var newMonday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                    if (newMonday.isBefore(minWeekOverallLimitMonday)) newMonday = minWeekOverallLimitMonday
                                    if (newMonday.isAfter(currentCalendarWeekMonday)) newMonday = currentCalendarWeekMonday
                                    onUpdateWeekMonday(newMonday)
                                }
                                showWeekPickerDialog = false
                            }) { Text(stringResource(android.R.string.ok)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showWeekPickerDialog = false }) { Text(stringResource(android.R.string.cancel)) }
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
                        title = { Text(stringResource(R.string.select_year_title)) },
                        text = {
                            LazyColumn {
                                items(years) { year ->
                                    Text(
                                        text = year.toString(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onUpdateYear(year)
                                                showYearPickerDialog = false
                                            }
                                            .padding(vertical = 12.dp), // Make list items taller
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        },
                        confirmButton = { // Changed to a dismiss button as selection is immediate
                            TextButton(onClick = { showYearPickerDialog = false }) { Text(stringResource(android.R.string.cancel)) }
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
    viewModel: MedicationGraphViewModel?, // Pass ViewModel if needed for specific loading/error states
    currentWeekMonday: LocalDate,
    onUpdateWeekMonday: (LocalDate) -> Unit, // Changed to onUpdateWeekMonday
    onShowWeekPicker: () -> Unit,
    weeklyChartEntries: List<ChartyGraphEntry>,
    isLoading: Boolean,
    error: String?
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
                val today = LocalDate.now() // Re-declare for local context if needed, or pass down
                val currentCalendarWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val minYear = today.year - 5
                val minWeekOverallLimitMonday = LocalDate.of(minYear, 1, 1).with(TemporalAdjusters.firstDayOfYear()).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

                StyledNavigationArrow(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_period_cd),
                    onClick = {
                        val prevWeek = currentWeekMonday.minusWeeks(1)
                        if (!prevWeek.isBefore(minWeekOverallLimitMonday)) {
                            onUpdateWeekMonday(prevWeek)
                        }
                    },
                    enabled = !currentWeekMonday.minusWeeks(1).isBefore(minWeekOverallLimitMonday)
                )

                val weekDayMonthFormatter = remember { DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()) }
                Text(
                    text = "${currentWeekMonday.format(weekDayMonthFormatter)} - ${currentWeekMonday.plusDays(6).format(weekDayMonthFormatter)}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { onShowWeekPicker() }
                )
                StyledNavigationArrow(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_period_cd),
                    onClick = {
                        val nextWeek = currentWeekMonday.plusWeeks(1)
                        if (!nextWeek.isAfter(currentCalendarWeekMonday)) {
                            onUpdateWeekMonday(nextWeek)
                        }
                    },
                    enabled = !currentWeekMonday.plusWeeks(1).isAfter(currentCalendarWeekMonday)
                )
            }

            val displayableItems = remember(weeklyChartEntries, isLoading, error, currentWeekMonday) {
                if (!isLoading && error == null && weeklyChartEntries.isEmpty()) {
                    val today = LocalDate.now()
                    val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
                    List(7) { i ->
                        val day = currentWeekMonday.plusDays(i.toLong())
                        BarChartItem(label = day.format(dayFormatter), value = 0f, isHighlighted = day.isEqual(today))
                    }
                } else {
                    weeklyChartEntries.map { BarChartItem(it.xValue, it.yValue, it.isHighlighted) }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 80.dp))
            } else if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 80.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Slightly smaller height for cards
                        .pointerInput(currentWeekMonday) { // Key by currentWeekMonday
                            var dragConsumed = false
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (abs(dragAmount) > 40 && !dragConsumed) {
                                        dragConsumed = true
                                        val today = LocalDate.now() // Ensure fresh 'today' inside gesture
                                        val currentCalWeekMon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                        val minYr = today.year - 5
                                        val minWeekOverallMon = LocalDate.of(minYr, 1, 1).with(TemporalAdjusters.firstDayOfYear()).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

                                        if (dragAmount > 0) { // Swiped Left (older dates)
                                            val prevWeek = currentWeekMonday.minusWeeks(1)
                                            if (!prevWeek.isBefore(minWeekOverallMon)) {
                                                onUpdateWeekMonday(prevWeek)
                                            }
                                        } else { // Swiped Right (newer dates)
                                            val nextWeek = currentWeekMonday.plusWeeks(1)
                                            if (!nextWeek.isAfter(currentCalWeekMon)) {
                                                onUpdateWeekMonday(nextWeek)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = { dragConsumed = false }
                            )
                        }
                ) {
                    SimpleBarChart(
                        data = displayableItems,
                        modifier = Modifier.fillMaxSize(),
                        highlightedBarColor = MaterialTheme.colorScheme.primary,
                        normalBarColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        valueTextColor = MaterialTheme.colorScheme.onSurface
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
    viewModel: MedicationGraphViewModel?,
    currentDisplayedYear: Int,
    onUpdateYear: (Int) -> Unit,
    onShowYearPicker: () -> Unit,
    yearlyChartEntries: List<ChartyGraphEntry>,
    isLoading: Boolean,
    error: String?
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.yearly_doses_taken_title_template, currentDisplayedYear),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val today = LocalDate.now() // Re-declare for local context
                val minYr = today.year - 5

                StyledNavigationArrow(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_period_cd),
                    onClick = {
                        if (currentDisplayedYear - 1 >= minYr) {
                            onUpdateYear(currentDisplayedYear - 1)
                        }
                    },
                    enabled = currentDisplayedYear > minYr
                )
                Text(
                    text = currentDisplayedYear.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { onShowYearPicker() }
                )
                StyledNavigationArrow(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_period_cd),
                    onClick = {
                        if (currentDisplayedYear + 1 <= today.year) {
                            onUpdateYear(currentDisplayedYear + 1)
                        }
                    },
                    enabled = currentDisplayedYear < today.year
                )
            }

             val displayableItems = remember(yearlyChartEntries, isLoading, error, currentDisplayedYear) {
                if (!isLoading && error == null && yearlyChartEntries.isEmpty()) {
                    val today = LocalDate.now()
                    List(12) { i ->
                        val month = java.time.Month.of(i + 1)
                        BarChartItem(label = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), value = 0f, isHighlighted = currentDisplayedYear == today.year && month == today.month)
                    }
                } else {
                    yearlyChartEntries.map { BarChartItem(it.xValue, it.yValue, it.isHighlighted) }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 80.dp))
            } else if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 80.dp))
            } else {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(currentDisplayedYear) { // Key by currentDisplayedYear
                            var dragConsumed = false
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    if (abs(dragAmount) > 40 && !dragConsumed) {
                                        dragConsumed = true
                                        val today = LocalDate.now() // Ensure fresh 'today'
                                        val minYr = today.year - 5
                                        if (dragAmount > 0) { // Swiped Left (older dates)
                                            if (currentDisplayedYear - 1 >= minYr) {
                                                onUpdateYear(currentDisplayedYear - 1)
                                            }
                                        } else { // Swiped Right (newer dates)
                                            if (currentDisplayedYear + 1 <= today.year) {
                                                onUpdateYear(currentDisplayedYear + 1)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = { dragConsumed = false }
                            )
                        }
                ) {
                    SimpleBarChart(
                        data = displayableItems,
                        modifier = Modifier.fillMaxSize(),
                        highlightedBarColor = MaterialTheme.colorScheme.primary,
                        normalBarColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        valueTextColor = MaterialTheme.colorScheme.onSurface
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
            widthSizeClass = WindowWidthSizeClass.Compact // Example
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Medication Graph Screen - Year View") // Renamed
@Composable
fun MedicationGraphScreenPreviewYear() { // Renamed
    AppTheme {
        val medicationName by remember { mutableStateOf("Sample Medication (Preview)") } // Not directly used by screen, but kept for context
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
        // Simplified: Directly call the YearlyChartCard for preview if MedicationGraphScreen is too complex to set up
        // Or pass parameters to MedicationGraphScreen to show yearly view by default if possible
        MedicationGraphScreen(
            medicationId = 1,
            colorName = "LIGHT_BLUE",
            onNavigateBack = {},
            viewModel = null,
            widthSizeClass = WindowWidthSizeClass.Compact // Example
        )
        // For a more isolated preview of YearlyChartCard:
        // YearlyChartCard(
        //     viewModel = null,
        //     currentDisplayedYear = currentYear,
        //     onUpdateYear = {},
        //     onShowYearPicker = {},
        //     yearlyChartEntries = sampleBarChartItems,
        //     isLoading = false,
        //     error = null
        // )
    }
}
