package com.d4viddf.medicationreminder.wear.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Chip
import androidx.wear.compose.material3.ChipDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
import androidx.compose.ui.tooling.preview.Preview // Added
import androidx.wear.tooling.preview.devices.WearDevices // Added
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme // Added

@Composable
fun MedicationReminderChip(
    reminder: WearReminder,
    onChipClick: () -> Unit,
    isTakenDisplay: Boolean = false
) {
    Chip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = {
            if (!reminder.isTaken && !isTakenDisplay) {
                onChipClick()
            }
        },
        label = {
            Text(
                text = reminder.medicationName,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        },
        secondaryLabel = {
            reminder.dosage?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        },
        icon = {
            val iconResId = R.drawable.medication_filled
            val iconTint = if (isTakenDisplay || reminder.isTaken) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            }
            val iconDesc = if (isTakenDisplay || reminder.isTaken) "Taken" else "Medication icon"

            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = iconDesc,
                modifier = Modifier.size(ChipDefaults.IconSize),
                tint = iconTint
            )
        },
        colors = if (isTakenDisplay || reminder.isTaken) {
            ChipDefaults.secondaryChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
        } else {
            ChipDefaults.primaryChipColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    )
}

// Previews for MedicationReminderChip
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = false, name = "Chip Upcoming")
@Composable
fun PreviewMedicationReminderChipUpcoming() {
    MedicationReminderTheme {
        MedicationReminderChip(
            reminder = WearReminder("chip1", 1L, "Amoxicillin", "10:00", false, "1 tablet"),
            onChipClick = {}
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = false, name = "Chip Taken")
@Composable
fun PreviewMedicationReminderChipTaken() {
    MedicationReminderTheme {
        MedicationReminderChip(
            reminder = WearReminder("chip2", 2L, "Vitamin D", "12:00", true, "1 capsule"),
            onChipClick = {},
            isTakenDisplay = true
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = false, name = "Chip No Dosage")
@Composable
fun PreviewMedicationReminderChipNoDosage() {
    MedicationReminderTheme {
        MedicationReminderChip(
            reminder = WearReminder("chip3", 3L, "Metformin", "14:00", false, null),
            onChipClick = {}
        )
    }
}
