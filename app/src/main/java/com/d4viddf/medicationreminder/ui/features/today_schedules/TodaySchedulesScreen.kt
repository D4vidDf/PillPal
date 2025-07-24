package com.d4viddf.medicationreminder.ui.features.today_schedules

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun TodaySchedulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: TodaySchedulesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(id = R.string.today_schedules_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(),
                        modifier = Modifier
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_arrow_back_ios_new_24),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                coroutineScope.launch {
                    val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    val sortedTimes = uiState.scheduleItems.keys.sorted()
                    val groupIndex = sortedTimes.indexOfFirst { it >= currentTime }

                    if (groupIndex != -1) {
                        var itemIndex = 0
                        for (i in 0 until groupIndex) {
                            itemIndex += 1 // For the header
                            itemIndex += uiState.scheduleItems[sortedTimes[i]]?.size ?: 0
                        }
                        listState.animateScrollToItem(itemIndex)
                    }
                }
            }) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = stringResource(id = R.string.scroll_to_current_time)
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            FilterControls(
                uiState = uiState,
                onMedicationFilterChanged = viewModel::onMedicationFilterChanged,
                onColorFilterChanged = viewModel::onColorFilterChanged,
                onTimeRangeFilterChanged = viewModel::onTimeRangeFilterChanged
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    ContainedLoadingIndicator(
                        modifier = Modifier.size(128.dp)
                    )
                } else if (uiState.scheduleItems.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp), // Padding for FAB
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.scheduleItems.forEach { (time, itemsForTime) ->
                            stickyHeader {
                                TimeHeader(time = time)
                            }
                            items(itemsForTime, key = { it.reminder.id }) { item ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    TodayScheduleItem(
                                        item = item,
                                        onMarkAsTaken = { /* TODO */ },
                                        onSkip = { /* TODO */ },
                                        onNavigateToDetails = { /* TODO */ },
                                        isFuture = try {
                                            LocalDateTime.parse(item.reminder.reminderTime)
                                                .isAfter(LocalDateTime.now())
                                        } catch (e: Exception) {
                                            false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterControls(
    uiState: TodaySchedulesViewModel.TodaySchedulesState,
    onMedicationFilterChanged: (Int?) -> Unit,
    onColorFilterChanged: (String?) -> Unit,
    onTimeRangeFilterChanged: (LocalTime?, LocalTime?) -> Unit,
) {
    var medicationMenuExpanded by remember { mutableStateOf(false) }
    var colorMenuExpanded by remember { mutableStateOf(false) }
    var showTimeRangeDialog by remember { mutableStateOf(false) }

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
        // Medication Filter
        item {
            Box {
                FilterChip(
                    selected = uiState.selectedMedicationId != null,
                    onClick = { medicationMenuExpanded = true },
                    label = {
                        Text(
                            uiState.allMedications.find { it.id == uiState.selectedMedicationId }?.name?.split(
                                " "
                            )?.first() ?: stringResource(id = R.string.filter_by_medication)
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null) }
                )
                DropdownMenu(
                    expanded = medicationMenuExpanded,
                    onDismissRequest = { medicationMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.all_medications)) },
                        onClick = {
                            onMedicationFilterChanged(null)
                            medicationMenuExpanded = false
                        }
                    )
                    uiState.allMedications.forEach { medication ->
                        DropdownMenuItem(
                            text = { Text(medication.name.split(" ").first()) },
                            onClick = {
                                onMedicationFilterChanged(medication.id)
                                medicationMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Color Filter
        item {
            Box {
                FilterChip(
                    selected = uiState.selectedColorName != null,
                    onClick = { colorMenuExpanded = true },
                    label = {
                        Text(
                            uiState.selectedColorName?.replace("_", " ")?.lowercase()
                                ?.replaceFirstChar { it.titlecase() }
                                ?: stringResource(id = R.string.filter_by_color))
                    },
                    leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) }
                )
                DropdownMenu(
                    expanded = colorMenuExpanded,
                    onDismissRequest = { colorMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.all_colors)) },
                        onClick = {
                            onColorFilterChanged(null)
                            colorMenuExpanded = false
                        }
                    )
                    MedicationColor.values().forEach { color ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(color.backgroundColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        color.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.titlecase() })
                                }
                            },
                            onClick = {
                                onColorFilterChanged(color.name)
                                colorMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Time Range Filter
        item {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val label = if (uiState.selectedTimeRange != null) {
                "${uiState.selectedTimeRange.start.format(formatter)} - ${
                    uiState.selectedTimeRange.endInclusive.format(
                        formatter
                    )
                }"
            } else {
                stringResource(id = R.string.filter_by_time_range)
            }
            InputChip(
                selected = uiState.selectedTimeRange != null,
                onClick = { showTimeRangeDialog = true },
                label = { Text(label) },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                trailingIcon = {
                    if (uiState.selectedTimeRange != null) {
                        IconButton(onClick = { onTimeRangeFilterChanged(null, null) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear time range filter"
                            )
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
        title = { Text(if (selectingStart) "Select Start Time" else "Select End Time") },
        text = {
            if (selectingStart) {
                TimePicker(state = startTimeState)
            } else {
                TimePicker(state = endTimeState)
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
                Text(if (selectingStart) "Next" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TimeHeader(time: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Medication,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.no_schedules_for_today),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}


// --- PREVIEW ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Today's Schedules Preview")
@Composable
fun TodaySchedulesScreenPreview() {
    val mockItems = mapOf(
        "08:00" to listOf(
            mockTodayScheduleUiItem(1, "Amoxicillin"),
            mockTodayScheduleUiItem(2, "Vitamin C", "LIGHT_YELLOW")
        ),
        "14:00" to listOf(
            mockTodayScheduleUiItem(3, "Ibuprofen", "LIGHT_RED")
        ),
        "22:00" to listOf(
            mockTodayScheduleUiItem(4, "Metformin", "LIGHT_BLUE")
        )
    )

    val previewState = TodaySchedulesViewModel.TodaySchedulesState(
        scheduleItems = mockItems,
        isLoading = false,
        allMedications = listOf(
            Medication(
                id = 1, name = "Amoxicillin",
                typeId = 2,
                color = "Orange",
                dosage = "1",
                packageSize = 12,
                remainingDoses = 2,
                startDate = null,
                endDate = null,
                reminderTime = null,
                registrationDate = null,
                nregistro = null
            ),
            Medication(
                id = 2, name = "Ibuprofen",
                typeId = 1,
                color = "Orange",
                dosage = "1",
                packageSize = 12,
                remainingDoses = 2,
                startDate = null,
                endDate = null,
                reminderTime = null,
                registrationDate = null,
                nregistro = null
            )
        )
    )

    AppTheme {
        // Re-implementing a simplified version of the screen for preview purposes
        // to avoid needing a full ViewModel instance.
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(id = R.string.today_schedules_title)) },
                    navigationIcon = {
                        IconButton(
                            onClick = {},
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(),
                            modifier = Modifier
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_arrow_back_ios_new_24),
                                contentDescription = stringResource(R.string.navigate_back),
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // In a real app, you would pass a real state object here
                // For preview, we can pass a dummy state.
                FilterControls(
                    uiState = previewState,
                    onMedicationFilterChanged = {},
                    onColorFilterChanged = {},
                    onTimeRangeFilterChanged = { _, _ -> }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    previewState.scheduleItems.forEach { (time, itemsForTime) ->
                        stickyHeader { TimeHeader(time = time) }
                        items(itemsForTime) { item ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                // You'll need a mock or a simplified version of TodayScheduleItem
                                // if it has complex dependencies.
                                // For now, we'll use a simple Text as a placeholder.
                                Text(
                                    text = "${item.medicationName} - ${item.medicationDosage}",
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun mockTodayScheduleUiItem(
    id: Int,
    name: String,
    color: String = "LIGHT_GREEN",
): TodayScheduleUiItem {
    val time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    return TodayScheduleUiItem(
        reminder = MedicationReminder(id, 100 + id, id, time, false, null, null),
        medicationName = name,
        medicationDosage = "1 tablet",
        medicationColorName = color,
        medicationIconUrl = null,
        medicationTypeName = "Tablet",
        formattedReminderTime = "08:00" // This would vary in real data
    )
}