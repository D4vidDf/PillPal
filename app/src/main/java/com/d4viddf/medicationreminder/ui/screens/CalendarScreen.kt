package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // Keep for MedicationScheduleListView
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState // Added
import androidx.compose.foundation.horizontalScroll // Added
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity // Added for Px conversion
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration // Added for screen width
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp // Added for type hint
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt // Added for scroll offset
import android.util.Log // Added for logging
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMedicationDetail: (Int) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val dayWidth = 48.dp
    val horizontalScrollState = rememberScrollState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

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

    // Effect for snapping when user scrolling stops
    LaunchedEffect(horizontalScrollState.isScrollInProgress) {
        if (!horizontalScrollState.isScrollInProgress && uiState.visibleDays.isNotEmpty()) {
            val dayWidthPx = with(density) { dayWidth.toPx() }
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val currentOffsetPx = horizontalScrollState.value

            val closestDayIndex = (currentOffsetPx / dayWidthPx).roundToInt().coerceIn(0, uiState.visibleDays.size - 1)

            // Calculate the centered offset for the closest day
            var targetCenteredSnapOffsetPx = (closestDayIndex * dayWidthPx) - (screenWidthPx / 2) + (dayWidthPx / 2)
            val maxScrollPx = horizontalScrollState.maxValue.toFloat()
            targetCenteredSnapOffsetPx = targetCenteredSnapOffsetPx.coerceIn(0f, maxScrollPx)
            val finalSnapOffset = targetCenteredSnapOffsetPx.roundToInt()

            if (kotlin.math.abs(currentOffsetPx - finalSnapOffset) > 1) { // Only animate if not already very close
                horizontalScrollState.animateScrollTo(finalSnapOffset)
            }

            // Update selected date based on the snapped day index
            val newSelectedDate = uiState.visibleDays[closestDayIndex].date
            if (newSelectedDate != uiState.selectedDate) {
                viewModel.setSelectedDate(newSelectedDate)
            }
        }
    }
    Scaffold(
        topBar = {
            CalendarTopAppBar(
                currentMonth = uiState.currentMonth,
                onDateSelectorClicked = { showDatePickerDialog = true }
            )
        }
    ) { innerPadding ->
        Column( // Main screen column
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box( // This Box will handle the horizontal scrolling
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Box( // This inner Box defines the total width of the scrollable content
                    modifier = Modifier.width(uiState.visibleDays.size * dayWidth)
                ) {
                    Column { // This Column stacks WeekCalendarView and MedicationScheduleListView
                        WeekCalendarView(
                            days = uiState.visibleDays,
                            currentMonthFocus = uiState.currentMonth,
                            selectedDate = uiState.selectedDate,
                            onDateSelected = { date -> viewModel.setSelectedDate(date) },
                            dayWidth = dayWidth
                        )
                        HorizontalDivider()
                        MedicationScheduleListView(
                            medicationSchedules = uiState.medicationSchedules,
                            numVisibleDays = uiState.visibleDays.size,
                            dayWidth = dayWidth,
                            currentMonth = uiState.currentMonth,
                            onMedicationClicked = { medicationId -> onNavigateToMedicationDetail(medicationId) }
                        )
                    }
                }
            }
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
    dayWidth: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth() // This Row will be as wide as its parent allows (which is total scrollable width)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), // Spacing between DayCells
        // No contentPadding needed here as the parent Box handles overall width
    ) {
        days.forEach { day ->
            DayCell(
                day = day,
                currentMonthFocus = currentMonthFocus,
                onDateSelected = { onDateSelected(day.date) },
                modifier = Modifier.width(dayWidth) // Apply dayWidth here
            )
        }
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    currentMonthFocus: YearMonth,
    onDateSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .aspectRatio(0.7f)
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
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            fontSize = 10.sp,
            color = if (day.date.month != currentMonthFocus.month) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            }
        )
        Spacer(modifier = Modifier.height(2.dp))
        val textColor = if (day.date.month != currentMonthFocus.month) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (day.isSelected || (day.isToday && day.date.month == currentMonthFocus.month)) FontWeight.Bold else FontWeight.Normal
        )
    }
}


