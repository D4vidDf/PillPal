package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow // Added for WeekCalendarView
import androidx.compose.foundation.lazy.LazyListState // Added
import androidx.compose.foundation.lazy.rememberLazyListState // Added
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
// Icons for TopAppBar were removed, but CalendarToday is still needed
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.scrollable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import android.util.Log
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.times
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.ThemeKeys
import com.d4viddf.medicationreminder.ui.calendar.rememberScheduleCalendarState
import com.d4viddf.medicationreminder.ui.calendar.ScheduleCalendarState
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.viewmodel.CalendarDay // Keep for Preview if needed, but main usage removed
import com.d4viddf.medicationreminder.viewmodel.CalendarViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleItem // Keep for Preview if needed
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

import com.d4viddf.medicationreminder.data.Medication // Keep for Preview
import com.d4viddf.medicationreminder.data.MedicationSchedule // Keep for Preview
import com.d4viddf.medicationreminder.data.ScheduleType // Keep for Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.viewmodel.CalendarUiState // Keep for Preview

// Constant for DayCell width, used in CalendarScreen and Previews - may become obsolete or used differently
private val dayWidthForCalendar = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMedicationDetail: (Int) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState() // Still needed for medicationSchedules
    var showDatePickerDialog by remember { mutableStateOf(false) }
    // val weekCalendarScrollState = rememberLazyListState() // Removed
    // val dayWidth = dayWidthForCalendar // Removed or will be used differently
    // val horizontalScrollOffsetPx calculation removed
    // val paginationBuffer removed
    // LaunchedEffect for pagination removed

    val calendarState = rememberScheduleCalendarState() // New calendar state

    if (showDatePickerDialog) {
        ShowDatePicker(
            initialSelectedDate = uiState.selectedDate, // This might need to point to calendarState's current date
            onDateSelected = { newDate ->
                viewModel.setSelectedDate(newDate) // This will eventually need to tell calendarState to scroll
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    Scaffold(
        topBar = {
            CalendarTopAppBar(
                currentMonth = YearMonth.from(calendarState.startDateTime.toLocalDate()), // Updated to use calendarState
                onDateSelectorClicked = { showDatePickerDialog = true }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up available vertical space
                    .scrollable(
                        state = calendarState.scrollableState,
                        orientation = Orientation.Horizontal,
                        flingBehavior = calendarState.scrollFlingBehavior
                    )
            ) {
                LaunchedEffect(constraints.maxWidth) {
                    if (constraints.maxWidth > 0) {
                        calendarState.updateView(newWidth = constraints.maxWidth)
                    }
                }
                Column(Modifier.fillMaxSize()) { // This Column allows stacking DaysRow and later MedicationRows
                    DaysRow(state = calendarState, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    MedicationRowsLayout(
                        state = calendarState,
                        medicationSchedules = uiState.medicationSchedules,
                        totalWidthPx = totalWidthPx,
                        onMedicationClicked = { medicationId -> onNavigateToMedicationDetail(medicationId) },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }
            // Old WeekCalendarView and MedicationScheduleListView are removed from active display
            /*
            WeekCalendarView(
                days = uiState.visibleDays,
                currentMonthFocus = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                onDateSelected = { date -> viewModel.setSelectedDate(date) },
                // listState = weekCalendarScrollState
            )
            HorizontalDivider()
            MedicationScheduleListView(
                medicationSchedules = uiState.medicationSchedules,
                numVisibleDays = uiState.visibleDays.size,
                currentMonth = uiState.currentMonth,
                onMedicationClicked = { medicationId -> onNavigateToMedicationDetail(medicationId) },
                horizontalScrollOffsetPx = 0, // Dummy
                dayWidth = dayWidthForCalendar
            )
            */
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDatePicker(
    initialSelectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// CalendarTopAppBar, WeekCalendarView, DayCell, MedicationScheduleListView, MedicationScheduleRow are heavily modified or removed by implication below.
// Previews will also need to be updated or removed if they rely on the old structure.
// For this step, we are focusing on integrating ScheduleCalendarState and DaysRow.
// The old composables will be removed/commented out in the CalendarScreen's Column.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTopAppBar(
    currentMonth: YearMonth, // This will now be driven by calendarState.startDateTime
    onDateSelectorClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        actions = {
            IconButton(onClick = onDateSelectorClicked) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = stringResource(R.string.select_date)
                )
            }
        }
    )
}

// ParentDataModifier for DaysRow
private data class DayData(val date: LocalDate) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@DayData
}

@Composable
private fun DaysRow(state: ScheduleCalendarState, modifier: Modifier = Modifier) {
    Layout(
        content = {
            var currentDay = state.startDateTime.truncatedTo(ChronoUnit.DAYS)
            val endLoopAt = state.endDateTime.truncatedTo(ChronoUnit.DAYS).plusDays(1) // Corrected: end is exclusive for loop
            var safetyCount = 0 // Safety break for loop
            while (currentDay.isBefore(endLoopAt) && safetyCount < 100) {
                key(currentDay.toLocalDate().toEpochDay()) {
                     Column(
                        modifier = Modifier.then(DayData(currentDay.toLocalDate())),
                        horizontalAlignment = Alignment.CenterHorizontally // Center text within the column
                     ) {
                        Text(
                            text = currentDay.format(DateTimeFormatter.ofPattern("MMM dd")),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                        Text(
                            text = currentDay.format(DateTimeFormatter.ofPattern("E")), // Day of week
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }
                currentDay = currentDay.plusDays(1)
                safetyCount++
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val placeablesWithDate = measurables.mapNotNull { measurable ->
            val dayData = measurable.parentData as? DayData
            if (dayData != null) {
                // Measure with unbounded width for preferred size, but respect original height constraint from parent (DaysRow modifier)
                Pair(measurable.measure(Constraints(maxHeight = constraints.maxHeight)), dayData.date)
            } else {
                null
            }
        }

        val rowHeight = placeablesWithDate.maxOfOrNull { it.first.height } ?: 0

        layout(constraints.maxWidth, rowHeight) {
            placeablesWithDate.forEach { (placeable, date) ->
                val dayStartDateTime = date.atStartOfDay()
                val dayEndDateTime = date.plusDays(1).atStartOfDay()

                val (widthPx, offsetXpx) = state.widthAndOffsetForEvent(
                    start = dayStartDateTime,
                    end = dayEndDateTime,
                    totalWidthPx = constraints.maxWidth
                )

                val centeredX = offsetXpx + ((widthPx - placeable.width) / 2).coerceAtLeast(0)
                placeable.placeRelative(centeredX.coerceAtLeast(0), 0)
            }
        }
    }
}

@Composable
fun MedicationRowsLayout(
    state: ScheduleCalendarState,
    medicationSchedules: List<MedicationScheduleItem>,
    totalWidthPx: Int,
    onMedicationClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    LazyColumn(modifier = modifier) {
        items(medicationSchedules, key = { it.medication.id.toString() + "-" + it.schedule.id.toString() }) { scheduleItem ->
            Box(modifier = Modifier.fillParentMaxWidth().height(40.dp).padding(vertical = 2.dp)) {
                val med = scheduleItem.medication
                val medStartDate = scheduleItem.actualStartDate
                val medEndDate = scheduleItem.actualEndDate

                val eventStartDateTime = medStartDate.atStartOfDay()
                val eventEndDateTime = if (scheduleItem.isOngoingOverall || medEndDate == null) {
                    state.endDateTime
                } else {
                    medEndDate.plusDays(1).atStartOfDay()
                }

                // Optional: Add a check to avoid processing events entirely outside the current state's broad range if many items
                // However, state.widthAndOffsetForEvent should correctly return 0 width for items outside the current viewport.
                if (eventStartDateTime.isAfter(state.endDateTime) || eventEndDateTime.isBefore(state.startDateTime)) {
                    // Event is completely outside the current visible range of ScheduleCalendarState
                    // Don't draw anything for this item in this case.
                } else {
                    val (widthPx, offsetXpx) = state.widthAndOffsetForEvent(
                        start = eventStartDateTime,
                        end = eventEndDateTime,
                        totalWidthPx = totalWidthPx
                    )

                    if (widthPx > 0) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offsetXpx, 0) }
                                .width(with(density) { widthPx.toDp() })
                                .fillMaxHeight()
                                .background(
                                    try { Color(android.graphics.Color.parseColor(med.color ?: "#CCCCCC")) }
                                    catch (e: IllegalArgumentException) { Color(0xFFCCCCCC) }
                                    .copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onMedicationClicked(med.id) }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = med.name,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewLight() {
    AppTheme(themePreference = ThemeKeys.LIGHT) {
        val dummyUiState = CalendarUiState(
            selectedDate = LocalDate.now(),
            currentMonth = YearMonth.now(),
            visibleDays = (-3..3).map {
                val date = LocalDate.now().plusDays(it.toLong())
                CalendarDay(date, date.isEqual(LocalDate.now()), date.isEqual(LocalDate.now().plusDays(it.toLong())))
            },
            medicationSchedules = listOf(
                MedicationScheduleItem(
                    medication = Medication(id = 1, name = "Metformin", typeId = 1, color = "#FFE91E63", dosage = "500mg", packageSize = 30, remainingDoses = 15, startDate = "2023-01-10", endDate = "2023-01-25", reminderTime = null),
                    schedule = MedicationSchedule(
                        id = 1,
                        medicationId = 1,
                        scheduleType = ScheduleType.DAILY,
                        specificTimes = "09:00",
                        intervalHours = null,
                        intervalMinutes = null,
                        daysOfWeek = null,
                        intervalStartTime = null,
                        intervalEndTime = null
                    ),
                    startOffsetInVisibleDays = 0, endOffsetInVisibleDays = 2
                ),
                MedicationScheduleItem(
                    medication = Medication(
                        id = 2,
                        name = "Lisinopril Long Name For Testing Ellipsis",
                        typeId = 2,
                        color = "#FF4CAF50",
                        dosage = "10mg",
                        packageSize = 90,
                        remainingDoses = 80,
                        startDate = "2022-12-01",
                        endDate = null,
                        reminderTime = null
                    ),
                    schedule = MedicationSchedule(
                        id = 2,
                        medicationId = 2,
                        scheduleType = ScheduleType.DAILY,
                        specificTimes = "09:00",
                        intervalHours = null,
                        intervalMinutes = null,
                        daysOfWeek = null,
                        intervalStartTime = null,
                        intervalEndTime = null
                    ),
                    isOngoingOverall = true, startOffsetInVisibleDays = 0, endOffsetInVisibleDays = 6
                )
            )
        )
        val currentMonth = previewUiState.currentMonth
        Scaffold( // Preview call reverted
            topBar = {
                CalendarTopAppBar(currentMonth, {})
            }
        ) { paddings ->
            Column(Modifier.padding(paddings)) {
                WeekCalendarView( // Call reverted
                    days = previewUiState.visibleDays,
                    currentMonthFocus = previewUiState.currentMonth,
                    selectedDate = previewUiState.selectedDate,
                    onDateSelected = {},
                    listState = previewWeekScrollState // Restored
                )
                HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
                MedicationScheduleListView( // Call reverted
                    medicationSchedules = previewUiState.medicationSchedules,
                    numVisibleDays = previewUiState.visibleDays.size,
                    currentMonth = currentMonth,
                    onMedicationClicked = {},
                    horizontalScrollOffsetPx = previewHorizontalScrollOffsetPx, // Restored
                    dayWidth = previewDayWidth
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewDark() { // Preview reverted
     AppTheme(themePreference = ThemeKeys.DARK) {
        val previewWeekScrollState = rememberLazyListState()
        val previewDayWidth = 48.dp
        val previewHorizontalScrollOffsetPx = 0

        val previewUiState = CalendarUiState(
            selectedDate = LocalDate.now(),
            currentMonth = YearMonth.now(),
            visibleDays = (-3..3).map {
                val date = LocalDate.now().plusDays(it.toLong())
                CalendarDay(date, date.isEqual(LocalDate.now()), date.isEqual(LocalDate.now().plusDays(it.toLong())))
            },
            medicationSchedules = listOf(
                MedicationScheduleItem(
                    medication = Medication(
                        id = 1,
                        name = "Aspirin",
                        typeId = 3,
                        color = "#FF03A9F4",
                        dosage = "81mg",
                        packageSize = 100,
                        remainingDoses = 50,
                        startDate = "2023-01-01",
                        endDate = null,
                        reminderTime = null
                    ),
                    schedule = MedicationSchedule(
                        id = 3,
                        medicationId = 1,
                        scheduleType = ScheduleType.DAILY,
                        specificTimes = "09:00",
                        intervalHours = null,
                        intervalMinutes = null,
                        daysOfWeek = null,
                        intervalStartTime = null,
                        intervalEndTime = null
                    ),
                    isOngoingOverall = true, startOffsetInVisibleDays = 2, endOffsetInVisibleDays = 5
                )
            )
        )
        val currentMonth = previewUiState.currentMonth
        Scaffold( // Preview call reverted
            topBar = {
                CalendarTopAppBar(currentMonth, {})
            }
        ) { paddings ->
            Column(Modifier.padding(paddings)) {
                WeekCalendarView( // Call reverted
                    days = previewUiState.visibleDays,
                    currentMonthFocus = previewUiState.currentMonth,
                    selectedDate = previewUiState.selectedDate,
                    onDateSelected = {},
                    listState = previewWeekScrollState // Restored
                    )
                HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
                MedicationScheduleListView( // Call reverted
                    medicationSchedules = previewUiState.medicationSchedules,
                    numVisibleDays = previewUiState.visibleDays.size,
                    currentMonth = currentMonth,
                    onMedicationClicked = {},
                    horizontalScrollOffsetPx = previewHorizontalScrollOffsetPx, // Restored
                    dayWidth = previewDayWidth
                )
            }
        }
    }
}