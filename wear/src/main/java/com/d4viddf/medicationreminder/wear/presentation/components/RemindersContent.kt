package com.d4viddf.medicationreminder.wear.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
// Ensure items is correctly imported if needed, often covered by ScalingLazyColumn import
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.presentation.WearViewModel
import androidx.compose.ui.tooling.preview.Preview // Added
import androidx.wear.tooling.preview.devices.WearDevices // Added
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme // Added
import androidx.compose.ui.platform.LocalContext // Added
import android.app.Application // Added

@Composable
fun RemindersContent(reminders: List<WearReminder>, viewModel: WearViewModel) {
    val isConnectedState by viewModel.isConnectedToPhone.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberScalingLazyListState(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ConnectionStatusIcon(isConnected = isConnectedState)
        }

        val upcomingReminders = reminders.filter { !it.isTaken }.sortedBy { it.time }

        if (upcomingReminders.isEmpty()) {
            item {
                Text(
                    text = "No upcoming medications.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val nextDoseTime = upcomingReminders.firstOrNull()?.time
            val nextDoseGroup = upcomingReminders.filter { it.time == nextDoseTime }
            val laterDoses = upcomingReminders.filter { it.time != nextDoseTime && it.time > (nextDoseTime ?: "00:00") }

            if (nextDoseGroup.isNotEmpty()) {
                item {
                    Text(
                        text = "Next: ${nextDoseGroup.first().time}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(nextDoseGroup.size, key = { index -> "next_${nextDoseGroup[index].id}" }) { index ->
                    val reminder = nextDoseGroup[index]
                    MedicationReminderChip(
                        reminder = reminder,
                        onChipClick = { viewModel.markReminderAsTaken(reminder.underlyingReminderId) }
                    )
                }
            }
            if (laterDoses.isNotEmpty()) {
                val uniqueLaterTimes = laterDoses.map { it.time }.distinct().sorted()
                uniqueLaterTimes.forEach { time ->
                    val remindersForTime = laterDoses.filter { it.time == time }
                    if (remindersForTime.isNotEmpty()) {
                        item {
                            Text(
                                text = "Later: $time",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                                    .fillMaxWidth()
                            )
                        }
                        items(remindersForTime.size, key = { index -> "later_${remindersForTime[index].id}" }) { index ->
                            val reminder = remindersForTime[index]
                            MedicationReminderChip(
                                reminder = reminder,
                                onChipClick = { viewModel.markReminderAsTaken(reminder.underlyingReminderId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Reminders Content Empty")
@Composable
fun PreviewRemindersContentEmpty() {
    val context = LocalContext.current
    val fakeViewModel = WearViewModel(context.applicationContext as Application)
    // Ensure isConnected is true for this preview to show "No upcoming" vs "Disconnected"
    // This requires a way to set state on fakeViewModel, or a more sophisticated fake.
    // For now, it will show based on default isConnected state (likely false).
    MedicationReminderTheme {
        RemindersContent(reminders = emptyList(), viewModel = fakeViewModel)
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Reminders Content With Data")
@Composable
fun PreviewRemindersContentWithData() {
    val context = LocalContext.current
    val fakeViewModel = WearViewModel(context.applicationContext as Application)
    // (fakeViewModel as any)._isConnectedToPhone.value = true // Example if mutable
    val sampleReminders = listOf(
        WearReminder("rem1", 1L, "Lisinopril", "08:00", false, "10mg"),
        WearReminder("rem2", 2L, "Metformin", "08:00", true, "500mg"),
        WearReminder("rem3", 3L, "Aspirin", "12:00", false, "81mg")
    )
    MedicationReminderTheme {
        RemindersContent(reminders = sampleReminders, viewModel = fakeViewModel)
    }
}
