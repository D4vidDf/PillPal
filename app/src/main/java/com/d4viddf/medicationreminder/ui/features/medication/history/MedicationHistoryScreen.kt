package com.d4viddf.medicationreminder.ui.features.medication.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.MedicationHistoryEntry
import com.d4viddf.medicationreminder.ui.features.medication.history.components.FilterControls
import com.d4viddf.medicationreminder.ui.features.medication.history.components.HistoryScheduleItem
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun MedicationHistoryScreen(
    medicationId: Int,
    colorName: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (Int) -> Unit,
    viewModel: MedicationHistoryViewModel? = hiltViewModel(), // Made nullable for preview
    selectedDate: String? = null,
    selectedMonth: String? = null
) {
    val medicationColor = remember(colorName) {
        try {
            MedicationColor.valueOf(colorName)
        } catch (e: IllegalArgumentException) {
            MedicationColor.LIGHT_ORANGE // Fallback
        }
    }

    val historyEntries by viewModel?.filteredAndSortedHistory?.collectAsState() ?: remember {
        mutableStateOf(emptyList())
    }
    val currentFilter by viewModel?.dateFilter?.collectAsState() ?: remember { mutableStateOf<Pair<LocalDate?, LocalDate?>?>(null) }
    val sortAscending by viewModel?.sortAscending?.collectAsState() ?: remember { mutableStateOf(false) }

    var showDateRangeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(medicationId, viewModel, selectedDate, selectedMonth) {
        var parsedSelectedDate: LocalDate? = null
        var parsedSelectedMonth: YearMonth? = null

        if (selectedDate != null && selectedDate.isNotBlank()) {
            try {
                parsedSelectedDate = LocalDate.parse(selectedDate)
            } catch (e: DateTimeParseException) {
                // Handle parse error
            }
        }

        if (selectedMonth != null && selectedMonth.isNotBlank()) {
            try {
                parsedSelectedMonth = YearMonth.parse(selectedMonth)
            } catch (e: DateTimeParseException) {
                // Handle parse error
            }
        }
        viewModel?.loadInitialHistory(medicationId, parsedSelectedDate, parsedSelectedMonth)
    }

    if (showDateRangeDialog) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = currentFilter?.first?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            initialSelectedEndDateMillis = currentFilter?.second?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= Instant.now().toEpochMilli()
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year <= LocalDate.now().year
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangeDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val startDateMillis = dateRangePickerState.selectedStartDateMillis
                        val endDateMillis = dateRangePickerState.selectedEndDateMillis
                        if (startDateMillis != null && endDateMillis != null) {
                            val startDate = Instant.ofEpochMilli(startDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                            val endDate = Instant.ofEpochMilli(endDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                            viewModel?.setDateFilter(startDate, endDate)
                        }
                        showDateRangeDialog = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = medicationColor.onBackgroundColor,
                        contentColor = medicationColor.cardColor
                    )
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDateRangeDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = medicationColor.onBackgroundColor,
                        contentColor = medicationColor.cardColor
                    )
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        ) {
            DateRangePicker(state = dateRangePickerState, title = null, headline = null, showModeToggle = true)
        }
    }

    AppTheme() {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
        Scaffold(
            modifier = Modifier,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.medHistory_screen_title)) },
                    navigationIcon = {
                        FilledTonalIconButton (onClick = onNavigateBack, shapes = IconButtonDefaults.shapes()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back)
                            )

                        }
                    },

                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                FilterControls(
                    sortAscending = sortAscending,
                    onSortOrderChange = { viewModel?.setSortOrder(it) },
                    onDateFilterSelected = { showDateRangeDialog = true },
                    isDateFilterActive = currentFilter != null,
                    onClearDateFilter = { viewModel?.clearDateFilter() }
                )
                if (historyEntries.isEmpty()) {
                    EmptyState(isFiltered = currentFilter != null)
                } else {
                    val groupedByMonth = historyEntries.groupBy {
                        it.originalDateTimeTaken.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedByMonth.forEach { (month, entries) ->
                            item {
                                Text(
                                    text = month.uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                            val groupedByDay = entries.groupBy { it.dateTaken }
                            items(groupedByDay.entries.toList(), key = { it.key }) { (day, dayEntries) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .animateItemPlacement(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Column {
                                        Text(
                                            text = day.format(DateTimeFormatter.ofPattern("EEEE, d")),
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        )
                                        dayEntries.forEach { entry ->
                                            HistoryScheduleItem(
                                                item = entry,
                                                onNavigateToDetails = { onNavigateToDetails(medicationId) },
                                                onTakenStatusChange = { isTaken ->
                                                    viewModel?.updateReminderStatus(entry.id, isTaken)
                                                },
                                                modifier = Modifier.padding(horizontal = 16.dp)
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
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyState(modifier: Modifier = Modifier, isFiltered: Boolean) {
    val message = if (isFiltered) {
        stringResource(R.string.no_reminders_that_match_the_filters)
    } else {
        stringResource(R.string.no_history_for_this_medication)
    }
    val icon = R.drawable.medication_filled

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

@Preview(showBackground = true, name = "Medication History Screen (Empty)")
@Composable
fun MedicationHistoryScreenPreview_Empty() {
    AppTheme {
        MedicationHistoryScreen(
            medicationId = 1,
            colorName = "LIGHT_BLUE",
            onNavigateBack = {},
            onNavigateToDetails = {},
            viewModel = null
        )
    }
}

@Preview(showBackground = true, name = "Medication History List Item")
@Composable
fun MedicationHistoryListItemPreview() {
    AppTheme {
        HistoryScheduleItem(
            item = MedicationHistoryEntry(
                id = "preview1",
                medicationName = "Sample Med",
                medicationDosage = "500mg",
                medicationColorName = "LIGHT_BLUE",
                medicationTypeName = "Tablet",
                dateTaken = LocalDate.now(),
                timeTaken = LocalTime.now(),
                originalDateTimeTaken = LocalDateTime.now(),
                isTaken = true
            ),
            onNavigateToDetails = {},
            onTakenStatusChange = {},
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}
