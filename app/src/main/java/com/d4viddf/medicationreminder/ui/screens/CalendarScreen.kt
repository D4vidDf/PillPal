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
// import androidx.compose.material.icons.filled.ArrowBackIosNew // Removed
// import androidx.compose.material.icons.filled.ArrowForwardIos // Removed
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
    val weekCalendarScrollState = rememberLazyListState() // Restored
    val dayWidth = dayWidthForCalendar // Use the existing file-level constant

    val density = LocalDensity.current // Restored
    val horizontalScrollOffsetPx by remember(weekCalendarScrollState.firstVisibleItemIndex, weekCalendarScrollState.firstVisibleItemScrollOffset) { // Restored
        derivedStateOf {
            val dayWidthPx = with(density) { dayWidth.toPx() }
            ((weekCalendarScrollState.firstVisibleItemIndex * dayWidthPx) + weekCalendarScrollState.firstVisibleItemScrollOffset).roundToInt()
        }
    }

    val paginationBuffer = 15 // Restored

    // Restored LaunchedEffect for pagination
    LaunchedEffect(weekCalendarScrollState, uiState.isLoading, uiState.visibleDays.size) {
        snapshotFlow { weekCalendarScrollState.layoutInfo }
            .collect { layoutInfo ->
                if (uiState.visibleDays.isEmpty() || uiState.isLoading || layoutInfo.visibleItemsInfo.isEmpty()) {
                    return@collect
                }
                val firstVisibleItemIndex = layoutInfo.visibleItemsInfo.first().index
                val totalItemsCount = uiState.visibleDays.size
                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.last().index

                // Log current state for easier debugging
                Log.d("CalendarScreen", "ScrollInfo: FirstVisible=$firstVisibleItemIndex, LastVisible=$lastVisibleItemIndex, TotalItems=$totalItemsCount, Buffer=$paginationBuffer, IsLoading=${uiState.isLoading}")

                // Load more past days if scrolled near the beginning
                if (!uiState.isLoading && firstVisibleItemIndex < paginationBuffer && totalItemsCount > 0) {
                    Log.d("CalendarScreen", "Triggering loadMorePastDays()")
                    viewModel.loadMorePastDays()
                }
                // Load more future days if scrolled near the end
                else if (!uiState.isLoading && lastVisibleItemIndex >= totalItemsCount - paginationBuffer && totalItemsCount > 0 && firstVisibleItemIndex < totalItemsCount -1 ) { // ensure not to trigger if already at very end due to small list
                    Log.d("CalendarScreen", "Triggering loadMoreFutureDays()")
                    viewModel.loadMoreFutureDays()
                }
            }
    }

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
            CalendarTopAppBar( // Call updated
                currentMonth = uiState.currentMonth,
                onDateSelectorClicked = { showDatePickerDialog = true }
                // onPreviousWeekClicked and onNextWeekClicked removed
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            WeekCalendarView( // Call updated
                days = uiState.visibleDays,
                currentMonthFocus = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                onDateSelected = { date -> viewModel.setSelectedDate(date) },
                listState = weekCalendarScrollState // Restored
            )
            HorizontalDivider()
            MedicationScheduleListView( // Call updated
                medicationSchedules = uiState.medicationSchedules,
                numVisibleDays = uiState.visibleDays.size,
                currentMonth = uiState.currentMonth,
                onMedicationClicked = { medicationId -> onNavigateToMedicationDetail(medicationId) },
                horizontalScrollOffsetPx = horizontalScrollOffsetPx, // Restored
                dayWidth = dayWidth
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
fun CalendarTopAppBar( // Signature reverted
    currentMonth: YearMonth,
    onDateSelectorClicked: () -> Unit
    // onPreviousWeekClicked and onNextWeekClicked removed
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
        // navigationIcon for Previous Week removed
        actions = {
            IconButton(onClick = onDateSelectorClicked) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = stringResource(R.string.select_date)
                )
            }
            // IconButton for Next Week removed
        }
    )
}

