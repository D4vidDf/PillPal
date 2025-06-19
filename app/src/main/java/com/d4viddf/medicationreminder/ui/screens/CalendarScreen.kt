@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.d4viddf.medicationreminder.ui.screens


import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateContentSize // ADDED IMPORT
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.res.painterResource
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
import androidx.navigation.NavHostController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.data.ThemeKeys
import com.d4viddf.medicationreminder.ui.calendar.ScheduleCalendarState
import com.d4viddf.medicationreminder.ui.calendar.rememberScheduleCalendarState
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.screens.medication.MedicationDetailsScreen
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

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun CalendarScreen(
    navController: NavHostController,
    widthSizeClass: WindowWidthSizeClass,
    onNavigateBack: () -> Unit,
    onNavigateToMedicationDetail: (Int) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val calendarState = rememberScheduleCalendarState()
    val coroutineScope = rememberCoroutineScope()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Int?>()

    LaunchedEffect(scaffoldNavigator.currentDestination) {
        if (scaffoldNavigator.currentDestination?.contentKey == null && uiState.selectedMedicationId != null) {
            viewModel.setSelectedMedicationId(null)
        }
    }

    LaunchedEffect(navController.currentBackStackEntry?.savedStateHandle) {
        navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("medicationDetailClosed")?.let {
            if (it) {
                viewModel.setSelectedMedicationId(null)
                navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("medicationDetailClosed")
            }
        }
    }

    val dateCurrentlyAtCenter = calendarState.dateAtCenter

    val accessibilityDateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())
    }
    val accessibilityDateText by remember(dateCurrentlyAtCenter) {
        mutableStateOf("Selected day: ${dateCurrentlyAtCenter.format(accessibilityDateFormatter)}")
    }

    if (showDatePickerDialog) {
        ShowDatePicker(
            initialSelectedDate = uiState.selectedDate,
            onDateSelected = { newDate: LocalDate ->
                viewModel.setSelectedDate(newDate)
                coroutineScope.launch {
                    calendarState.scrollToDate(newDate, initialSnap = false)
                }
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    NavigableListDetailPaneScaffold(
        navigator = scaffoldNavigator,
        // paneExpansionState = null, // Parameter removed to use default internal state
        // paneExpansionDragHandle = null, // Parameter removed
        listPane = {
            AnimatedPane(
            ) {
                Scaffold(
                    topBar = {
                        CalendarTopAppBar(
                            currentMonth = YearMonth.from(dateCurrentlyAtCenter),
                            onDateSelectorClicked = { showDatePickerDialog = true }
                        )
                    }
                ) { innerPadding ->
                    Column(modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()) {
                        Text(
                            text = accessibilityDateText,
                            modifier = Modifier
                                .semantics { liveRegion = LiveRegionMode.Polite }
                                .alpha(0f)
                                .size(0.dp)
                        )
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(
                                    animationSpec = tween(
                                        durationMillis = 600,
                                        easing = LinearOutSlowInEasing
                                    )
                                )
                                .weight(1f)
                                .scrollable(
                                    state = calendarState.scrollableState,
                                    orientation = Orientation.Horizontal,
                                    flingBehavior = calendarState.scrollFlingBehavior
                                )
                        ) {
                            val totalWidthPx = constraints.maxWidth
                            var hasWidthForInitialScroll by remember { mutableStateOf(false) }

                            LaunchedEffect(totalWidthPx) {
                                if (totalWidthPx > 0) {
                                    Log.d(
                                        "CalendarScreen",
                                        "Updating calendarState view width to: $totalWidthPx"
                                    )
                                    calendarState.updateView(newWidth = totalWidthPx)
                                    if (!hasWidthForInitialScroll) {
                                        hasWidthForInitialScroll = true
                                    }
                                }
                            }
                            LaunchedEffect(hasWidthForInitialScroll) {
                                if (hasWidthForInitialScroll) {
                                    Log.d("CalendarScreen", "Performing initial scroll to today.")
                                    calendarState.scrollToDate(LocalDate.now(), initialSnap = true)
                                }
                            }
                            Column(
                                Modifier
                                    .fillMaxSize()
                            ) {
                                DaysRow(
                                    state = calendarState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(
                                            animationSpec = tween(
                                                durationMillis = 600,
                                                easing = LinearOutSlowInEasing
                                            )
                                        )
                                        .padding(bottom = 16.dp)
                                )
                                MedicationRowsLayout(
                                    state = calendarState,
                                    medicationSchedules = uiState.medicationSchedules,
                                    totalWidthPx = totalWidthPx,
                                    onMedicationClicked = { medicationId: Int ->
                                        viewModel.setSelectedMedicationId(medicationId)
                                        if (widthSizeClass == WindowWidthSizeClass.Compact) {
                                            onNavigateToMedicationDetail(medicationId)
                                        } else {
                                            coroutineScope.launch {
                                                scaffoldNavigator.navigateTo(
                                                    ListDetailPaneScaffoldRole.Detail,
                                                    medicationId
                                                )
                                            }
                                        }
                                    },
                                    selectedMedicationId = uiState.selectedMedicationId,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(
                                            animationSpec = tween(
                                                durationMillis = 600,
                                            )
                                        )
                                        .weight(1f)
                                        .padding(bottom = 16.dp)
                                )
                            }
                            val daysRowApproxHeight = 54.dp
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = daysRowApproxHeight + 8.dp)
                                    .size(8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        },
        detailPane = {
            val detailMedicationId = scaffoldNavigator.currentDestination?.contentKey

            AnimatedVisibility(
                visible = detailMedicationId != null && widthSizeClass != WindowWidthSizeClass.Compact,
                enter = slideInHorizontally { fullWidth -> fullWidth } + expandHorizontally(expandFrom = Alignment.End),
                exit = slideOutHorizontally { fullWidth -> fullWidth } + shrinkHorizontally(shrinkTowards = Alignment.End)
            ) {
                if (detailMedicationId != null) {
                    MedicationDetailsScreen(
                        medicationId = detailMedicationId,
                        navController = navController,
                        onNavigateBack = {
                            coroutineScope.launch { scaffoldNavigator.navigateBack() }
                        },
                        isHostedInPane = true,
                        widthSizeClass = widthSizeClass,
                        sharedTransitionScope = null,
                        animatedVisibilityScope = null,
                        onNavigateToAllSchedules = { medId, colorName ->
                            navController.navigate(Screen.AllSchedules.createRoute(medId, colorName, true))
                        },
                        onNavigateToMedicationHistory = { medId, colorName ->
                            navController.navigate(Screen.MedicationHistory.createRoute(medId, colorName))
                        },
                        onNavigateToMedicationGraph = { medId, colorName ->
                            navController.navigate(Screen.MedicationGraph.createRoute(medId, colorName))
                        },
                        onNavigateToMedicationInfo = { medId, colorName ->
                            navController.navigate(Screen.MedicationInfo.createRoute(medId, colorName))
                        }
                    )
                }
            }
        }
    )
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
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
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
        modifier = Modifier.animateContentSize(),
        title = {
            val monthYearString = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            val capitalizedMonthYearString = monthYearString.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            Text(
                text = capitalizedMonthYearString,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        actions = {
            IconButton(onClick = onDateSelectorClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar),
                    contentDescription = stringResource(R.string.select_date)
                )
            }
        }
    )
}

private data class DayData(val date: LocalDate) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@DayData
}

@Composable
private fun DaysRow(state: ScheduleCalendarState, modifier: Modifier = Modifier) {
    Layout(
        content = {
            val today = LocalDate.now()
            var currentDay = state.startDateTime.truncatedTo(ChronoUnit.DAYS)
            val endLoopAt = state.endDateTime.truncatedTo(ChronoUnit.DAYS).plusDays(1)
            var safetyCount = 0
            while (currentDay.isBefore(endLoopAt) && safetyCount < 100) {
                key(currentDay.toLocalDate().toEpochDay()) {
                    val localDate = currentDay.toLocalDate()
                    val isCurrentDay = localDate.isEqual(today)
                    Column(
                        modifier = Modifier
                            .animateContentSize(
                                animationSpec = tween(
                                    durationMillis = 600,
                                    easing = LinearOutSlowInEasing
                                )
                            )
                            .then(DayData(localDate))
                            .then(
                                if (isCurrentDay) Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                else Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentDay.format(DateTimeFormatter.ofPattern("E", Locale.getDefault())),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            color = if (isCurrentDay) MaterialTheme.colorScheme.onSecondaryContainer else LocalContentColor.current
                        )
                        Text(
                            text = currentDay.format(DateTimeFormatter.ofPattern("d", Locale.getDefault())),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            color = if (isCurrentDay) MaterialTheme.colorScheme.onSecondaryContainer else LocalContentColor.current
                        )
                    }
                }
                currentDay = currentDay.plusDays(1)
                safetyCount++
            }
        },
        modifier = modifier.animateContentSize(
            animationSpec = tween(
                durationMillis = 600,
                easing = LinearOutSlowInEasing
            )
        )
    ) { measurables, constraints ->
        val placeablesWithDate = measurables.mapNotNull { measurable ->
            val dayData = measurable.parentData as? DayData
            if (dayData != null) {
                Pair(measurable.measure(Constraints(maxHeight = constraints.maxHeight)), dayData.date)
            } else { null }
        }
        val rowHeight = placeablesWithDate.maxOfOrNull { it.first.height } ?: 0
        layout(constraints.maxWidth, rowHeight) {
            placeablesWithDate.forEach { pair ->
                val placeable = pair.first
                val date = pair.second
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
    selectedMedicationId: Int?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    LazyColumn(modifier = modifier) {
        items(medicationSchedules, key = { it.medication.id.toString() + "-" + it.schedule.id.toString() }) { scheduleItem ->
            val isSelected = selectedMedicationId != null && scheduleItem.medication.id == selectedMedicationId
            val boxModifier = if (isSelected) {
                Modifier
                    .fillParentMaxWidth()
                    .height(55.dp)
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            } else {
                Modifier
                    .fillParentMaxWidth()
                    .height(55.dp)
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            }
            Box(modifier = boxModifier) {
                val med = scheduleItem.medication
                val medStartDate = scheduleItem.actualStartDate
                val medEndDate = scheduleItem.actualEndDate
                val eventStartDateTime = medStartDate.atStartOfDay()
                val eventEndDateTime = if (scheduleItem.isOngoingOverall || medEndDate == null) {
                    state.endDateTime
                } else {
                    medEndDate.plusDays(1).atStartOfDay()
                }
                if (!(eventStartDateTime.isAfter(state.endDateTime) || eventEndDateTime.isBefore(state.startDateTime))) {
                    val (widthPx, offsetXpx) = state.widthAndOffsetForEvent(
                        start = eventStartDateTime,
                        end = eventEndDateTime,
                        totalWidthPx = totalWidthPx
                    )
                    if (widthPx > 0) {
                        val medColorString = med.color
                        val medicationEnumInstance = try {
                            if (medColorString.isEmpty()) null else MedicationColor.valueOf(medColorString)
                        } catch (e: IllegalArgumentException) { null }
                        val backgroundColor = medicationEnumInstance?.backgroundColor ?: Color(0xFFCCCCCC)
                        val textColor = medicationEnumInstance?.textColor ?: MaterialTheme.colorScheme.onSurface
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offsetXpx, 0) }
                                .width(with(density) { widthPx.toDp() })
                                .fillMaxHeight()
                                .background(
                                    backgroundColor,
                                    shape = RoundedCornerShape(percent = 50)
                                )
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable { onMedicationClicked(med.id) }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = med.name,
                                fontSize = 13.sp,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
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
        val calendarState = rememberScheduleCalendarState()
        val sampleMedication1 = Medication(id = 1, name = "Metformin", typeId = 1, color = MedicationColor.BLUE.toString(), dosage = "500mg", packageSize = 30, remainingDoses = 15, startDate = "2024-05-28", endDate = "2024-06-05", reminderTime = null, registrationDate = "2024-05-01")
        val sampleSchedule1 = MedicationSchedule(id = 1, medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = "09:00", intervalHours = null, intervalMinutes = null, daysOfWeek = null, intervalStartTime = null, intervalEndTime = null)
        val sampleMedication2 = Medication(id = 2, name = "Lisinopril (Ongoing)", typeId = 2, color = MedicationColor.ORANGE.toString(), dosage = "10mg", packageSize = 90, remainingDoses = 80, startDate = "2024-05-20", endDate = null, reminderTime = null, registrationDate = "2024-05-01")
        val sampleSchedule2 = MedicationSchedule(id = 2, medicationId = 2, scheduleType = ScheduleType.DAILY, specificTimes = "08:00", intervalHours = null, intervalMinutes = null, daysOfWeek = null, intervalStartTime = null, intervalEndTime = null)
        val previewMedicationSchedules = listOf(
            MedicationScheduleItem(medication = sampleMedication1, schedule = sampleSchedule1, actualStartDate = LocalDate.parse("2025-05-28"), actualEndDate = LocalDate.parse("2025-06-05"), isOngoingOverall = false),
            MedicationScheduleItem(medication = sampleMedication2, schedule = sampleSchedule2, actualStartDate = LocalDate.parse("2025-05-20"), actualEndDate = null, isOngoingOverall = true)
        )
        Scaffold(
            topBar = { CalendarTopAppBar(currentMonth = YearMonth.from(calendarState.startDateTime.toLocalDate()), onDateSelectorClicked = {}) }
        ) { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)) {
                    val totalWidthPx = constraints.maxWidth
                    LaunchedEffect(totalWidthPx) { if (totalWidthPx > 0) calendarState.updateView(newWidth = totalWidthPx) }
                    Column(Modifier.fillMaxSize()) {
                        DaysRow(state = calendarState, modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp))
                        MedicationRowsLayout(state = calendarState, medicationSchedules = previewMedicationSchedules, totalWidthPx = totalWidthPx, onMedicationClicked = {}, selectedMedicationId = 1, modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f))
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
        val calendarState = rememberScheduleCalendarState()
        val sampleMedication1 = Medication(id = 1, name = "Aspirin (Fixed)", typeId = 3, color = MedicationColor.LIGHT_PURPLE.toString(), dosage = "81mg", packageSize = 100, remainingDoses = 50, startDate = "2024-06-03", endDate = "2024-06-07", reminderTime = null, registrationDate = "2024-06-01")
            val sampleSchedule1 = MedicationSchedule(id = 3, medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = "07:00", intervalHours = null, intervalMinutes = null, daysOfWeek = null, intervalStartTime = null, intervalEndTime = null)
        val previewMedicationSchedules = listOf(MedicationScheduleItem(medication = sampleMedication1, schedule = sampleSchedule1, actualStartDate = LocalDate.parse("2025-06-03"), actualEndDate = LocalDate.parse("2025-06-07"), isOngoingOverall = false))
        Scaffold(
            topBar = { CalendarTopAppBar(currentMonth = YearMonth.from(calendarState.startDateTime.toLocalDate()), onDateSelectorClicked = {}) }
        ) { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)) {
                    val totalWidthPx = constraints.maxWidth
                    LaunchedEffect(totalWidthPx) { if (totalWidthPx > 0) calendarState.updateView(newWidth = totalWidthPx) }
                    Column(Modifier.fillMaxSize()) {
                        DaysRow(state = calendarState, modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp))
                        MedicationRowsLayout(state = calendarState, medicationSchedules = previewMedicationSchedules, totalWidthPx = totalWidthPx, onMedicationClicked = {}, selectedMedicationId = null, modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f))
                    }
                }
            }
        }
    }
}

