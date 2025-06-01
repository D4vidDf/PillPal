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
import androidx.compose.ui.platform.LocalDensity // Ensured
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration // Already present, ensure it stays
import androidx.compose.ui.text.font.FontWeight // Ensure this is present
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow // Added
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip // Added
import androidx.compose.foundation.layout.BoxWithConstraints // Add this import
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt // Ensure this is present
import android.util.Log // Ensure this is present
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset // Added for offset
import androidx.compose.ui.unit.times
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.ThemeKeys
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.viewmodel.CalendarDay
import com.d4viddf.medicationreminder.viewmodel.CalendarViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.viewmodel.CalendarUiState

// Constant for DayCell width, used in CalendarScreen and Previews
private val dayWidthForCalendar = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMedicationDetail: (Int) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePickerDialog by remember { mutableStateOf(false) }
    // val weekCalendarScrollState = rememberLazyListState() // Removed
    val dayWidth = dayWidthForCalendar // Use the existing file-level constant
    val horizontalScrollOffsetPx = 0 // Fixed to 0 as scrolling is removed

    // val density = LocalDensity.current // No longer needed for horizontalScrollOffsetPx here
    // horizontalScrollOffsetPx calculation removed

    // val paginationBuffer = 15 // Removed

    if (showDatePickerDialog) {
        ShowDatePicker(
            initialSelectedDate = uiState.selectedDate,
            onDateSelected = { newDate ->
                viewModel.setSelectedDate(newDate)
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    Scaffold(
        topBar = {
            CalendarTopAppBar(
                currentMonth = uiState.currentMonth,
                onDateSelectorClicked = { showDatePickerDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            WeekCalendarView(
                days = uiState.visibleDays,
                currentMonthFocus = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                onDateSelected = { date -> viewModel.setSelectedDate(date) }
                // listState removed
            )
            HorizontalDivider()
            MedicationScheduleListView(
                medicationSchedules = uiState.medicationSchedules,
                numVisibleDays = uiState.visibleDays.size,
                currentMonth = uiState.currentMonth, // Keep for now, might be removed if unused
                onMedicationClicked = { medicationId -> onNavigateToMedicationDetail(medicationId) },
                // horizontalScrollOffsetPx = horizontalScrollOffsetPx, // Removed
                dayWidth = dayWidth // NEW (using the one defined in CalendarScreen)
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTopAppBar(
    currentMonth: YearMonth,
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

@Composable
fun WeekCalendarView(
    days: List<CalendarDay>,
    currentMonthFocus: YearMonth, // Keep this for DayCell styling
    selectedDate: LocalDate,    // Keep this for DayCell styling
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // LaunchedEffect for scrolling removed
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp), // Adjusted padding
        horizontalArrangement = Arrangement.SpaceAround // Distributes space evenly
    ) {
        // Ensure days list is indeed 7 days, or handle gracefully if not (though ViewModel should ensure it)
        days.forEach { day ->
            // Each DayCell will effectively get an equal portion of the width.
            // The DayCell itself might need internal adjustments if it relied on a fixed parent Box width.
            // For now, assume DayCell's internal Modifier.fillMaxWidth() will adapt to the space given by SpaceAround.
            // Alternatively, wrap DayCell in Box(Modifier.weight(1f))
            Box(modifier = Modifier.weight(1f)) { // Ensure even distribution
                 DayCell(
                    day = day,
                    currentMonthFocus = currentMonthFocus,
                    onDateSelected = { onDateSelected(day.date) }
                )
            }
        }
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    currentMonthFocus: YearMonth,
    onDateSelected: () -> Unit
) {
    // Changed Box to Column to arrange day name and number vertically
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth() // Fill the width provided by parent Box(dayWidthForCalendar) in WeekCalendarView
            .aspectRatio(0.75f) // Make cell taller than wide to fit day name + number
            .background(
                color = if (day.isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (day.isToday && !day.isSelected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .clickable(onClick = onDateSelected)
            .padding(vertical = 4.dp) // Add some overall vertical padding
    ) {
        val isOutOfFocusMonth = day.date.month != currentMonthFocus.month

        // Day Name (e.g., Mon)
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            fontSize = 10.sp,
            color = if (isOutOfFocusMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), // Slightly less prominent
            textAlign = TextAlign.Center
        )

        val dayNumberTextColor = if (isOutOfFocusMonth) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        } else if (day.isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else if (day.isToday) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        // Day Number
        Text(
            text = day.date.dayOfMonth.toString(),
            color = dayNumberTextColor,
            fontSize = 12.sp,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (day.isSelected || (day.isToday && !isOutOfFocusMonth)) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MedicationScheduleListView(
    medicationSchedules: List<MedicationScheduleItem>,
    numVisibleDays: Int,
    currentMonth: YearMonth,
    onMedicationClicked: (Int) -> Unit,
    // horizontalScrollOffsetPx: Int, // Removed
    dayWidth: Dp
) {
    Log.d("MedicationScheduleListView", "Received ${medicationSchedules.size} schedules. First: ${medicationSchedules.firstOrNull()?.medication?.name ?: "N/A"}.") // Offset removed
    // val density = LocalDensity.current // No longer needed here
    if (medicationSchedules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_medications_scheduled_for_this_month))
        }
        return
    }

    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
        items(medicationSchedules, key = { it.medication.id.toString() + "-" + it.schedule.id.toString() }) { scheduleItem ->
            // val dayWidthPx = with(density) { dayWidth.toPx() } // Not needed here
            // val currentScrollOffsetPx = remember(weekViewScrollState.firstVisibleItemIndex, weekViewScrollState.firstVisibleItemScrollOffset) { // Not needed here
            //     ((weekViewScrollState.firstVisibleItemIndex * dayWidthPx) + weekViewScrollState.firstVisibleItemScrollOffset).roundToInt()
            // }

            MedicationScheduleRow(
                scheduleItem = scheduleItem,
                numVisibleDays = numVisibleDays, // This might become less relevant
                onClicked = { onMedicationClicked(scheduleItem.medication.id) },
                // horizontalScrollOffsetPx = horizontalScrollOffsetPx, // Removed
                dayWidth = dayWidth // Pass down
            )
            HorizontalDivider(thickness = 0.5.dp, color = DividerDefaults.color)
        }
    }
}

@Composable
fun MedicationScheduleRow(
    scheduleItem: MedicationScheduleItem,
    numVisibleDays: Int, // This parameter is not used in the logic below but is kept for signature consistency
    dayWidth: Dp,
    // horizontalScrollOffsetPx: Int, // Removed
    onClicked: () -> Unit
) {
    val density = LocalDensity.current

    // Use BoxWithConstraints to get the actual available width for the row content.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val parentRowWidthPx = with(density) { maxWidth.toPx() }

        Row(
            modifier = Modifier
                // .fillMaxWidth() // Not needed here as BoxWithConstraints handles it
                .clickable(onClick = onClicked)
                .padding(vertical = 2.dp) // Keep consistent padding
                .clipToBounds(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (scheduleItem.startOffsetInVisibleDays != null && scheduleItem.endOffsetInVisibleDays != null) {
                val startOffset = scheduleItem.startOffsetInVisibleDays!!
                // endOffset is not used for width calculation of ongoing items.
                // For non-ongoing items, it's used as before.
                val endOffset = scheduleItem.endOffsetInVisibleDays!!

                val dayWidthPx = with(density) { dayWidth.toPx() }

                // barStartXpx is the calculated starting X pixel of the medication bar,
                // relative to the start of the parentRow. Can be negative.
                val barStartXpx = startOffset * dayWidthPx // horizontalScrollOffsetPx removed

                val finalBarWidthDp: Dp

                if (scheduleItem.isOngoingOverall) {
                    // For ongoing items:
                    // The width in pixels should be such that the bar ends exactly at parentRowWidthPx.
                    // widthInPx = parentRowWidthPx (end edge) - barStartXpx (start edge)
                    val widthInPx = parentRowWidthPx - barStartXpx
                    finalBarWidthDp = with(density) { widthInPx.toDp() }.coerceAtLeast(0.dp)
                } else {
                    // For items with a defined end:
                    // Width is based on the number of days it spans.
                    val daySpan = endOffset - startOffset + 1
                    finalBarWidthDp = (daySpan * dayWidth).coerceAtLeast(0.dp)
                }

                // Log the calculation details
                Log.d("MedScheduleRowCalc", "Med: ${scheduleItem.medication.name}, " +
                        "isOngoing: ${scheduleItem.isOngoingOverall}, " +
                        "SO_idx: $startOffset, EO_idx: $endOffset, " + // Clarified SO/EO are indices
                        "dayWidthPx: $dayWidthPx, dayWidthDp: $dayWidth, " +
                        // "HSO_px: $horizontalScrollOffsetPx, " + // Removed HSO
                        "barStartXpx_float: $barStartXpx, " + // This is now rawSO_px effectively
                        "parentRowWidthPx: $parentRowWidthPx, " +
                        "finalBarWidthDp: $finalBarWidthDp")

                if (finalBarWidthDp > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = barStartXpx.roundToInt(), y = 0) }
                            .width(finalBarWidthDp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                try { Color(android.graphics.Color.parseColor(scheduleItem.medication.color ?: "#CCCCCC")) }
                                catch (e: IllegalArgumentException) { Color(0xFFCCCCCC) }
                                .copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = scheduleItem.medication.name,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // If calculated width is zero or negative, draw nothing or a minimal spacer
                    // to maintain row height and prevent visual collapse.
                    Spacer(Modifier.height(32.dp)) // Keep consistent height if it's an empty part of a row
                                                 // If the whole row is effectively empty due to this item,
                                                 // it might still take up padding space.
                                                 // If fillMaxWidth was here, it would ensure it takes space.
                                                 // For now, just a spacer of the same height.
                }
            } else {
                // This case is when start/end offsets are null (item not in range as per ViewModel)
                Log.d("MedScheduleRow", "Med: ${scheduleItem.medication.name} has null offsets, drawing full-width spacer")
                Spacer(Modifier.height(32.dp).fillMaxWidth()) // Keep full width spacer for items completely out of range
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewLight() {
    AppTheme(themePreference = ThemeKeys.LIGHT) {
        // For preview, we need to provide some mock values for the new parameters
        // val previewWeekScrollState = rememberLazyListState() // Removed
        val previewDayWidth = 48.dp // This might be less relevant for WeekCalendarView directly now
        val previewHorizontalScrollOffsetPx = 0 // Fixed for preview

        val previewUiState = CalendarUiState(
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
        Scaffold(
            topBar = {
                CalendarTopAppBar(currentMonth, {})
            }
        ) { paddings ->
            Column(Modifier.padding(paddings)) {
                WeekCalendarView(
                    days = previewUiState.visibleDays,
                    currentMonthFocus = previewUiState.currentMonth,
                    selectedDate = previewUiState.selectedDate,
                    onDateSelected = {}
                    // listState removed
                )
                HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
                MedicationScheduleListView(
                    medicationSchedules = previewUiState.medicationSchedules,
                    numVisibleDays = previewUiState.visibleDays.size,
                    currentMonth = currentMonth,
                    onMedicationClicked = {},
                    // weekViewScrollState = previewWeekScrollState, // Removed
                    // horizontalScrollOffsetPx = previewHorizontalScrollOffsetPx, // Removed
                    dayWidth = previewDayWidth
                    // horizontalScrollOffsetPx = previewHorizontalScrollOffsetPx, // Removed
                    dayWidth = previewDayWidth
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewDark() {
     AppTheme(themePreference = ThemeKeys.DARK) {
        // val previewWeekScrollState = rememberLazyListState() // Removed
        val previewDayWidth = 48.dp // This might be less relevant for WeekCalendarView directly now
        val previewHorizontalScrollOffsetPx = 0 // Fixed for preview

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
        Scaffold(
            topBar = {
                CalendarTopAppBar(currentMonth, {})
            }
        ) { paddings ->
            Column(Modifier.padding(paddings)) {
                WeekCalendarView(
                    days = previewUiState.visibleDays,
                    currentMonthFocus = previewUiState.currentMonth,
                    selectedDate = previewUiState.selectedDate,
                    onDateSelected = {}
                    // listState removed
                    )
                HorizontalDivider(thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
                MedicationScheduleListView(
                    medicationSchedules = previewUiState.medicationSchedules,
                    numVisibleDays = previewUiState.visibleDays.size,
                    currentMonth = currentMonth,
                    onMedicationClicked = {},
                    // weekViewScrollState = previewWeekScrollState, // Removed
                    horizontalScrollOffsetPx = previewHorizontalScrollOffsetPx, // Added for preview
                    dayWidth = previewDayWidth // Added for preview
                )
            }
        }
    }
}