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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight // Ensure this is present
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow // Added
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip // Added
import androidx.compose.foundation.layout.BoxWithConstraints // Added
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit, // This is no longer used by CalendarTopAppBar
    onNavigateToMedicationDetail: (Int) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePickerDialog by remember { mutableStateOf(false) }

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
                currentMonth = uiState.currentMonth, // Title still uses currentMonth
                onDateSelectorClicked = { showDatePickerDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Replace MonthCalendarView with WeekCalendarView
            WeekCalendarView(
                days = uiState.visibleDays,
                currentMonthFocus = uiState.currentMonth, // Pass the month of the selectedDate
                selectedDate = uiState.selectedDate,
                onDateSelected = { date -> viewModel.setSelectedDate(date) }
            )
            Divider()
            MedicationScheduleListView(
                medicationSchedules = uiState.medicationSchedules,
                numVisibleDays = uiState.visibleDays.size, // Pass the size
                currentMonth = uiState.currentMonth,
                onMedicationClicked = { medicationId -> onNavigateToMedicationDetail(medicationId) }
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

// New WeekCalendarView Composable
@Composable
fun WeekCalendarView(
    days: List<CalendarDay>,
    currentMonthFocus: YearMonth, // Added: To pass to DayCell
    selectedDate: LocalDate, // Keep selectedDate to ensure DayCell can highlight correctly
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(days, selectedDate) {
        // Find the index of the selectedDate. If not found, it might be outside the current 3-week range
        // (shouldn't happen if generateVisibleDays is correct), or default to middle week start.
        val selectedIndex = days.indexOfFirst { it.date == selectedDate }.takeIf { it != -1 } ?: 7

        // Scroll to position to make the selected day visible,
        // typically by centering the week it's in.
        // For a 3-week view (21 days), the middle week starts at index 7.
        // We want to scroll so that this item is visible, potentially centered.
        // listState.scrollToItem(selectedIndex) might be enough if the item is brought into view.
        // For centering, it's more complex. A simpler approach is to scroll to the start of its week.
        val targetScrollIndex = (selectedIndex / 7) * 7 // Scroll to the start of the week containing selectedIndex
        if (targetScrollIndex >= 0 && targetScrollIndex < days.size) {
            listState.scrollToItem(targetScrollIndex)
        }
    }

    LazyRow(
        state = listState, // Use the state here
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(days, key = { it.date.toEpochDay() }) { day ->
            Box(modifier = Modifier.width(48.dp)) { // Fixed width for each day cell
                 DayCell(
                    day = day,
                    currentMonthFocus = currentMonthFocus, // Pass it down
                    onDateSelected = { onDateSelected(day.date) }
                )
            }
        }
    }
}

// MonthCalendarView was here, now removed.

@Composable
fun DayCell(
    day: CalendarDay,
    currentMonthFocus: YearMonth, // Added: To determine if day is in focus month
    onDateSelected: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f) // Keeps the cell square
            .background(
                color = if (day.isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .then(
                if (day.isToday && !day.isSelected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .clickable(onClick = onDateSelected)

    ) {
        val textColor = if (day.date.month != currentMonthFocus.month) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // Muted color for days not in focus month
        } else if (day.isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else if (day.isToday) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        Text(
            text = day.date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 12.sp,
            style = MaterialTheme.typography.bodyMedium
        )
        // Optionally, add day of week text here if needed for week view, e.g., "Mon"
        // Text(day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), fontSize = 8.sp)
    }
}


@Composable
fun MedicationScheduleListView(
    medicationSchedules: List<MedicationScheduleItem>,
    numVisibleDays: Int, // Added: To help rows calculate bar size
    currentMonth: YearMonth, // Kept for now, maybe useful for context or can be removed if not
    onMedicationClicked: (Int) -> Unit
) {
    if (medicationSchedules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_medications_scheduled_for_this_month))
        }
        return
    }

    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
        items(medicationSchedules, key = { it.medication.id.toString() + "-" + it.schedule.id.toString() }) { scheduleItem ->
            MedicationScheduleRow(
                scheduleItem = scheduleItem,
                numVisibleDays = numVisibleDays, // Pass this
                // currentMonth = currentMonth, // This might no longer be needed by the row itself
                onClicked = { onMedicationClicked(scheduleItem.medication.id) }
            )
            Divider(thickness = 0.5.dp) // Thinner divider
        }
    }
}

