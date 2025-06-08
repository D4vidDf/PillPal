package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.hilt.navigation.compose.hiltViewModel // For later ViewModel integration
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme exists
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Placeholder data structure
data class MedicationHistoryEntry(
    val id: Int,
    val date: LocalDate,
    val timeTaken: LocalTime,
    val medicationName: String // Could be useful context
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationHistoryScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit
    // viewModel: MedicationHistoryViewModel = hiltViewModel() // Placeholder
) {
    // Placeholder states for filter/sort
    var selectedDateRange by remember { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var sortAscending by remember { mutableStateOf(true) }

    // Placeholder data
    val historyEntries = remember(medicationId, sortAscending) {
        val baseList = List(15) { index ->
            MedicationHistoryEntry(
                id = index,
                date = LocalDate.now().minusDays(index.toLong()),
                timeTaken = LocalTime.of((8 + index) % 24, (index * 13) % 60),
                medicationName = "Medication $medicationId"
            )
        }
        if (sortAscending) baseList.sortedBy { it.date } else baseList.sortedByDescending { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medication History") }, // Hardcoded string
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back" // Hardcoded string
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
            // Date Filtering Options
            FilterControls(
                selectedDateRange = selectedDateRange,
                onDateRangeSelected = { startDate, endDate ->
                    selectedDateRange = Pair(startDate, endDate)
                    // TODO: Implement actual filtering logic
                },
                onClearDateFilter = { selectedDateRange = null }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation and Sorting Controls
            ActionControls(
                sortAscending = sortAscending,
                onSortToggle = { sortAscending = !sortAscending },
                onGoToFirst = { /* TODO: Scroll to first item */ },
                onGoToLast = { /* TODO: Scroll to last item */ }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // History List
            if (historyEntries.isEmpty()) {
                Text(
                    "No history found for this medication.", // Hardcoded
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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

@Composable
fun FilterControls(
    selectedDateRange: Pair<LocalDate, LocalDate>?,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onClearDateFilter: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Filter by Date:", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { /* TODO: Show Date Range Picker Dialog */
                // For now, let's simulate a selection
                onDateRangeSelected(LocalDate.now().minusWeeks(1), LocalDate.now())
            },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Icon(Icons.Filled.CalendarToday, contentDescription = "Select Date", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                selectedDateRange?.let {
                    "${it.first.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))} - ${it.second.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))}"
                } ?: "Select Range",
                fontSize = 12.sp
            )
        }
    }
    if (selectedDateRange != null) {
        OutlinedButton(onClick = onClearDateFilter, modifier = Modifier.fillMaxWidth()) {
            Text("Clear Date Filter")
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
        HistoryActionButton(icon = Icons.Filled.KeyboardArrowUp, text = "First", onClick = onGoToFirst)
        HistoryActionButton(icon = Icons.Filled.KeyboardArrowDown, text = "Last", onClick = onGoToLast)
        HistoryActionButton(
            icon = Icons.Filled.SortByAlpha, // Could use different icons for asc/desc
            text = if (sortAscending) "Asc" else "Desc",
            onClick = onSortToggle
        )
    }
}

@Composable
fun HistoryActionButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(8.dp)) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(18.dp))
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
                    text = entry.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), // e.g., "Monday, April 1, 2024"
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Taken at: ${entry.timeTaken.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Could add a small icon or indicator here if needed
        }
    }
}

@Preview(showBackground = true, name = "Medication History Screen")
@Composable
fun MedicationHistoryScreenPreview() {
    AppTheme {
        MedicationHistoryScreen(
            medicationId = 1,
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Medication History List Item")
@Composable
fun MedicationHistoryListItemPreview() {
    AppTheme {
        MedicationHistoryListItem(
            entry = MedicationHistoryEntry(
                id = 1,
                date = LocalDate.now(),
                timeTaken = LocalTime.now(),
                medicationName = "Sample Med"
            )
        )
    }
}
