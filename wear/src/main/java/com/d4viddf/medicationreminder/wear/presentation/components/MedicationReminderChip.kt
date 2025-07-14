package com.d4viddf.medicationreminder.wear.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.wear.compose.material3.ChipDefaults

@Composable
fun MedicationReminderChip(
    reminder: WearReminder,
    onChipClick: () -> Unit,
    isTakenDisplay: Boolean = false // Kept for parity with old code, but reminder.isTaken is primary
) {
    // isTakenDisplay is effectively overridden by reminder.isTaken for actual state
    val displayAsTaken = reminder.isTaken || isTakenDisplay

    Card(
        onClick = {
            if (!displayAsTaken) { // Only allow click if not already considered taken for display/action
                onChipClick()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        enabled = !displayAsTaken,
        colors = CardDefaults.cardColors(
            containerColor = if (displayAsTaken) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (displayAsTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp), // Inner padding for content
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.medication_filled), // Ensure this drawable exists
                contentDescription = "Medication icon", // Generic description
                modifier = Modifier.size(ChipDefaults.LargeIconSize), // M3 ChipDefaults for standard icon size
                tint = if (displayAsTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.medicationName.split(" ").first(),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall // M3 Typography
                )
                Text(
                    text = reminder.time, // Just the time
                    style = MaterialTheme.typography.bodyMedium, // M3 Typography
                    maxLines = 1
                )
                reminder.dosage?.let {
                    if (it.isNotBlank()){
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall, // M3 Typography
                            maxLines = 1
                        )
                    }
                }
            }
            // Optional: Add an explicit checkmark icon if 'taken' similar to MedicationListItem
            // if (displayAsTaken) {
            //     Icon(
            //         imageVector = Icons.Filled.CheckCircle,
            //         contentDescription = stringResource(R.string.already_taken),
            //         tint = MaterialTheme.colorScheme.primary,
            //         modifier = Modifier.size(ChipDefaults.IconSize)
            //     )
            // }
        }
    }
}

// Previews for MedicationReminderChip
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = false, name = "Chip Upcoming")
@Composable
fun PreviewMedicationReminderChipUpcoming() {
    MedicationReminderTheme {
        MedicationReminderChip(
            reminder = WearReminder(
                id = "chip1",
                medicationId = 1,
                scheduleId = 1L,
                underlyingReminderId = 101L, // Example phone DB ID
                medicationName = "Amoxicillin",
                time = "10:00",
                isTaken = false,
                dosage = "1 tablet",
                takenAt = null
            ),
            onChipClick = {}
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = false, name = "Chip Taken")
@Composable
fun PreviewMedicationReminderChipTaken() {
    MedicationReminderTheme {
        MedicationReminderChip(
            reminder = WearReminder(
                id = "chip2",
                medicationId = 2,
                scheduleId = 2L,
                underlyingReminderId = 102L,
                medicationName = "Vitamin D",
                time = "12:00",
                isTaken = true,
                dosage = "1 capsule",
                takenAt = "2023-01-01T12:00:00Z"
            ),
            onChipClick = {}
            // isTakenDisplay = true // Redundant if reminder.isTaken is used
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = false, name = "Chip No Dosage")
@Composable
fun PreviewMedicationReminderChipNoDosage() {
    MedicationReminderTheme {
        MedicationReminderChip(
            reminder = WearReminder(
                id = "chip3",
                medicationId = 3,
                scheduleId = 3L,
                underlyingReminderId = 3L,
                medicationName = "Metformin",
                time = "14:00",
                isTaken = false,
                dosage = null, // Explicitly null
                takenAt = null
            ),
            onChipClick = {}
        )
    }
}