@Composable
fun WeekCalendarView( // Signature reverted
    days: List<CalendarDay>,
    currentMonthFocus: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    listState: LazyListState, // Restored
    modifier: Modifier = Modifier
) {
    // Restored LaunchedEffect for scrolling to center selectedDate
    LaunchedEffect(days, selectedDate, listState) {
        if (days.isNotEmpty() && listState.layoutInfo.totalItemsCount > 0) { // Ensure list is ready for scroll operations
            val selectedIndex = days.indexOfFirst { it.date == selectedDate }

            if (selectedIndex != -1) {
                val layoutInfo = listState.layoutInfo
                val visibleItemsCount = layoutInfo.visibleItemsInfo.size.let { if (it == 0) 7 else it } // Default to 7 if not yet calculated

                // Calculate the target index to center the selectedDate
                val targetScrollIndex = (selectedIndex - visibleItemsCount / 2)
                    .coerceIn(0, (days.size - 1).coerceAtLeast(0)) // Ensure bounds

                // Check if scrolling is necessary:
                // - If selected item is not currently visible.
                // - Or if it's visible but not reasonably centered.
                val currentFirstVisibleIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: -1
                val currentLastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1

                val isVisible = selectedIndex >= currentFirstVisibleIndex && selectedIndex <= currentLastVisibleIndex
                val isCenteredEnough = isVisible && kotlin.math.abs(selectedIndex - (currentFirstVisibleIndex + visibleItemsCount / 2)) <= 1

                if (!isVisible || !isCenteredEnough || layoutInfo.visibleItemsInfo.isEmpty()) { // Also scroll if layout is empty (initial setup)
                    try {
                        Log.d("WeekCalendarView", "Scrolling to center selectedDate. TargetIndex: $targetScrollIndex, SelectedIndex: $selectedIndex, VisibleCount: $visibleItemsCount")
                        listState.scrollToItem(index = targetScrollIndex)
                    } catch (e: Exception) {
                        Log.e("WeekCalendarView", "Error scrolling to selected item: $targetScrollIndex", e)
                    }
                }
            } else {
                // Fallback: If selectedDate is not in `days` (should ideally not happen if data is consistent),
                // scroll to the middle of the current list as a sensible default.
                val middleIndex = (days.size / 2).coerceIn(0, (days.size - 1).coerceAtLeast(0))
                try {
                    Log.d("WeekCalendarView", "SelectedDate not found in days. Scrolling to middle: $middleIndex")
                    listState.scrollToItem(index = middleIndex)
                } catch (e: Exception) {
                    Log.e("WeekCalendarView", "Error scrolling to middle item: $middleIndex", e)
                }
            }
        }
    }

    LazyRow( // Reverted to LazyRow
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Reverted padding
        horizontalArrangement = Arrangement.spacedBy(4.dp), // Reverted
        contentPadding = PaddingValues(horizontal = 4.dp) // Reverted
    ) {
        items(days, key = { it.date.toEpochDay() }) { day ->
            Box(modifier = Modifier.width(dayWidthForCalendar)) { // Reverted to fixed width Box
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
    horizontalScrollOffsetPx: Int, // Restored
    dayWidth: Dp
) {
    Log.d("MedicationScheduleListView", "Received ${medicationSchedules.size} schedules. First: ${medicationSchedules.firstOrNull()?.medication?.name ?: "N/A"}. Offset: $horizontalScrollOffsetPx") // Restored log
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

            MedicationScheduleRow( // Call updated
                scheduleItem = scheduleItem,
                numVisibleDays = numVisibleDays,
                onClicked = { onMedicationClicked(scheduleItem.medication.id) },
                horizontalScrollOffsetPx = horizontalScrollOffsetPx, // Restored
                dayWidth = dayWidth
            )
            HorizontalDivider(thickness = 0.5.dp, color = DividerDefaults.color)
        }
    }
}

@Composable
fun MedicationScheduleRow( // Signature reverted
    scheduleItem: MedicationScheduleItem,
    numVisibleDays: Int,
    dayWidth: Dp,
    horizontalScrollOffsetPx: Int, // Restored
    onClicked: () -> Unit
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) { // BoxWithConstraints still useful for parentRowWidthPx
        val parentRowWidthPx = with(density) { maxWidth.toPx() }
        // val actualDayWidthPx = parentRowWidthPx / 7.0f // Removed this, dayWidthPx from parameter is used

        Row(
            modifier = Modifier
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

                val dayWidthPx = with(density) { dayWidth.toPx() } // Restored from Dp parameter

                // barStartXpx calculation reverted
                val barStartXpx = (startOffset * dayWidthPx) - horizontalScrollOffsetPx

                val finalBarWidthDp: Dp

                if (scheduleItem.isOngoingOverall) {
                    // For ongoing items:
                    // The width in pixels should be such that the bar ends exactly at parentRowWidthPx.
                    // widthInPx = parentRowWidthPx (end edge) - barStartXpx (start edge)
                    val widthInPx = parentRowWidthPx - barStartXpx
                    finalBarWidthDp = with(density) { widthInPx.toDp() }.coerceAtLeast(0.dp)
                } else {
                    // For items with a defined end:
                    // Width calculation reverted
                    val daySpan = endOffset - startOffset + 1
                    finalBarWidthDp = (daySpan * dayWidth).coerceAtLeast(0.dp)
                }

                // Log the calculation details - reverted
                Log.d("MedScheduleRowCalc", "Med: ${scheduleItem.medication.name}, " +
                        "isOngoing: ${scheduleItem.isOngoingOverall}, " +
                        "SO_idx: $startOffset, EO_idx: $endOffset, " +
                        "dayWidthPx: $dayWidthPx, dayWidthDpParam: $dayWidth, " + // Using dayWidthPx from Dp
                        "HSO_px: $horizontalScrollOffsetPx, " + // Restored HSO
                        "barStartXpx_float: $barStartXpx, " +
                        "parentRowWidthPx: $parentRowWidthPx, " + // parentRowWidthPx still used for ongoing
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
fun CalendarScreenPreviewLight() { // Preview reverted
    AppTheme(themePreference = ThemeKeys.LIGHT) {
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