@Composable
fun MedicationScheduleListView(
    medicationSchedules: List<MedicationScheduleItem>,
    numVisibleDays: Int,
    dayWidth: Dp,
    currentMonth: YearMonth,
    onMedicationClicked: (Int) -> Unit
) {
    if (medicationSchedules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_medications_scheduled_for_this_month))
        }
        return
    }
    // This LazyColumn is inside a horizontally scrolling parent.
    // Its width should ideally be constrained or matched to the parent's scrollable content width.
    // However, LazyColumn itself is vertically scrolling.
    // For now, relying on the outer Box's width definition.
    LazyColumn(modifier = Modifier.padding(horizontal = 4.dp)) { // Use small padding to align with DayCell spacing
        items(medicationSchedules, key = { it.medication.id.toString() + "-" + it.schedule.id.toString() }) { scheduleItem ->
            MedicationScheduleRow(
                scheduleItem = scheduleItem,
                numVisibleDays = numVisibleDays,
                dayWidth = dayWidth,
                onClicked = { onMedicationClicked(scheduleItem.medication.id) }
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
    onClicked: () -> Unit
) {
    Log.d("MedicationScheduleRow", "Rendering item: ${scheduleItem.medication.name}, " +
            "startOff: ${scheduleItem.startOffsetInVisibleDays}, " +
            "endOff: ${scheduleItem.endOffsetInVisibleDays}, " +
            "numVisibleDays: $numVisibleDays, " +
            "dayWidth: $dayWidth"
    )

    val density = LocalDensity.current
    Row( // This Row will be as wide as its parent allows (total scrollable width)
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClicked)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BoxWithConstraints is removed, calculations are now based on passed dayWidth
        // The parent of this Row is effectively the total width of all days (e.g., 21 * 48.dp)
        // So, this Row will also occupy that full width.
        // The actual bar is then placed within this Row using padding.
        if (numVisibleDays > 0 && scheduleItem.startOffsetInVisibleDays != null && scheduleItem.endOffsetInVisibleDays != null) {
            val startOffset = scheduleItem.startOffsetInVisibleDays!!
            val endOffset = scheduleItem.endOffsetInVisibleDays!!

            val barStartPaddingDp = startOffset * dayWidth
            val barWidthDp = (endOffset - startOffset + 1) * dayWidth

            Log.d("MedicationScheduleRow", "Med: ${scheduleItem.medication.name}, " +
                    "barStartPaddingDp: $barStartPaddingDp, barWidthDp: $barWidthDp"
            )

            val barColor = try {
                Color(android.graphics.Color.parseColor(scheduleItem.medication.color ?: "#CCCCCC"))
            } catch (e: IllegalArgumentException) {
                Color(0xFFCCCCCC)
            }

            Box( // This box is a spacer to push the actual bar to the correct start position
                modifier = Modifier.width(barStartPaddingDp)
            )
            Box( // This is the actual medication bar
                modifier = Modifier
                    .width(barWidthDp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(barColor.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp),
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
        } else {
            // Optional: Placeholder or empty space if schedule item has no valid offsets
            // This ensures the Row still takes up space if needed, or it can be an Empty composable
            Spacer(modifier = Modifier.height(48.dp).fillMaxWidth())
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewLight() {
    AppTheme(themePreference = ThemeKeys.LIGHT) {
        // For previews, we construct a viewModel that provides a static state,
        // or we pass a manually constructed UiState to a dumb CalendarScreenContent composable.
        // Calling CalendarScreen directly relies on Hilt and default ViewModel state which is empty for previews.
        // For better preview, let's mock the UiState directly for the content part.

        val dayWidth = 48.dp
        val visibleDaysExample = (-10..10).map { // 21 days
            val date = LocalDate.now().plusDays(it.toLong())
            CalendarDay(date, date.isEqual(LocalDate.now()), date.isEqual(date))
        }

        val previewUiState = CalendarUiState(
            selectedDate = LocalDate.now(),
            currentMonth = YearMonth.now(),
            visibleDays = visibleDaysExample,
            medicationSchedules = listOf(
                MedicationScheduleItem(
                    medication = Medication(id = 1, name = "Metformin", typeId = 1, color = "#FFE91E63", dosage = "500mg", packageSize = 30, remainingDoses = 15, startDate = "2023-01-10", endDate = "2023-01-25", reminderTime = null),
                    schedule = MedicationSchedule(id = 1, medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = "09:00"),
                    startOffsetInVisibleDays = 7, endOffsetInVisibleDays = 9 // Example: middle week, 3 days
                ),
                MedicationScheduleItem(
                    medication = Medication(id = 2, name = "Lisinopril", typeId = 2, color = "#FF4CAF50", dosage = "10mg", packageSize = 90, remainingDoses = 80, startDate = "2022-12-01", endDate = null),
                    schedule = MedicationSchedule(id = 2, medicationId = 2, scheduleType = ScheduleType.DAILY, specificTimes = "09:00"),
                    isOngoingOverall = true, startOffsetInVisibleDays = 0, endOffsetInVisibleDays = 20 // Spans all visible days
                )
            )
        )
        // This is a simplified preview focusing on the scrollable content structure
        Column {
            CalendarTopAppBar(currentMonth = previewUiState.currentMonth, onDateSelectorClicked = {})
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Box(modifier = Modifier.width(previewUiState.visibleDays.size * dayWidth)) {
                    Column {
                        WeekCalendarView(
                            days = previewUiState.visibleDays,
                            currentMonthFocus = previewUiState.currentMonth,
                            selectedDate = previewUiState.selectedDate,
                            onDateSelected = {},
                            dayWidth = dayWidth
                        )
                        HorizontalDivider()
                        MedicationScheduleListView(
                            medicationSchedules = previewUiState.medicationSchedules,
                            numVisibleDays = previewUiState.visibleDays.size,
                            dayWidth = dayWidth,
                            currentMonth = previewUiState.currentMonth,
                            onMedicationClicked = {}
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewDark() {
     AppTheme(themePreference = ThemeKeys.DARK) {
        val dayWidth = 48.dp
        val visibleDaysExample = (-10..10).map { // 21 days
            val date = LocalDate.now().plusDays(it.toLong())
            CalendarDay(date, date.isEqual(LocalDate.now().plusDays(1)), date.isEqual(date))
        }
        val previewUiState = CalendarUiState(
            selectedDate = LocalDate.now().plusDays(1),
            currentMonth = YearMonth.from(LocalDate.now().plusDays(1)),
            visibleDays = visibleDaysExample,
            medicationSchedules = listOf(
                MedicationScheduleItem(
                    medication = Medication(id = 1, name = "Aspirin", typeId = 3, color = "#FF03A9F4", dosage = "81mg", packageSize = 100, remainingDoses = 50, startDate = "2023-01-01", endDate = null),
                    schedule = MedicationSchedule(id = 3, medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = "09:00"),
                    isOngoingOverall = true, startOffsetInVisibleDays = 5, endOffsetInVisibleDays = 15
                )
            )
        )
        Column {
            CalendarTopAppBar(currentMonth = previewUiState.currentMonth, onDateSelectorClicked = {})
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Box(modifier = Modifier.width(previewUiState.visibleDays.size * dayWidth)) {
                     Column {
                        WeekCalendarView(
                            days = previewUiState.visibleDays,
                            currentMonthFocus = previewUiState.currentMonth,
                            selectedDate = previewUiState.selectedDate,
                            onDateSelected = {},
                            dayWidth = dayWidth
                        )
                        HorizontalDivider()
                        MedicationScheduleListView(
                            medicationSchedules = previewUiState.medicationSchedules,
                            numVisibleDays = previewUiState.visibleDays.size,
                            dayWidth = dayWidth,
                            currentMonth = previewUiState.currentMonth,
                            onMedicationClicked = {}
                        )
                    }
                }
            }
        }
    }
}
