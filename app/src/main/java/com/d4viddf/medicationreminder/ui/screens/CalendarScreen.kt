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
// import androidx.compose.foundation.layout.BoxWithConstraints // No longer needed in MedicationScheduleRow
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
    val weekCalendarScrollState = rememberLazyListState()
    val dayWidth = dayWidthForCalendar // Use the existing file-level constant

    // Calculate horizontalScrollOffsetPx in CalendarScreen
    val density = LocalDensity.current
    val horizontalScrollOffsetPx by remember(weekCalendarScrollState.firstVisibleItemIndex, weekCalendarScrollState.firstVisibleItemScrollOffset) {
        derivedStateOf {
            val dayWidthPx = with(density) { dayWidth.toPx() }
            ((weekCalendarScrollState.firstVisibleItemIndex * dayWidthPx) + weekCalendarScrollState.firstVisibleItemScrollOffset).roundToInt()
        }
    }

    // Define buffer for pagination
    val paginationBuffer = 15 // Number of items from the edge to trigger loading

    // LaunchedEffect for pagination logic, reacting to scroll state and data changes
    LaunchedEffect(weekCalendarScrollState, uiState.isLoading, uiState.visibleDays.size) {
        snapshotFlow { weekCalendarScrollState.layoutInfo }
            .collect { layoutInfo ->
                // Prevent unnecessary calls if data is empty, loading, or layout info is not ready
                if (uiState.visibleDays.isEmpty() || uiState.isLoading || layoutInfo.visibleItemsInfo.isEmpty()) {
                    return@collect
                }

                val firstVisibleItemIndex = layoutInfo.visibleItemsInfo.first().index
                // totalItemsCount should be based on the current list size in the UIState for consistency
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
                onDateSelected = { date -> viewModel.setSelectedDate(date) },
                listState = weekCalendarScrollState
                // uiState and viewModel removed, as pagination is handled in CalendarScreen
            )
            HorizontalDivider()
            MedicationScheduleListView(
                medicationSchedules = uiState.medicationSchedules,
                numVisibleDays = uiState.visibleDays.size,
                currentMonth = uiState.currentMonth, // Keep for now, might be removed if unused
                onMedicationClicked = { medicationId -> onNavigateToMedicationDetail(medicationId) },
                horizontalScrollOffsetPx = horizontalScrollOffsetPx, // NEW
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
    currentMonthFocus: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
    // uiState and viewModel parameters removed
) {
    // Effect to scroll to selected date initially or when days/selectedDate changes, attempting to center it.
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

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(days, key = { it.date.toEpochDay() }) { day ->
            Box(modifier = Modifier.width(dayWidthForCalendar)) {
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
    // weekViewScrollState: LazyListState, // Removed
    horizontalScrollOffsetPx: Int, // NEW
    dayWidth: Dp // NEW
) {
    Log.d("MedicationScheduleListView", "Received ${medicationSchedules.size} schedules. First: ${medicationSchedules.firstOrNull()?.medication?.name ?: "N/A"}. Offset: $horizontalScrollOffsetPx")
    // val density = LocalDensity.current // No longer needed here if dayWidthPx calculation is removed
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
                horizontalScrollOffsetPx = horizontalScrollOffsetPx, // Pass down
                dayWidth = dayWidth // Pass down
            )
            HorizontalDivider(thickness = 0.5.dp, color = DividerDefaults.color)
        }
    }
}

