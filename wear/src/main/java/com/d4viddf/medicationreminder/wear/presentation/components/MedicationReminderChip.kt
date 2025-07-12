package com.d4viddf.medicationreminder.wear.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Card // Using Card as M3 replacement for Chip for more content flexibility
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ChipDefaults // For icon size if needed with Button
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment

@Composable
fun MedicationReminderChip(
    reminder: WearReminder,
    onChipClick: () -> Unit,
    isTakenDisplay: Boolean = false // This parameter might be redundant if reminder.isTaken is the source of truth
) {
    val isActuallyTaken = isTakenDisplay || reminder.isTaken

    Card( // M3 Card instead of M2 Chip
        onClick = {
            if (!isActuallyTaken) { // Only allow click if not taken
                onChipClick()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        enabled = !isActuallyTaken, // Card is disabled if taken
        colors = CardDefaults.cardColors(
            containerColor = if (isActuallyTaken) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
            contentColor = if (isActuallyTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.medication_filled), // Assuming this drawable exists
                contentDescription = if (isActuallyTaken) "Medication taken" else "Medication due",
                modifier = Modifier.size(ChipDefaults.IconSize), // Use M3 ChipDefaults for standard icon size
                tint = if (isActuallyTaken) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = reminder.medicationName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyLarge
                )
                reminder.dosage?.let {
                    if (it.isNotBlank()){
                        Text(
                            text = it,
                            fontSize = 12.sp, // Consider using MaterialTheme.typography.labelSmall or similar
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            // No explicit "taken" icon here, the card's appearance and disabled state indicate it.
            // An explicit check/close icon can be added if preferred, similar to MedicationListItem
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
                underlyingReminderId = 1L,
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
                underlyingReminderId = 2L,
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
