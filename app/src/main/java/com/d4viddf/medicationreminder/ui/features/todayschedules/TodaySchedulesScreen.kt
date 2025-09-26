package com.d4viddf.medicationreminder.ui.features.todayschedules

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.domain.usecase.ReminderCalculator
import com.d4viddf.medicationreminder.ui.features.todayschedules.components.ColorFilterBottomSheet
import com.d4viddf.medicationreminder.ui.features.todayschedules.components.MedicationFilterBottomSheet
import com.d4viddf.medicationreminder.ui.features.todayschedules.components.TodayScheduleItem
import com.d4viddf.medicationreminder.ui.features.todayschedules.components.TodaySchedulesSkeletonLoader
import com.d4viddf.medicationreminder.ui.features.todayschedules.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MaterialShapes
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TodaySchedulesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (medicationId: Int) -> Unit,
    viewModel: TodaySchedulesViewModel = hiltViewModel(),
) {
    // Collect all necessary states from the ViewModel
    val scheduleItems by viewModel.scheduleItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showMissed by viewModel.showMissed.collectAsState()
    val screenTitle = if (showMissed) stringResource(id = R.string.missed_reminders_title) else stringResource(id = R.string.todays_reminders_title)

    // States for the filter controls
    val allMedications by viewModel.allMedications.collectAsState()
    val selectedMedicationIds by viewModel.selectedMedicationIds.collectAsState()
    val selectedColorName by viewModel.selectedColorName.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val sortedTimes = scheduleItems.keys.toList()
    val nextTimeIndex = sortedTimes.indexOfFirst { it >= LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {FilledTonalIconButton (onClick = onNavigateBack, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back)
                    )

                }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (!showMissed && scheduleItems.isNotEmpty()) {
                val tooltipState = rememberTooltipState()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(id = R.string.scroll_to_current_time))
                        }
                    },
                    state = tooltipState
                ) {
                    FloatingActionButton(onClick = {
                        coroutineScope.launch {
                            if (nextTimeIndex != -1) {
                                listState.animateScrollToItem(nextTimeIndex)
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // ** RESTORED: Filters are only shown for the normal daily schedule **
            if (!showMissed) {
                FilterControls(
                    allMedications = allMedications,
                    selectedMedicationIds = selectedMedicationIds,
                    selectedColorName = selectedColorName,
                    selectedTimeRange = selectedTimeRange,
                    onMedicationFilterChanged = viewModel::onMedicationFilterChanged,
                    onColorFilterChanged = viewModel::onColorFilterChanged,
                    onTimeRangeFilterChanged = viewModel::onTimeRangeFilterChanged
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    TodaySchedulesSkeletonLoader()
                } else if (scheduleItems.isEmpty()) {
                    val isFiltered = selectedMedicationIds.isNotEmpty() || selectedColorName != null || selectedTimeRange != null
                    EmptyState(isMissedMode = showMissed, isFiltered = isFiltered)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            count = sortedTimes.size,
                            key = { index -> sortedTimes[index] }
                        ) { index ->
                            val time = sortedTimes[index]
                            val itemsForTime = scheduleItems[time].orEmpty()
                            val isNextCard = index == nextTimeIndex && !showMissed

                            TimeGroupCard(
                                time = time,
                                itemsForTime = itemsForTime,
                                isHighlighted = isNextCard,
                                onNavigateToDetails = onNavigateToDetails,
                                onMarkAsTaken = viewModel::updateReminderStatus,
                                onSkip = { /* Implement skip logic if needed */ }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeGroupCard(
    time: String,
    itemsForTime: List<TodayScheduleUiItem>,
    isHighlighted: Boolean,
    onMarkAsTaken: (MedicationReminder, Boolean) -> Unit,
    onSkip: (MedicationReminder) -> Unit,
    onNavigateToDetails: (medicationId: Int) -> Unit,
) {
    val cardColors = if (isHighlighted) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = cardColors
    ) {
        Column {
            TimeHeader(time = time)
            itemsForTime.forEach { item ->
                TodayScheduleItem(
                    item = item,
                    onMarkAsTaken = onMarkAsTaken,
                    onSkip = onSkip,
                    onNavigateToDetails = onNavigateToDetails,
                    isFuture = try {
                        LocalDateTime.parse(item.reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                            .isAfter(LocalDateTime.now())
                    } catch (e: Exception) {
                        false
                    }
                )
                if (itemsForTime.last() != item) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterControls(
    allMedications: List<Medication>,
    selectedMedicationIds: List<Int>,
    selectedColorName: String?,
    selectedTimeRange: ClosedRange<LocalTime>?,
    onMedicationFilterChanged: (List<Int>) -> Unit,
    onColorFilterChanged: (String?) -> Unit,
    onTimeRangeFilterChanged: (LocalTime?, LocalTime?) -> Unit,
) {
    var showMedicationFilter by remember { mutableStateOf(false) }
    var showColorFilter by remember { mutableStateOf(false) }
    var showTimeRangeDialog by remember { mutableStateOf(false) }

    if (showMedicationFilter) {
        MedicationFilterBottomSheet(
            allMedications = allMedications,
            selectedMedicationIds = selectedMedicationIds,
            onDismiss = { showMedicationFilter = false },
            onConfirm = {
                onMedicationFilterChanged(it)
                showMedicationFilter = false
            }
        )
    }

    if (showColorFilter) {
        ColorFilterBottomSheet(
            selectedColorName = selectedColorName,
            onDismiss = { showColorFilter = false },
            onConfirm = {
                onColorFilterChanged(it)
                showColorFilter = false
            }
        )
    }

    if (showTimeRangeDialog) {
        TimeRangePickerDialog(
            onDismiss = { showTimeRangeDialog = false },
            onConfirm = { start, end ->
                onTimeRangeFilterChanged(start, end)
                showTimeRangeDialog = false
            }
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            val medicationLabel = when {
                selectedMedicationIds.isEmpty() -> stringResource(id = R.string.filter_by_medication)
                selectedMedicationIds.size == 1 -> stringResource(id = R.string.filter_by_medication_singular)
                else -> stringResource(id = R.string.filter_by_medication_plural, selectedMedicationIds.size)
            }
            FilterChip(
                selected = selectedMedicationIds.isNotEmpty(),
                onClick = { showMedicationFilter = true },
                label = { Text(medicationLabel) },
                leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) }
            )
        }

        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            val colorResId = if (selectedColorName != null) {
                val resId = context.resources.getIdentifier("color_${selectedColorName.lowercase()}", "string", context.packageName)
                if (resId != 0) resId else R.string.filter_by_color
            } else {
                R.string.filter_by_color
            }
            FilterChip(
                selected = selectedColorName != null,
                onClick = { showColorFilter = true },
                label = { Text(stringResource(id = colorResId)) },
                leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) }
            )
        }

        item {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val timeLabel = if (selectedTimeRange != null) {
                "${selectedTimeRange.start.format(formatter)} - ${selectedTimeRange.endInclusive.format(formatter)}"
            } else {
                stringResource(id = R.string.filter_by_time_range)
            }
            FilterChip(
                selected = selectedTimeRange != null,
                onClick = { showTimeRangeDialog = true },
                label = { Text(timeLabel) },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                trailingIcon = {
                    if (selectedTimeRange != null) {
                        IconButton(onClick = { onTimeRangeFilterChanged(null, null) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.clear_time_range_filter))
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalTime, LocalTime) -> Unit,
) {
    val startTimeState = rememberTimePickerState(is24Hour = true)
    val endTimeState = rememberTimePickerState(is24Hour = true)
    var selectingStart by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectingStart) stringResource(id = R.string.select_start_time) else stringResource(id = R.string.select_end_time)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectingStart) {
                    TimePicker(state = startTimeState)
                } else {
                    TimePicker(state = endTimeState)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectingStart) {
                    selectingStart = false
                } else {
                    val startTime = LocalTime.of(startTimeState.hour, startTimeState.minute)
                    val endTime = LocalTime.of(endTimeState.hour, endTimeState.minute)
                    onConfirm(startTime, endTime)
                }
            }) {
                Text(if (selectingStart) stringResource(id = R.string.next) else stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun TimeHeader(time: String, modifier: Modifier = Modifier) {
    Text(
        text = time,
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, isMissedMode: Boolean, isFiltered: Boolean) {
    val message = when {
        isMissedMode -> stringResource(R.string.no_missed_reminders)
        isFiltered -> stringResource(R.string.no_reminders_for_today_filtered)
        else -> stringResource(R.string.no_reminders_for_today)
    }
    val icon = if(isMissedMode) R.drawable.task_alt else R.drawable.medication_filled

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(128.dp),
            shape = MaterialShapes.Pill.toShape(),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}