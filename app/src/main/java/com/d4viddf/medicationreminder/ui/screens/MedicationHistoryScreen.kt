package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // Added Box import
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn // Added for DateRangePicker
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState // For scroll control
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState // For scroll control
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog // Added
import androidx.compose.material3.DateRangePicker // Added
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton // Added
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState // Added
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState // Added
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope // Added
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // Added
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R // Moved import to top
import com.d4viddf.medicationreminder.data.MedicationHistoryEntry // Use new data class
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme exists
import com.d4viddf.medicationreminder.viewmodel.MedicationHistoryViewModel
import java.time.Instant // Added
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId // Added
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

// Removed old placeholder data class MedicationHistoryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationHistoryScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MedicationHistoryViewModel? = hiltViewModel() // Made nullable for preview
) {
    val medicationName by viewModel?.medicationName?.collectAsState() ?: remember { mutableStateOf( "Medication History (Preview)") }
    val historyEntries by viewModel?.filteredAndSortedHistory?.collectAsState() ?: remember {
        // Sample data for preview if viewModel is null
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

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(medicationId, viewModel) {
        viewModel?.loadInitialHistory(medicationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(medicationName.ifEmpty { stringResource(id = R.string.medication_history_title) }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.med_history_navigate_back_cd)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
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
                onSortToggle = { viewModel?.setSortOrder(!sortAscending) },
                onGoToFirst = {
                    if (historyEntries.isNotEmpty()) {
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    }
                },
                onGoToLast = {
                    if (historyEntries.isNotEmpty()) {
                        coroutineScope.launch { listState.animateScrollToItem(historyEntries.size - 1) }
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

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
                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                        items(historyEntries, key = { it.id }) { entry ->
                            MedicationHistoryListItem(entry = entry)
                            if (historyEntries.last() != entry) {
                                Divider(modifier = Modifier.padding(start = 16.dp, end = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterControls(
    currentFilter: Pair<LocalDate?, LocalDate?>?,
    onFilterChanged: (startDate: LocalDate?, endDate: LocalDate?) -> Unit,
    onClearDateFilter: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(id = R.string.med_history_filter_by_date_label), style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { showDialog = true }, // Corrected: removed .value
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
    }
    if (currentFilter != null) { // Show clear button if any filter is active
        OutlinedButton(onClick = onClearDateFilter, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(id = R.string.med_history_filter_clear_button))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ActionControls(
    sortAscending: Boolean,
    onSortToggle: () -> Unit,
    onGoToFirst: () -> Unit,
    onGoToLast: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HistoryActionButton(icon = Icons.Filled.KeyboardArrowUp, text = stringResource(id = R.string.med_history_action_first), onClick = onGoToFirst)
        HistoryActionButton(icon = Icons.Filled.KeyboardArrowDown, text = stringResource(id = R.string.med_history_action_last), onClick = onGoToLast)
        HistoryActionButton(
            icon = Icons.Filled.SortByAlpha, // Could use different icons for asc/desc
            text = if (sortAscending) stringResource(id = R.string.med_history_sort_asc) else stringResource(id = R.string.med_history_sort_desc),
            onClick = onSortToggle
        )
    }
}

@Composable
fun HistoryActionButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(8.dp)) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp)) // Content description could be more specific if needed
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp)
    }
}


@Composable
fun MedicationHistoryListItem(entry: MedicationHistoryEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    // Medication name can be part of the item if needed, or use a general prefix
                    text = stringResource(id = R.string.med_history_item_taken_at_prefix) + entry.timeTaken.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Medication History Screen (Loading)")
@Composable
fun MedicationHistoryScreenPreview_Loading() {
    AppTheme {
        MedicationHistoryScreen(medicationId = 1, onNavigateBack = {}, viewModel = null) // viewModel = null will show sample data or loading state
    }
}


@Preview(showBackground = true, name = "Medication History List Item")
@Composable
fun MedicationHistoryListItemPreview() {
    AppTheme {
        MedicationHistoryListItem(
            entry = com.d4viddf.medicationreminder.data.MedicationHistoryEntry( // Use the correct data class
                id = "preview1",
                medicationName = "Sample Med",
                dateTaken = LocalDate.now(),
                timeTaken = LocalTime.now(),
                originalDateTimeTaken = LocalDateTime.now()
            )
        )
    }
}
