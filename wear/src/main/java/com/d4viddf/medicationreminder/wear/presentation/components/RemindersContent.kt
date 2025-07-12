package com.d4viddf.medicationreminder.wear.presentation.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // Ensured import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// import androidx.lifecycle.viewmodel.compose.viewModel // Not needed here directly
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
// import com.d4viddf.medicationreminder.wear.presentation.WearViewModel // Not needed here directly
// import com.d4viddf.medicationreminder.wear.presentation.WearViewModelFactory // Not needed here
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme

@Composable
fun RemindersContent(
    reminders: List<WearReminder>,
    onMarkAsTaken: (WearReminder) -> Unit // Expects the full WearReminder object
) {
    // isConnectedToPhone logic is removed, WearApp controls when this is shown.
    // This composable now assumes it's only displayed when there are reminders to show.

    if (reminders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_reminders_today), // Updated string
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ListHeader {
                Text(stringResource(R.string.todays_reminders_header))
            }
        }
        items(items = reminders, key = { it.id }) { reminder ->
            // Assuming MedicationReminderChip is the M3 version from MedicationReminderChip.kt
            // or that MedicationListItem is the intended composable here.
            // The errors previously mentioned MedicationReminderChip argument mismatches.
            // If MedicationListItem is used from WearApp.kt, this call needs to match its signature.
            // For now, let's assume MedicationListItem is the one to use from WearApp.kt
            MedicationListItem( // This was the composable defined within WearApp.kt
                reminder = reminder,
                onMarkAsTaken = { onMarkAsTaken(reminder) } // Pass the full reminder object
            )
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
