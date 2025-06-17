package com.d4viddf.medicationreminder.ui.screens.medication


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.automirrored.filled.ArrowBack // Removed in previous step
// import androidx.compose.material.icons.filled.CalendarToday // Removed
// import androidx.compose.material.icons.filled.SwapVert // Removed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
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
// import androidx.compose.ui.input.nestedscroll.nestedScroll // Not needed anymore
import androidx.compose.ui.platform.LocalConfiguration // Added import
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.MedicationHistoryEntry
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
// import androidx.compose.material.icons.automirrored.filled.ArrowBack // Removed by previous rule, but ensure it's gone or remove again
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationSpecificTheme
import com.d4viddf.medicationreminder.viewmodel.MedicationHistoryViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

// Sealed interface for list items
sealed interface HistoryListItemType
data class MonthHeader(val monthYear: String, val id: String = "month_header_$monthYear") : HistoryListItemType
data class HistoryEntryItem(val entry: MedicationHistoryEntry, val originalId: String) : HistoryListItemType


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationHistoryScreen(
    medicationId: Int,
    colorName: String,
    onNavigateBack: () -> Unit,
    viewModel: MedicationHistoryViewModel? = hiltViewModel(), // Made nullable for preview
    selectedDate: String? = null, // Existing parameter
    selectedMonth: String? = null // New parameter for YYYY-MM
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

    var showDateRangeDialog by remember { mutableStateOf(false) } // Hoisted state variable

    LaunchedEffect(medicationId, viewModel, selectedDate, selectedMonth) { // Added selectedMonth to key
        var parsedSelectedDate: LocalDate? = null
        var parsedSelectedMonth: YearMonth? = null

        if (selectedDate != null && selectedDate.isNotBlank()) {
            try {
                parsedSelectedDate = LocalDate.parse(selectedDate)
                // Log.d("MedHistoryScreen", "Parsed selectedDate: $parsedSelectedDate")
            } catch (e: DateTimeParseException) {
                // Log.e("MedHistoryScreen", "Failed to parse selectedDate string: '$selectedDate'", e)
            }
        }

        if (selectedMonth != null && selectedMonth.isNotBlank()) {
            try {
                parsedSelectedMonth = YearMonth.parse(selectedMonth) // YearMonth.parse expects "YYYY-MM"
                // Log.d("MedHistoryScreen", "Parsed selectedMonth: $parsedSelectedMonth")
            } catch (e: DateTimeParseException) {
                // Log.e("MedHistoryScreen", "Failed to parse selectedMonth string: '$selectedMonth'", e)
            }
        }
        // ViewModel will prioritize selectedDate if both are somehow provided
        viewModel?.loadInitialHistory(medicationId, parsedSelectedDate, parsedSelectedMonth)
    }

    // DateRangePickerDialog logic moved here
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
                Button( // Changed from TextButton
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
                    colors = ButtonDefaults.buttonColors( // Added colors
                        containerColor = medicationColor.onBackgroundColor,
                        contentColor = medicationColor.cardColor
                    )
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                Button( // Changed from TextButton
                    onClick = { showDateRangeDialog = false },
                    colors = ButtonDefaults.buttonColors( // Added colors
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

    MedicationSpecificTheme(medicationColor = medicationColor) {
        Scaffold(
            modifier = Modifier,
            topBar = {
                TopAppBar(
                    title = { Text("History") }, // Changed title
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_arrow_back_ios_24),
                                contentDescription = stringResource(id = R.string.back_button_cd)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Column( // This outer column receives paddingValues from Scaffold
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
                val isLargeScreen = screenWidthDp >= 600.dp

                if (isLargeScreen) {
                    Row(Modifier.fillMaxSize()) {
                        // Left Pane: Filters
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally // ADDED
                        ) {
                            // This Row becomes a Column
                            Column(
                                modifier = Modifier.fillMaxWidth(), // It should still fill the width of its parent Column (the filter pane)
                                verticalArrangement = Arrangement.spacedBy(16.dp), // ADDED for spacing when stacked
                                horizontalAlignment = Alignment.CenterHorizontally // ADDED to center filter groups
                            ) {
                                // Original Left Column for Date Range Filter
                                Column(
                                    modifier = Modifier.width(IntrinsicSize.Max).padding(vertical = 4.dp), // CHANGED
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        stringResource(id = R.string.med_history_filter_by_date_label),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    OutlinedButton(
                                        onClick = { showDateRangeDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_calendar),
                                            contentDescription = null,
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text(
                                            text = currentFilter?.let {
                                                val start = it.first?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) ?: "..."
                                                val end = it.second?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) ?: "..."
                                                "$start - $end"
                                            } ?: stringResource(id = R.string.med_history_filter_select_range_button_label),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (currentFilter != null) {
                                        OutlinedButton(
                                            onClick = { viewModel?.setDateFilter(null, null) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(id = R.string.med_history_filter_clear_button))
                                        }
                                    }
                                }

                                // Original Right Column for Sort Order
                                Column(
                                    modifier = Modifier.width(IntrinsicSize.Max).padding(vertical = 4.dp), // CHANGED
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                    // horizontalAlignment = Alignment.Start // REMOVED, parent Column centers
                                ) {
                                    Text(
                                        stringResource(id = R.string.med_history_sort_order_label),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel?.setSortOrder(!sortAscending) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_swap_vert),
                                            contentDescription = null,
                                            modifier = Modifier.size(ButtonDefaults.IconSize)
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text(
                                            text = if (sortAscending) stringResource(id = R.string.med_history_sort_by_oldest_button)
                                                   else stringResource(id = R.string.med_history_sort_by_newest_button),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Middle Pane: History List
                        Column(
                            modifier = Modifier.weight(2f).padding(horizontal = 16.dp, vertical = 8.dp), // CHANGED weight
                            horizontalAlignment = Alignment.CenterHorizontally // ADDED
                        ) {
                            // Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) // REMOVED

                            val listModifier = Modifier.fillMaxSize() // Padding handled by parent Column

                            when {
                                isLoading -> Box(modifier = listModifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                                error != null -> Box(modifier = listModifier, contentAlignment = Alignment.Center) {
                                    Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                                }
                                historyEntries.isEmpty() -> Box(modifier = listModifier, contentAlignment = Alignment.Center) {
                                    Text(
                                        stringResource(id = R.string.med_history_no_history_found),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                else -> {
                                    val groupedItems = remember(historyEntries, sortAscending) {
                                        processHistoryEntries(historyEntries, sortAscending)
                                    }
                                    if (groupedItems.isEmpty()) {
                                        Box(modifier = listModifier, contentAlignment = Alignment.Center) {
                                            Text(
                                                stringResource(id = R.string.med_history_no_history_found),
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(16.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        LazyColumn(modifier = listModifier) {
                                            groupedItems.forEach { itemData ->
                                                when (itemData) {
                                                    is MonthHeader -> {
                                                        stickyHeader(key = itemData.id) {
                                                            Text(
                                                                text = itemData.monthYear.uppercase(Locale.getDefault()),
                                                                style = MaterialTheme.typography.titleLarge,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(MaterialTheme.colorScheme.surface)
                                                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                                            )
                                                        }
                                                    }
                                                    is HistoryEntryItem -> {
                                                        item(key = itemData.originalId) {
                                                            MedicationHistoryListItem(entry = itemData.entry, itemModifier = Modifier.widthIn(max = 400.dp)) // MODIFIED call
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right Pane: Empty
                        // Spacer(Modifier.weight(1f)) // REMOVED
                    }
                } else {
                    // Existing Small Screen Layout (filters above list)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column for Date Range Filter
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                stringResource(id = R.string.med_history_filter_by_date_label),
                                style = MaterialTheme.typography.titleSmall
                            )
                            OutlinedButton(
                                onClick = { showDateRangeDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_calendar),
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(
                                    text = currentFilter?.let {
                                        val start = it.first?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) ?: "..."
                                        val end = it.second?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) ?: "..."
                                        "$start - $end"
                                    } ?: stringResource(id = R.string.med_history_filter_select_range_button_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (currentFilter != null) {
                                OutlinedButton(
                                    onClick = { viewModel?.setDateFilter(null, null) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(id = R.string.med_history_filter_clear_button))
                                }
                            }
                        }

                        // Right Column for Sort Order
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                stringResource(id = R.string.med_history_sort_order_label),
                                style = MaterialTheme.typography.titleSmall
                            )
                            OutlinedButton(
                                onClick = { viewModel?.setSortOrder(!sortAscending) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_swap_vert),
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(
                                    text = if (sortAscending) stringResource(id = R.string.med_history_sort_by_oldest_button)
                                           else stringResource(id = R.string.med_history_sort_by_newest_button),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) // REMOVED

                    val listModifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)

                    when {
                        isLoading -> Box(modifier = listModifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        error != null -> Box(modifier = listModifier, contentAlignment = Alignment.Center) {
                            Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                        }
                        historyEntries.isEmpty() -> Box(modifier = listModifier, contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(id = R.string.med_history_no_history_found),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            val groupedItems = remember(historyEntries, sortAscending) {
                                processHistoryEntries(historyEntries, sortAscending)
                            }
                            if (groupedItems.isEmpty()) {
                                Box(modifier = listModifier, contentAlignment = Alignment.Center) {
                                    Text(
                                        stringResource(id = R.string.med_history_no_history_found),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(modifier = listModifier) {
                                            groupedItems.forEach { itemData ->
                                                when (itemData) {
                                            is MonthHeader -> {
                                                        stickyHeader(key = itemData.id) {
                                                            Text(
                                                                text = itemData.monthYear.uppercase(Locale.getDefault()),
                                                                style = MaterialTheme.typography.titleLarge,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(MaterialTheme.colorScheme.surface)
                                                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                                            )
                                                        }
                                            }
                                            is HistoryEntryItem -> {
                                                        item(key = itemData.originalId) {
                                                            MedicationHistoryListItem(entry = itemData.entry, itemModifier = Modifier.fillMaxWidth()) // MODIFIED call
                                                        }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // Closes Column triggered by if/else
        } // Closes Scaffold content lambda
    } // Closes MedicationSpecificTheme content lambda
} // Closes MedicationHistoryScreen

@OptIn(ExperimentalFoundationApi::class) // Added for stickyHeader
// Function to process history entries and insert month headers
private fun processHistoryEntries(
    entries: List<MedicationHistoryEntry>,
    sortAscending: Boolean
): List<HistoryListItemType> {
    if (entries.isEmpty()) return emptyList()

    val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    val result = mutableListOf<HistoryListItemType>()
    var currentMonthYear = ""

    // The sorting is now handled by the ViewModel, so we respect the order of `entries`
    for (entry in entries) {
        val entryMonthYear = entry.originalDateTimeTaken.format(monthYearFormatter)
        if (entryMonthYear != currentMonthYear) {
            currentMonthYear = entryMonthYear
            result.add(MonthHeader(monthYear = currentMonthYear))
        }
        result.add(HistoryEntryItem(entry = entry, originalId = entry.id))
    }
    return result
}


// FilterControls and ActionControls composables are now inlined into MedicationHistoryScreen.
// They can be removed if they are not used elsewhere.

@Composable
fun MedicationHistoryListItem(entry: MedicationHistoryEntry, itemModifier: Modifier = Modifier) { // Added itemModifier
    Card(
        modifier = itemModifier.padding(vertical = 6.dp), // Used itemModifier, removed .fillMaxWidth()
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
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
            ),
            itemModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp) // MODIFIED call
        )
    }
}
