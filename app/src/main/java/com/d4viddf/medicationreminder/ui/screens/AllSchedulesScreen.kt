package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.TodayScheduleItem // Assuming this structure for now
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.viewmodel.MedicationReminderViewModel // Placeholder
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSchedulesScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MedicationReminderViewModel = hiltViewModel() // Placeholder for actual data fetching
) {
    // For now, using the same todayScheduleItems, ideally this would be all schedules for the medicationId
    // val schedules by viewModel.getSchedulesForMedication(medicationId).collectAsState(initial = emptyList())
    // Placeholder data for UI structure:
    val schedules = List(10) { index ->
        TodayScheduleItem(
            id = index.toLong(),
            medicationReminderId = medicationId.toLong(),
            medicationName = "Medication $medicationId",
            time = LocalTime.of(8 + index % 12, (index * 15) % 60), // Sample times
            isTaken = index % 3 == 0,
            isSkipped = false,
            scheduledDate = java.time.LocalDate.now()
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.all_schedules_title)) }, // Placeholder for actual string resource
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back) // Placeholder
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (schedules.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.no_schedules_found), // Placeholder
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(schedules, key = { it.id }) { scheduleItem ->
                    // Re-using a simplified ScheduleItem structure similar to MedicationDetailScreen
                    // This might need adjustment based on actual data structure for all schedules vs today's.
                    FullScheduleItem(
                        time = scheduleItem.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                        label = scheduleItem.medicationName, // Assuming medicationName is part of the schedule item
                        isTaken = scheduleItem.isTaken,
                        onTakenChange = { /* TODO: Handle taken change if needed, or make it read-only */ },
                        enabled = true // Or based on some logic like !scheduleItem.time.isAfter(LocalTime.now())
                    )
                }
            }
        }
    }
}

@Composable
fun FullScheduleItem(
    time: String,
    label: String,
    isTaken: Boolean,
    onTakenChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        // Assuming a Switch is still desired. If read-only, this could be an Icon or Text.
        Switch(
            checked = isTaken,
            onCheckedChange = onTakenChange,
            enabled = enabled
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AllSchedulesScreenPreview() {
    AppTheme {
        AllSchedulesScreen(
            medicationId = 1,
            onNavigateBack = {}
        )
    }
}

// TODO: Add actual string resources for R.string.all_schedules_title, R.string.back, R.string.no_schedules_found
// For now, these will cause a build error if not present.
// Consider adding them to strings.xml or using hardcoded strings for the initial structure.
// For example, title = { Text("All Schedules") }
// For the preview to work without real string resources, use hardcoded text in the composable directly.
// For example:
// title = { Text("All Schedules (Preview)") },
// contentDescription = "Back (Preview)"
// text = "No schedules found (Preview)"
// This is just for the preview. For the actual app, use stringResource.
// For this step, I will use hardcoded strings to ensure the file can be created and reviewed.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSchedulesScreenPreviewWithHardcodedStrings(
    medicationId: Int,
    onNavigateBack: () -> Unit
) {
    val schedules = List(10) { index ->
        TodayScheduleItem( // Using TodayScheduleItem for preview data structure
            id = index.toLong(),
            medicationReminderId = medicationId.toLong(),
            medicationName = "Sample Medication $medicationId",
            time = LocalTime.of(8 + index % 12, (index * 15) % 60),
            isTaken = index % 3 == 0,
            isSkipped = false,
            scheduledDate = java.time.LocalDate.now()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Schedules") }, // Hardcoded
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back" // Hardcoded
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (schedules.isEmpty()) {
                item {
                    Text(
                        text = "No schedules found for this medication.", // Hardcoded
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(schedules, key = { it.id }) { scheduleItem ->
                    FullScheduleItem(
                        time = scheduleItem.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                        label = scheduleItem.medicationName,
                        isTaken = scheduleItem.isTaken,
                        onTakenChange = { }, // Dummy action for preview
                        enabled = true
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "All Schedules Screen (Hardcoded Strings)")
@Composable
fun AllSchedulesScreenHardcodedPreview() {
    AppTheme {
        AllSchedulesScreenPreviewWithHardcodedStrings( // Calling the version with hardcoded strings
            medicationId = 1,
            onNavigateBack = {}
        )
    }
}