@Composable
fun MedicationScheduleRow(
    scheduleItem: MedicationScheduleItem,
    numVisibleDays: Int,
    dayWidth: Dp,
    horizontalScrollOffsetPx: Int,
    onClicked: () -> Unit
) {
    val density = LocalDensity.current // For Px conversions

    Log.d("MedScheduleRow", "Med: ${scheduleItem.medication.name}, " +
            "SO: ${scheduleItem.startOffsetInVisibleDays}, EO: ${scheduleItem.endOffsetInVisibleDays}, " +
            "dayWidthDp: $dayWidth, scrollOffsetPx: $horizontalScrollOffsetPx")

    Row(
        modifier = Modifier
            .fillMaxWidth() // Ensure this is present
            .clickable(onClick = onClicked)
            .padding(vertical = 2.dp)
            .clipToBounds(), // Ensure this is present
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (scheduleItem.startOffsetInVisibleDays != null && scheduleItem.endOffsetInVisibleDays != null) {
            val startOffset = scheduleItem.startOffsetInVisibleDays!!
            val endOffset = scheduleItem.endOffsetInVisibleDays!!

            val dayWidthPx = with(density) { dayWidth.toPx() }

            val barWidthDp = (endOffset - startOffset + 1) * dayWidth
            val barStartXpx = (startOffset * dayWidthPx) - horizontalScrollOffsetPx

            // Log these values (some logging is already there, ensure these are covered)
            Log.d("MedScheduleRowCalc", "Med: ${scheduleItem.medication.name}, " +
                    "dayWidthPx: $dayWidthPx, " +
                    "barWidthDp: $barWidthDp, barStartXpx_float: $barStartXpx, barStartXpx_rounded: ${barStartXpx.roundToInt()}, " +
                    "rawSO_px: ${startOffset * dayWidthPx}, HSO_px: $horizontalScrollOffsetPx")

            // Standard Box structure for ALL medications:
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = barStartXpx.roundToInt(), y = 0) } // Apply calculated offset
                    .width(barWidthDp) // Set calculated width
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
                    fontSize = 10.sp, // From previous change
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            // Existing Spacer for null offsets
            Log.d("MedScheduleRow", "Med: ${scheduleItem.medication.name} has null offsets, drawing spacer")
            Spacer(Modifier.height(32.dp).fillMaxWidth())
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewLight() {
    AppTheme(themePreference = ThemeKeys.LIGHT) {
        // For preview, we need to provide some mock values for the new parameters
        val previewWeekScrollState = rememberLazyListState() // Keep for WeekCalendarView
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
                    onDateSelected = {},
                    listState = previewWeekScrollState
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

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewDark() {
     AppTheme(themePreference = ThemeKeys.DARK) {
        val previewWeekScrollState = rememberLazyListState() // Keep for WeekCalendarView
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
                    onDateSelected = {},
                    listState = previewWeekScrollState
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
            // allowing it to be potentially wider than the screen if its content is.
            .clipToBounds()
            .clickable(onClick = onClicked)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x = -horizontalScrollOffsetPx, y = 0) }
        ) {
            if (numVisibleDays > 0 && scheduleItem.startOffsetInVisibleDays != null && scheduleItem.endOffsetInVisibleDays != null) {
                val startOffset = scheduleItem.startOffsetInVisibleDays!!
                val endOffset = scheduleItem.endOffsetInVisibleDays!!

                val barStartPaddingDp = startOffset * dayWidth
                val barWidthDp = (endOffset - startOffset + 1) * dayWidth

                val barColor = try {
                    Color(android.graphics.Color.parseColor(scheduleItem.medication.color ?: "#CCCCCC"))
                } catch (e: IllegalArgumentException) {
                    Color(0xFFCCCCCC)
                }
                Row {
                    Box(modifier = Modifier.width(barStartPaddingDp))
                    Box(
                        modifier = Modifier
                            .width(barWidthDp)
                            .height(32.dp) // Changed height
                            .clip(RoundedCornerShape(4.dp)) // Changed corner shape
                            .background(barColor.copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp), // Changed padding
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = scheduleItem.medication.name,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Optional: render something or a Spacer if offsets are null,
                // though VM should ideally always provide valid offsets for visible items.
                Spacer(Modifier.height(32.dp).fillMaxWidth()) // Placeholder, height matches the bar
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewLight() {
    AppTheme(themePreference = ThemeKeys.LIGHT) {
        // For preview, we need to provide some mock values for the new parameters
        val previewWeekScrollState = rememberLazyListState() // Keep for WeekCalendarView
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
                    onDateSelected = {},
                    listState = previewWeekScrollState
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

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewDark() {
     AppTheme(themePreference = ThemeKeys.DARK) {
        val previewWeekScrollState = rememberLazyListState() // Keep for WeekCalendarView
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
                    onDateSelected = {},
                    listState = previewWeekScrollState
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
