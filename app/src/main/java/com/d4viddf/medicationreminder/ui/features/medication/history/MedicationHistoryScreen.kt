package com.d4viddf.medicationreminder.ui.features.medication.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.MedicationHistoryEntry
import com.d4viddf.medicationreminder.ui.features.medication.history.components.FilterControls
import com.d4viddf.medicationreminder.ui.features.medication.history.components.HistoryScheduleItem
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.theme.MedicationSpecificTheme
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
import java.time.temporal.WeekFields
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.res.painterResource
import com.d4viddf.medicationreminder.R

// Sealed interface for list items
sealed interface HistoryListItemType
data class MonthHeader(val monthYear: String, val id: String = "month_header_$monthYear") : HistoryListItemType
data class WeekHeader(val weekRange: String, val id: String = "week_header_$weekRange") : HistoryListItemType
data class HistoryEntryItem(val entry: MedicationHistoryEntry, val originalId: String) : HistoryListItemType


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationHistoryScreen(
    medicationId: Int,
    colorName: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (Int) -> Unit,
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
                medicationDosage = "500mg",
                medicationColorName = "LIGHT_BLUE",
                medicationTypeName = "Tablet",
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
    val grouping by viewModel?.grouping?.collectAsState() ?: remember { mutableStateOf(HistoryGrouping.BY_MONTH) }

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
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
        Scaffold(
            modifier = Modifier,
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.medHistory_screen_title)) }, // Changed title
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(id = R.string.back_button_cd)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val newGrouping = if (grouping == HistoryGrouping.BY_MONTH) HistoryGrouping.BY_WEEK else HistoryGrouping.BY_MONTH
                            viewModel?.setGrouping(newGrouping)
                        }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = stringResource(id = R.string.group_by)
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
            Column( // This outer column receives paddingValues from Scaffold
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                FilterControls(
                    sortAscending = sortAscending,
                    onSortOrderChange = { viewModel?.setSortOrder(it) },
                    onDateFilterSelected = { showDateRangeDialog = true },
                    onAllTimeSelected = { viewModel?.setAllTimeFilter() }
                )
                if (historyEntries.isEmpty()) {
                    EmptyState(isFiltered = currentFilter != null)
                } else {
                    val groupedItems = remember(historyEntries, grouping) {
                        processHistoryEntries(historyEntries, grouping)
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        groupedItems.forEach { itemData ->
                            when (itemData) {
                                is MonthHeader -> {
                                    stickyHeader(key = itemData.id) {
                                        Text(
                                            text = itemData.monthYear.uppercase(Locale.getDefault()),
                                            style = MaterialTheme.typography.titleLarge,
                                            modifier = Modifier
                                                .background(color = MaterialTheme.colorScheme.background)
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 16.dp)
                                        )
                                    }
                                }
                                is WeekHeader -> {
                                    stickyHeader(key = itemData.id) {
                                        Text(
                                            text = itemData.weekRange.uppercase(Locale.getDefault()),
                                            style = MaterialTheme.typography.titleLarge,
                                            modifier = Modifier
                                                .background(color = MaterialTheme.colorScheme.background)
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 16.dp)
                                        )
                                    }
                                }
                                is HistoryEntryItem -> {
                                    item(key = itemData.originalId) {
                                        HistoryScheduleItem(
                                            item = itemData.entry,
                                            onNavigateToDetails = { onNavigateToDetails(medicationId) },
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

@OptIn(ExperimentalFoundationApi::class) // Added for stickyHeader
// Function to process history entries and insert month headers
private fun processHistoryEntries(
    entries: List<MedicationHistoryEntry>,
    grouping: HistoryGrouping
): List<HistoryListItemType> {
    if (entries.isEmpty()) return emptyList()

    return when (grouping) {
        HistoryGrouping.BY_MONTH -> {
            val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            val result = mutableListOf<HistoryListItemType>()
            var currentMonthYear = ""
            for (entry in entries) {
                val entryMonthYear = entry.originalDateTimeTaken.format(monthYearFormatter)
                if (entryMonthYear != currentMonthYear) {
                    currentMonthYear = entryMonthYear
                    result.add(MonthHeader(monthYear = currentMonthYear))
                }
                result.add(HistoryEntryItem(entry = entry, originalId = entry.id))
            }
            result
        }
        HistoryGrouping.BY_WEEK -> {
            val weekFields = WeekFields.of(Locale.getDefault())
            val weekFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            val result = mutableListOf<HistoryListItemType>()
            var currentWeekRange = ""
            for (entry in entries) {
                val date = entry.originalDateTimeTaken.toLocalDate()
                val startOfWeek = date.with(weekFields.dayOfWeek(), 1)
                val endOfWeek = startOfWeek.plusDays(6)
                val weekRange = "${startOfWeek.format(weekFormatter)} - ${endOfWeek.format(weekFormatter)}"
                if (weekRange != currentWeekRange) {
                    currentWeekRange = weekRange
                    result.add(WeekHeader(weekRange = currentWeekRange))
                }
                result.add(HistoryEntryItem(entry = entry, originalId = entry.id))
            }
            result
        }
    }
}

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

@Preview(showBackground = true, name = "Medication History Screen (Loading)")
@Composable
fun MedicationHistoryScreenPreview_Loading() {
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
                originalDateTimeTaken = LocalDateTime.now()
            ),
            onNavigateToDetails = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
    }
}