@Composable
fun MedicationScheduleRow(
    scheduleItem: MedicationScheduleItem,
    numVisibleDays: Int, // Number of days in the current week view
    onClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClicked)
            .padding(vertical = 2.dp), // Reduced vertical padding for a denser schedule look
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use BoxWithConstraints to get the available width for the schedule track
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp) // Height for each schedule track
        ) {
            if (numVisibleDays > 0 && scheduleItem.startOffsetInVisibleDays != null && scheduleItem.endOffsetInVisibleDays != null) {
                val dayWidth = maxWidth / numVisibleDays
                val startOffset = scheduleItem.startOffsetInVisibleDays!!
                val endOffset = scheduleItem.endOffsetInVisibleDays!!

                val barStartPadding = startOffset * dayWidth
                val barWidth = (endOffset - startOffset + 1) * dayWidth

                val barColor = try {
                    Color(android.graphics.Color.parseColor(scheduleItem.medication.color ?: "#CCCCCC"))
                } catch (e: IllegalArgumentException) {
                    Color(0xFFCCCCCC) // Fallback
                }

                Box(
                    modifier = Modifier
                        .padding(start = barStartPadding)
                        .width(barWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor.copy(alpha = 0.5f)) // Use medication color with some alpha
                        .padding(horizontal = 4.dp), // Padding inside the bar for text
                    contentAlignment = Alignment.CenterStart // Align text to the start of the bar
                ) {
                    Text(
                        text = scheduleItem.medication.name,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), // Make text readable
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewLight() {
    AppTheme(themePreference = ThemeKeys.LIGHT) {
        val previewUiState = com.d4viddf.medicationreminder.viewmodel.CalendarUiState(
            selectedDate = LocalDate.now(),
            currentMonth = YearMonth.now(),
            visibleDays = (-3..3).map {
                val date = LocalDate.now().plusDays(it.toLong())
                com.d4viddf.medicationreminder.viewmodel.CalendarDay(date, date.isEqual(LocalDate.now()), date.isEqual(LocalDate.now().plusDays(it.toLong())))
            },
            medicationSchedules = listOf(
                MedicationScheduleItem( // Example with offsets
                    medication = Medication(id = 1, name = "Metformin", typeId = 1, color = "#FFE91E63", dosage = "500mg", packageSize = 30, remainingDoses = 15, startDate = "2023-01-10", endDate = "2023-01-25", reminderTime = null),
                    schedule = MedicationSchedule(id = 1, medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = listOf("08:00")),
                    startOffsetInVisibleDays = 0, endOffsetInVisibleDays = 2 // Example: first 3 days of the 7 visible
                ),
                MedicationScheduleItem( // Example ongoing
                    medication = Medication(id = 2, name = "Lisinopril Long Name For Testing Ellipsis", typeId = 2, color = "#FF4CAF50", dosage = "10mg", packageSize = 90, remainingDoses = 80, startDate = "2022-12-01", endDate = null, reminderTime = null),
                    schedule = MedicationSchedule(id = 2, medicationId = 2, scheduleType = ScheduleType.DAILY, specificTimes = listOf("09:00")),
                    isOngoingOverall = true, startOffsetInVisibleDays = 0, endOffsetInVisibleDays = 6 // Spans all 7 visible days
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
                WeekCalendarView(previewUiState.visibleDays, previewUiState.currentMonth, previewUiState.selectedDate, {})
                Divider()
                MedicationScheduleListView(
                    medicationSchedules = previewUiState.medicationSchedules,
                    numVisibleDays = previewUiState.visibleDays.size, // Pass size for preview
                    currentMonth = currentMonth,
                    onMedicationClicked = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewDark() {
     AppTheme(themePreference = ThemeKeys.DARK) {
        val previewUiState = com.d4viddf.medicationreminder.viewmodel.CalendarUiState(
            selectedDate = LocalDate.now(),
            currentMonth = YearMonth.now(),
            visibleDays = (-3..3).map {
                val date = LocalDate.now().plusDays(it.toLong())
                com.d4viddf.medicationreminder.viewmodel.CalendarDay(date, date.isEqual(LocalDate.now()), date.isEqual(LocalDate.now().plusDays(it.toLong())))
            },
            medicationSchedules = listOf(
                MedicationScheduleItem( // Example that ends within the week
                    medication = Medication(id = 1, name = "Aspirin", typeId = 3, color = "#FF03A9F4", dosage = "81mg", packageSize = 100, remainingDoses = 50, startDate = "2023-01-01", endDate = null, reminderTime = null),
                    schedule = MedicationSchedule(id = 3, medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = listOf("07:00")),
                    isOngoingOverall = true, startOffsetInVisibleDays = 2, endOffsetInVisibleDays = 5 // Example: middle 4 days
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
                WeekCalendarView(previewUiState.visibleDays, previewUiState.currentMonth, previewUiState.selectedDate, {}) // Use WeekCalendarView
                Divider()
                MedicationScheduleListView(
                    medicationSchedules = previewUiState.medicationSchedules,
                    numVisibleDays = previewUiState.visibleDays.size, // Pass size for preview
                    currentMonth = currentMonth,
                    onMedicationClicked = {}
                )
            }
        }
    }
}
