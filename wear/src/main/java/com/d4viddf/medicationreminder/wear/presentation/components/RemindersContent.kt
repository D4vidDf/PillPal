package com.d4viddf.medicationreminder.wear.presentation.components

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // Ensure this is imported
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // For previews
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn // M3
import androidx.wear.compose.foundation.lazy.items // M3
import androidx.wear.compose.material3.MaterialTheme // M3
import androidx.wear.compose.material3.Text // M3
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.presentation.WearViewModel // For preview type
import com.d4viddf.medicationreminder.wear.presentation.WearViewModelFactory
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun RemindersContent(
    reminders: List<WearReminder>,
    onMarkAsTaken: (WearReminder) -> Unit // Expects the full WearReminder object
) {
    // isConnectedToPhone logic is removed, WearApp controls when this is shown.
    // This composable now assumes it's only displayed when there are reminders to show.

    if (reminders.isEmpty()) { // Should ideally be handled by WearApp, but as a fallback:
        Text(
            text = "No upcoming medications for today.", // Or use stringResource
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            textAlign = TextAlign.Center
        )
        return
    }

    // The logic for 'nextDoseGroup' and 'laterDoses' can be complex and might be better
    // handled in the ViewModel or a utility function if it involves more than simple filtering.
    // For now, keeping a simplified list structure.
    // The ViewModel already sorts by time and then by isTaken.
    // We can just display them in that order.

    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Optional: Add a header if desired
        // item {
        //     Text(
        //         text = "Today's Reminders",
        //         style = MaterialTheme.typography.titleMedium,
        //         modifier = Modifier.padding(vertical = 8.dp)
        //     )
        // }

        items(items = reminders, key = { it.id }) { reminder ->
            MedicationReminderChip( // Assuming this is the M3 version from MedicationReminderChip.kt
                reminder = reminder,
                onChipClick = { onMarkAsTaken(reminder) } // Pass the full reminder
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Reminders Content With Data")
@Composable
fun PreviewRemindersContentWithData() {
    val sampleReminders = listOf(
        WearReminder("rem1",1, 1L, 1L, "Lisinopril", "08:00", false, "10mg", null),
        WearReminder("rem2",2, 2L, 2L, "Metformin", "08:00", true, "500mg", "2023-01-01T08:00:00Z"),
        WearReminder("rem3",3, 3L, 3L, "Aspirin", "12:00", false, "81mg", null)
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
