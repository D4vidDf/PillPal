package com.d4viddf.medicationreminder.ui.screens.medication

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // Added Box import
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar // Changed import
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState // Added
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // Added
import androidx.compose.ui.graphics.Color // Added import
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R // Moved import to top
import com.d4viddf.medicationreminder.data.MedicationHistoryEntry // Use new data class
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.components.ThemedAppBarBackButton
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme exists
import com.d4viddf.medicationreminder.ui.theme.MedicationSpecificTheme
import com.d4viddf.medicationreminder.viewmodel.MedicationHistoryViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Removed old placeholder data class MedicationHistoryEntry


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationHistoryScreen(
    medicationId: Int,
    colorName: String,
    onNavigateBack: () -> Unit,
    viewModel: MedicationHistoryViewModel? = hiltViewModel() // Made nullable for preview
) {
    val medicationColor = remember(colorName) {
        try {
            MedicationColor.valueOf(colorName)
        } catch (e: IllegalArgumentException) {
            MedicationColor.LIGHT_ORANGE // Fallback
        }
    }

    val medicationName by viewModel?.medicationName?.collectAsState() ?: remember { mutableStateOf( "Medication History (Preview)") }
    val historyEntries by viewModel?.filteredAndSortedHistory?.collectAsState() ?: remember {
        mutableStateOf(List(5) { index ->
            val time = LocalTime.now().minusHours(index.toLong())
            MedicationHistoryEntry(
                id = index.toString(),
                medicationName = "Sample Medication",
                dateTaken = LocalDate.now().minusDays(index.toLong()),
                timeTaken = time,
                originalDateTimeTaken = LocalDateTime.of(LocalDate.now().minusDays(index.toLong()), time)
            )
        })
    }
    val isLoading by viewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val error by viewModel?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    val currentFilter by viewModel?.dateFilter?.collectAsState() ?: remember { mutableStateOf<Pair<LocalDate?, LocalDate?>?>(null) }
    val sortAscending by viewModel?.sortAscending?.collectAsState() ?: remember { mutableStateOf(false) }

    LaunchedEffect(medicationId, viewModel) {
        viewModel?.loadInitialHistory(medicationId)
    }

    MedicationSpecificTheme(medicationColor = medicationColor) {
        Scaffold(
            topBar = {
                TopAppBar( // Changed back to TopAppBar
                    title = { Text(stringResource(id = R.string.medication_history_title)) },
                    navigationIcon = {
                        Box(modifier = Modifier.padding(start = 10.dp)) {
                            ThemedAppBarBackButton(onClick = onNavigateBack)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors( // Changed to topAppBarColors
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                        // scrolledContainerColor is not applicable here
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp)) // ADDED SPACER HERE
                FilterControls(
                    currentFilter = currentFilter,
                    onFilterChanged = { startDate, endDate ->
                        viewModel?.setDateFilter(startDate, endDate)
                    },
                    onClearDateFilter = { viewModel?.setDateFilter(null, null) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ActionControls(
                    sortAscending = sortAscending,
                    onSortOldestFirst = { viewModel?.setSortOrder(true) },
                    onSortNewestFirst = { viewModel?.setSortOrder(false) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp)) // This is a Material 3 Divider

                when {
                    isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    }
                    historyEntries.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(id = R.string.med_history_no_history_found),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(historyEntries, key = { it.id }) { entry ->
                                MedicationHistoryListItem(entry = entry)
                            }
                        }
                    }
                }
            } // Closes Column
        } // Closes Scaffold content lambda
    } // Closes MedicationSpecificTheme content lambda
} // Closes MedicationHistoryScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterControls(
    currentFilter: Pair<LocalDate?, LocalDate?>?,
    onFilterChanged: (startDate: LocalDate?, endDate: LocalDate?) -> Unit,
    onClearDateFilter: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    // DateRangePickerDialog implementation
    if (showDialog) {
        val state = rememberDateRangePickerState(
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
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startDateMillis = state.selectedStartDateMillis
                        val endDateMillis = state.selectedEndDateMillis
                        if (startDateMillis != null && endDateMillis != null) {
                            val startDate = Instant.ofEpochMilli(startDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                            val endDate = Instant.ofEpochMilli(endDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                            onFilterChanged(startDate, endDate)
                        }
                        showDialog = false
                    },
                    enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        ) {
            DateRangePicker(state = state, title = null, headline = null, showModeToggle = true)
        }
    } // Closes if (showDialog)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(id = R.string.med_history_filter_by_date_label), style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { showDialog = true },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Icon(Icons.Filled.CalendarToday, contentDescription = stringResource(id = R.string.med_history_filter_select_date_cd), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                currentFilter?.let {
                    val start = it.first?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) ?: "..."
                    val end = it.second?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) ?: "..."
                    "$start - $end"
                } ?: stringResource(id = R.string.med_history_filter_select_range_button),
                fontSize = 12.sp
            )
        }
    } // Closes Row

    if (currentFilter != null) {
        OutlinedButton(onClick = onClearDateFilter, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(id = R.string.med_history_filter_clear_button))
        }
        Spacer(modifier = Modifier.height(8.dp))
    } // Closes if (currentFilter != null)
}

@Composable
fun ActionControls(
    sortAscending: Boolean,
    onSortOldestFirst: () -> Unit,
    onSortNewestFirst: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val oldestFirstButtonColors = if (sortAscending) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer // Explicitly set
            )
        } else {
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary // Keep explicit for unselected
            )
        }
        OutlinedButton(
            onClick = onSortOldestFirst,
            shape = RoundedCornerShape(8.dp),
            colors = oldestFirstButtonColors
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.med_history_action_sort_oldest_first), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.med_history_action_sort_oldest_first), fontSize = 12.sp)
        }

        val newestFirstButtonColors = if (!sortAscending) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer // Explicitly set
            )
        } else {
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary // Keep explicit for unselected
            )
        }
        OutlinedButton(
            onClick = onSortNewestFirst,
            shape = RoundedCornerShape(8.dp),
            colors = newestFirstButtonColors
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.med_history_action_sort_newest_first), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.med_history_action_sort_newest_first), fontSize = 12.sp)
        }
    }
}

@Composable
fun MedicationHistoryListItem(entry: MedicationHistoryEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer // Changed
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = entry.dateTaken.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer // Added/Changed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.med_history_item_taken_at_prefix) + entry.timeTaken.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer // Changed
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Medication History Screen (Loading)")
@Composable
fun MedicationHistoryScreenPreview_Loading() {
    AppTheme {
        MedicationHistoryScreen(
            medicationId = 1,
            colorName = "LIGHT_BLUE",
            onNavigateBack = {},
            viewModel = null
        )
    }
}

@Preview(showBackground = true, name = "Medication History List Item")
@Composable
fun MedicationHistoryListItemPreview() {
    AppTheme {
        MedicationHistoryListItem(
            entry = MedicationHistoryEntry(
                id = "preview1",
                medicationName = "Sample Med",
                dateTaken = LocalDate.now(),
                timeTaken = LocalTime.now(),
                originalDateTimeTaken = LocalDateTime.now()
            )
        )
    }
}
