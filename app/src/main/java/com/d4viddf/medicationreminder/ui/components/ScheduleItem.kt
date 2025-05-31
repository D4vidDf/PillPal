package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme

// ScheduleItem Composable
@Composable
fun ScheduleItem(
    time: String,
    label: String,
    isTaken: Boolean,
    onTakenChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier // Added modifier for flexibility
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp), // Adjusted padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Pushes switch to the end
    ) {
        Column(modifier = Modifier.weight(1f)) { // Take available space, pushing switch
            Text(text = time, style = MaterialTheme.typography.titleMedium)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked = isTaken,
            onCheckedChange = onTakenChange,
            enabled = enabled,
            modifier = Modifier.padding(start = 8.dp) // Add some padding before the switch
            // No explicit contentDescription needed for Switch when accompanied by descriptive text labels.
        )
    }
}

@Preview(showBackground = true, name = "Schedule Item - Not Taken")
@Composable
fun ScheduleItemNotTakenPreview() {
    AppTheme {
        ScheduleItem(
            time = "10:00 AM",
            label = "Aspirin",
            isTaken = false,
            onTakenChange = {},
            enabled = true
        )
    }
}

@Preview(showBackground = true, name = "Schedule Item - Taken")
@Composable
fun ScheduleItemTakenPreview() {
    AppTheme {
        ScheduleItem(
            time = "02:00 PM",
            label = "Ibuprofen",
            isTaken = true,
            onTakenChange = {},
            enabled = true
        )
    }
}

@Preview(showBackground = true, name = "Schedule Item - Disabled")
@Composable
fun ScheduleItemDisabledPreview() {
    AppTheme {
        ScheduleItem(
            time = "08:00 PM",
            label = "Vitamin C",
            isTaken = false,
            onTakenChange = {},
            enabled = false
        )
    }
}
