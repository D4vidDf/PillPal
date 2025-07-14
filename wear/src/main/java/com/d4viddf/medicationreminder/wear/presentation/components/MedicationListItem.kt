package com.d4viddf.medicationreminder.wear.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder

@Composable
fun MedicationListItem(
    reminder: WearReminder,
    onMarkAsTaken: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { /* TODO: Decide if item click does something, e.g. details */ },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if(reminder.isTaken) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = reminder.medicationName.split(" ").first(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (reminder.isTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.reminder_time_prefix, reminder.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (reminder.isTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (reminder.dosage?.isNotBlank() == true) {
                    Text(
                        text = stringResource(R.string.reminder_dosage_prefix, reminder.dosage),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reminder.isTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!reminder.isTaken) {
                Button(
                    onClick = onMarkAsTaken,
                    modifier = Modifier.size(32.dp).padding(0.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        painterResource(R.drawable.rounded_check),
                        contentDescription = stringResource(R.string.mark_as_taken),
                        modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                    )
                }
            } else {
                Icon(
                    painterResource(R.drawable.rounded_check_circle_24),
                    contentDescription = stringResource(R.string.already_taken),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
