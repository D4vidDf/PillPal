@file:OptIn(ExperimentalSharedTransitionApi::class) // Moved OptIn to file-level
package com.d4viddf.medicationreminder.ui.screens

// Icons for TopAppBar were removed, but CalendarToday is still needed

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
// import androidx.compose.animation.LocalSharedTransitionScope // To be removed
import androidx.compose.animation.SharedTransitionScope // Added import
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.data.ThemeKeys
import com.d4viddf.medicationreminder.ui.calendar.ScheduleCalendarState
import com.d4viddf.medicationreminder.ui.calendar.rememberScheduleCalendarState
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.viewmodel.CalendarViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleItem
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// Constant for DayCell width, used in CalendarScreen and Previews - may become obsolete or used differently
private val dayWidthForCalendar = 48.dp

@OptIn(ExperimentalMaterial3Api::class) // Removed ExperimentalSharedTransitionApi from here
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMedicationDetail: (Int) -> Unit,
    // sharedTransitionScope: SharedTransitionScope?, // Add this // REMOVED
    // animatedVisibilityScope: AnimatedVisibilityScope?, // Make nullable // REMOVED
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
    val coroutineScope = rememberCoroutineScope() // Added: remember a coroutine scope

    // Collect dateAtCenter from calendarState
    // Note: Since dateAtCenter itself is a State<LocalDate> (due to by derivedStateOf),
    // direct access .value is not needed if passed to another Composable that can take State,
    // but for deriving YearMonth here, .value is appropriate if not using another collectAsState.
    // However, derivedStateOf is already a State, so we can use its value directly.
    val dateCurrentlyAtCenter = calendarState.dateAtCenter // This is already a LocalDate due to `by derivedStateOf`

    // New: For accessibility announcement of the centered day
    val accessibilityDateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())
    }
    val accessibilityDateText by remember(dateCurrentlyAtCenter) {
        // Use derivedStateOf or just remember to recompute when dateCurrentlyAtCenter changes.
        // Simple remember is fine as dateCurrentlyAtCenter is already a State.
        mutableStateOf("Selected day: ${dateCurrentlyAtCenter.format(accessibilityDateFormatter)}")
    }

    if (showDatePickerDialog) {
        ShowDatePicker(
            initialSelectedDate = uiState.selectedDate, // This might need to point to calendarState's current date
            onDateSelected = { newDate ->
                // viewModel.setSelectedDate(newDate) // Keep this if it serves other purposes or if ViewModel needs to know
                // For now, the primary action is to scroll the calendarState.
                // If viewModel.setSelectedDate also triggers UI changes that
                // depend on the selected date, it should be kept.
                // Let's assume it's still needed for now.
                viewModel.setSelectedDate(newDate)


                coroutineScope.launch { // Added: launch a coroutine
                    calendarState.scrollToDate(newDate)
                }
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }


    Scaffold(
        topBar = {
            CalendarTopAppBar(
                // currentMonth was YearMonth.from(calendarState.startDateTime.toLocalDate())
                // Change it to use the month of the date at the center of the view:
                currentMonth = YearMonth.from(dateCurrentlyAtCenter),
                onDateSelectorClicked = { showDatePickerDialog = true }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // Add the invisible Text for screen reader announcements here
            Text(
                text = accessibilityDateText,
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite } // Corrected usage
                    .alpha(0f) // Make it invisible
                    .size(0.dp) // Ensure it takes no space
            )

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
                val totalWidthPx = constraints.maxWidth // This is an Int
                // Keep the existing LaunchedEffect key as constraints.maxWidth or a more stable key if needed.
                // A more robust key for one-time initialization might be `Unit` or a specific remembered boolean flag.
                // However, reacting to `totalWidthPx > 0` is also a common pattern.
                // Let's refine this to ensure it runs once when width is properly available.

                var hasWidthForInitialScroll by remember { mutableStateOf(false) }

                // This LaunchedEffect updates the calendarState's view width whenever it changes.
                LaunchedEffect(totalWidthPx) {
                    if (totalWidthPx > 0) {
                        Log.d("CalendarScreen", "Updating calendarState view width to: $totalWidthPx")
                        calendarState.updateView(newWidth = totalWidthPx)
                        if (!hasWidthForInitialScroll) { // Check if we haven't marked width as ready for initial scroll
                            hasWidthForInitialScroll = true // Mark it as ready
                        }
                    }
                }

                // This LaunchedEffect performs the initial scroll to today.
                // It runs once when hasWidthForInitialScroll becomes true.
                LaunchedEffect(hasWidthForInitialScroll) {
                    if (hasWidthForInitialScroll) {
                        Log.d("CalendarScreen", "Performing initial scroll to today.")
                        // coroutineScope.launch { // LaunchedEffect provides its own CoroutineScope
                        calendarState.scrollToDate(LocalDate.now())
                        // }
                    }
                }
                Column(Modifier.fillMaxSize()) { // This Column allows stacking DaysRow and later MedicationRows
                    DaysRow(state = calendarState, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) // Changed
                    MedicationRowsLayout(
                        state = calendarState,
                        medicationSchedules = uiState.medicationSchedules,
                        totalWidthPx = totalWidthPx,
                        onMedicationClicked = { medicationId -> onNavigateToMedicationDetail(medicationId) },
                            // sharedTransitionScope = sharedTransitionScope, // Pass this // REMOVED
                            // animatedVisibilityScope = animatedVisibilityScope, // Pass it here // REMOVED
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }

                // Centering Guide - REMOVE THE OLD LINE (if it was here as a separate Box)
                // New Dot Indicator
                // Positioned to be visually below where DaysRow renders, and horizontally centered in the viewport.
                val daysRowApproxHeight = 54.dp // Estimate for DaysRow visual height
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter) // Horizontally centered within BoxWithConstraints
                        .padding(top = daysRowApproxHeight + 8.dp) // Positioned below the estimated DaysRow area + spacing
                        .size(8.dp) // Size of the dot
                        .background(MaterialTheme.colorScheme.primary, CircleShape) // Dot's color and shape
                )
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
            val monthYearString = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            val capitalizedMonthYearString = monthYearString.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            Text(
                text = capitalizedMonthYearString,
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
            val today = LocalDate.now() // Added
            var currentDay = state.startDateTime.truncatedTo(ChronoUnit.DAYS)
            val endLoopAt = state.endDateTime.truncatedTo(ChronoUnit.DAYS).plusDays(1) // Corrected: end is exclusive for loop
            var safetyCount = 0 // Safety break for loop
            while (currentDay.isBefore(endLoopAt) && safetyCount < 100) {
                key(currentDay.toLocalDate().toEpochDay()) {
                    val localDate = currentDay.toLocalDate() // Added
                    val isCurrentDay = localDate.isEqual(today) // Added

                    Column(
                        modifier = Modifier
                            .then(DayData(localDate))
                            .then(
                                if (isCurrentDay) Modifier
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                else Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text( // Day of week
                            text = currentDay.format(DateTimeFormatter.ofPattern("E", Locale.getDefault())),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            color = if (isCurrentDay) MaterialTheme.colorScheme.onSecondaryContainer else LocalContentColor.current // Added color
                        )
                        Text( // Day number
                            text = currentDay.format(DateTimeFormatter.ofPattern("d", Locale.getDefault())),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            color = if (isCurrentDay) MaterialTheme.colorScheme.onSecondaryContainer else LocalContentColor.current // Added color
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
    // sharedTransitionScope: SharedTransitionScope?, // Add this // REMOVED
    // animatedVisibilityScope: AnimatedVisibilityScope?, // Make nullable // REMOVED
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Removed: val sharedTransitionScope = LocalSharedTransitionScope.current

    LazyColumn(modifier = modifier) {
        items(medicationSchedules, key = { it.medication.id.toString() + "-" + it.schedule.id.toString() }) { scheduleItem ->
            Box(modifier = Modifier.fillParentMaxWidth().height(55.dp).padding(vertical = 4.dp, horizontal = 8.dp)) { // Changed height and vertical padding
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
                        val medColorString = med.color // Expected to be an enum name like "ORANGE"
                        val medicationEnumInstance = try {
                            if (medColorString.isEmpty()) {
                                Log.w("CalendarScreen", "Medication color string is null or empty for medication ${med.name}. Using default colors.")
                                null
                            } else {
                                MedicationColor.valueOf(medColorString)
                            }
                        } catch (e: IllegalArgumentException) {
                            Log.w("CalendarScreen", "Invalid color name: '$medColorString' for medication ${med.name}. Using default colors. Error: ${e.message}")
                            null
                        }

                        val backgroundColor = medicationEnumInstance?.backgroundColor ?: Color(0xFFCCCCCC) // Default fallback for background
                        val textColor = medicationEnumInstance?.textColor ?: MaterialTheme.colorScheme.onSurface // Default fallback for text

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offsetXpx, 0) }
                                .width(with(density) { widthPx.toDp() })
                                .fillMaxHeight()
                                .background(
                                    backgroundColor,
                                    shape = RoundedCornerShape(percent = 50) // Changed
                                )
                                .clip(RoundedCornerShape(percent = 50)) // Changed
                                .clickable { onMedicationClicked(med.id) }
                                // REMOVED .then(...) for sharedElement (background)
                                .padding(horizontal = 12.dp, vertical = 4.dp), // Changed
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = med.name,
                                fontSize = 13.sp, // Changed fontSize
                                color = textColor, // Apply the resolved textColor
                                fontWeight = FontWeight.Bold, // Added this line
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier // REMOVED .then(...) for sharedElement (name)
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
    AppTheme(themePreference = ThemeKeys.LIGHT) { // Assuming AppTheme is correctly defined
        // 1. Create a remembered ScheduleCalendarState instance for the preview
        val calendarState = rememberScheduleCalendarState(
            // Optional: Provide a specific referenceDateTime if needed for consistent previews
            // referenceDateTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        )

        // Mock data for MedicationScheduleItem
        val sampleMedication1 = Medication(id = 1, name = "Metformin", typeId = 1, color = MedicationColor.BLUE.toString(), dosage = "500mg", packageSize = 30, remainingDoses = 15, startDate = "2024-05-28", endDate = "2024-06-05", reminderTime = null, registrationDate = "2024-05-01")
        val sampleSchedule1 = MedicationSchedule(
            id = 1, medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = "09:00",
            intervalHours = null,
            intervalMinutes = null,
            daysOfWeek = null,
            intervalStartTime = null,
            intervalEndTime = null
        )

        val sampleMedication2 = Medication(id = 2, name = "Lisinopril (Ongoing)", typeId = 2, color = MedicationColor.ORANGE.toString(), dosage = "10mg", packageSize = 90, remainingDoses = 80, startDate = "2024-05-20", endDate = null, reminderTime = null, registrationDate = "2024-05-01")
        val sampleSchedule2 = MedicationSchedule(
            id = 2, medicationId = 2, scheduleType = ScheduleType.DAILY, specificTimes = "08:00",
            intervalHours = null,
            intervalMinutes = null,
            daysOfWeek = null,
            intervalStartTime = null,
            intervalEndTime = null
        )

        val previewMedicationSchedules = listOf(
            MedicationScheduleItem(
                medication = sampleMedication1,
                schedule = sampleSchedule1,
                actualStartDate = LocalDate.parse("2025-05-28"),
                actualEndDate = LocalDate.parse("2025-06-05"),
                isOngoingOverall = false // Explicitly false as endDate is present
            ),
            MedicationScheduleItem(
                medication = sampleMedication2,
                schedule = sampleSchedule2,
                actualStartDate = LocalDate.parse("2025-05-20"),
                actualEndDate = null, // No actual end date
                isOngoingOverall = true  // Explicitly true
            )
        )

        // Mimic the structure of CalendarScreen's Scaffold content
        Scaffold(
            topBar = {
                CalendarTopAppBar(
                    currentMonth = YearMonth.from(calendarState.startDateTime.toLocalDate()),
                    onDateSelectorClicked = { /* Dummy action for preview */ }
                    // No prev/next week buttons
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                    // Note: .scrollable is stateful and might be hard to preview perfectly.
                    // For preview, we often just show a static snapshot.
                    // If .scrollable causes issues in preview, it can be omitted here.
                ) {
                    val totalWidthPx = constraints.maxWidth
                    // Call updateView for the state, though its effect might be limited in preview
                    LaunchedEffect(totalWidthPx) {
                        if (totalWidthPx > 0) {
                            calendarState.updateView(newWidth = totalWidthPx)
                        }
                    }

                    Column(Modifier.fillMaxSize()) {
                        DaysRow(
                            state = calendarState,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp) // Consistent height defined in DaysRow
                        )
                        MedicationRowsLayout(
                            state = calendarState,
                            medicationSchedules = previewMedicationSchedules,
                            totalWidthPx = totalWidthPx,
                            onMedicationClicked = { /* Dummy action */ },
                            // sharedTransitionScope = null, // Preview // REMOVED
                            // animatedVisibilityScope = null, // Preview // REMOVED
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// CalendarScreenPreviewDark should be identical except for `AppTheme(themePreference = ThemeKeys.DARK)`
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewDark() {
    AppTheme(themePreference = ThemeKeys.DARK) {
        val calendarState = rememberScheduleCalendarState()

        val sampleMedication1 = Medication(id = 1, name = "Aspirin (Fixed)", typeId = 3, color = MedicationColor.LIGHT_PURPLE.toString(), dosage = "81mg", packageSize = 100, remainingDoses = 50, startDate = "2024-06-03", endDate = "2024-06-07", reminderTime = null, registrationDate = "2024-06-01")
        val sampleSchedule1 = MedicationSchedule(
            id = 3, medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = "07:00",
            intervalHours = null,
            intervalMinutes = null,
            daysOfWeek = null,
            intervalStartTime = null,
            intervalEndTime = null
        )

        val previewMedicationSchedules = listOf(
            MedicationScheduleItem(
                medication = sampleMedication1,
                schedule = sampleSchedule1,
                actualStartDate = LocalDate.parse("2025-06-03"),
                actualEndDate = LocalDate.parse("2025-06-07"),
                isOngoingOverall = false
            )
        )

        Scaffold(
            topBar = {
                CalendarTopAppBar(
                    currentMonth = YearMonth.from(calendarState.startDateTime.toLocalDate()),
                    onDateSelectorClicked = { }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val totalWidthPx = constraints.maxWidth
                    LaunchedEffect(totalWidthPx) {
                        if (totalWidthPx > 0) {
                            calendarState.updateView(newWidth = totalWidthPx)
                        }
                    }

                    Column(Modifier.fillMaxSize()) {
                        DaysRow(
                            state = calendarState,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        MedicationRowsLayout(
                            state = calendarState,
                            medicationSchedules = previewMedicationSchedules,
                            totalWidthPx = totalWidthPx,
                            onMedicationClicked = { },
                            // sharedTransitionScope = null, // Preview // REMOVED
                            // animatedVisibilityScope = null, // Preview // REMOVED
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
            }
        }
    }
}
// Removed old preview content that used WeekCalendarView, MedicationScheduleListView, etc.
// The new previews above use ScheduleCalendarState, DaysRow, and MedicationRowsLayout.