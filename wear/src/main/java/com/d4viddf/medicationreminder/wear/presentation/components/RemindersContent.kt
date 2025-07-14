package com.d4viddf.medicationreminder.wear.presentation.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
// Import the newly created MedicationListItem
import com.d4viddf.medicationreminder.wear.presentation.components.MedicationListItem

@Composable
fun RemindersContent(
    reminders: List<WearReminder>,
    onMarkAsTaken: (WearReminder) -> Unit,
    onMoreClick: () -> Unit
) {
    if (reminders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_reminders_today),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = stringResource(R.string.todays_reminders_header),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(reminders.size) { index ->
            val reminder = reminders[index]
            MedicationListItem(
                reminder = reminder,
                onMarkAsTaken = { onMarkAsTaken(reminder) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        item {
            Button(
                onClick = onMoreClick,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(text = stringResource(R.string.more_options))
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Reminders Content With Data")
@Composable
fun PreviewRemindersContentWithData() {
    val sampleReminders = listOf(
        WearReminder("rem1",1, 1L, 101L, "Lisinopril", "08:00", false, "10mg", null),
        WearReminder("rem2",2, 2L, 102L, "Metformin", "08:00", true, "500mg", "2023-01-01T08:00:00Z"),
        WearReminder("rem3",3, 3L, 103L, "Aspirin", "12:00", false, "81mg", null)
    )
    MedicationReminderTheme {
        RemindersContent(reminders = sampleReminders, onMarkAsTaken = {})
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Reminders Content Empty")
@Composable
fun PreviewRemindersContentEmpty() {
    MedicationReminderTheme {
        RemindersContent(reminders = emptyList(), onMarkAsTaken = {})
    }
}
