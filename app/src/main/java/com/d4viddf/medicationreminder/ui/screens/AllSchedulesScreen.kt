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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
// import androidx.compose.material3.Switch // No longer needed
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.MedicationSchedule // Changed import
import com.d4viddf.medicationreminder.data.ScheduleType // For preview
import com.d4viddf.medicationreminder.data.getFormattedSchedule // Import extension function
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.viewmodel.AllSchedulesViewModel // Changed ViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel // Added ViewModel for name
// import java.time.LocalTime // No longer directly used here for placeholder generation
// import java.time.format.DateTimeFormatter // No longer directly used here
// import java.time.format.FormatStyle // No longer directly used here

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSchedulesScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    allSchedulesViewModel: AllSchedulesViewModel = hiltViewModel(),
    medicationViewModel: MedicationViewModel = hiltViewModel()
) {
    val schedules by allSchedulesViewModel.getSchedules(medicationId).collectAsState(initial = emptyList())
    var medicationName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(medicationId) {
        val med = medicationViewModel.getMedicationById(medicationId)
        medicationName = med?.name
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(medicationName ?: stringResource(id = R.string.all_schedules_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_button_content_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (medicationName == null && schedules.isEmpty()) { // Show loading only if name is null and schedules are empty
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                if (schedules.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.all_schedules_no_schedules_found),
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
                            medicationSchedule = scheduleItem,
                            medicationName = medicationName ?: "Medication" // Fallback name
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullScheduleItem(
    medicationSchedule: MedicationSchedule,
    medicationName: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Keep this if you add an icon or action later
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = medicationName, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = medicationSchedule.getFormattedSchedule(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        // Switch removed as per requirements
    }
}

@Preview(showBackground = true)
@Composable
fun AllSchedulesScreenPreview() {
    AppTheme {
        // This preview will show loading or empty state as ViewModels are not truly injected.
        // For a more representative preview, mock the ViewModels or pass preview data directly.
        AllSchedulesScreen(
            medicationId = 1,
            onNavigateBack = {}
            // Preview will use default Hilt ViewModels which won't have real data.
        )
    }
}

@Preview(showBackground = true, name = "All Schedules With Data")
@Composable
fun AllSchedulesScreenWithDataPreview() {
    val sampleSchedules = listOf(
        MedicationSchedule(id = 1, medicationId = 1, scheduleType = ScheduleType.DAILY, specificTimes = "08:00,14:00,20:00", intervalHours = null, intervalMinutes = null, daysOfWeek = null, intervalStartTime = null, intervalEndTime = null),
        MedicationSchedule(id = 2, medicationId = 1, scheduleType = ScheduleType.WEEKLY, daysOfWeek = "1,3,5", specificTimes = "10:00", intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null),
        MedicationSchedule(id = 3, medicationId = 1, scheduleType = ScheduleType.INTERVAL, intervalHours = 6, intervalMinutes = 0, intervalStartTime = "07:00", intervalEndTime = "23:00", specificTimes = null, daysOfWeek = null)
    )
    var medicationName by remember { mutableStateOf("Sample Medication") }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(medicationName) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back_button_content_description)
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
                if (sampleSchedules.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.all_schedules_no_schedules_found),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    items(sampleSchedules, key = { it.id }) { scheduleItem ->
                        FullScheduleItem(
                            medicationSchedule = scheduleItem,
                            medicationName = medicationName
                        )
                    }
                }
            }
        }
    }
}
// Removed AllSchedulesScreenPreviewWithHardcodedStrings as it's now obsolete
