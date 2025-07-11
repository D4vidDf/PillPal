package com.d4viddf.medicationreminder.wear.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import com.d4viddf.medicationreminder.wear.R
import androidx.compose.ui.tooling.preview.Preview // Added for Preview
import androidx.wear.tooling.preview.devices.WearDevices // Added for WearDevices
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme // Added for Theme

@Composable
fun ConnectionStatusIcon(isConnected: Boolean) {
    val iconResId = R.drawable.medication_filled // Replace with actual icons if available
    val tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val contentDesc = if (isConnected) "Connected to phone" else "Disconnected from phone"

    Icon(
        painter = painterResource(id = iconResId),
        contentDescription = contentDesc,
        tint = tint,
        modifier = Modifier
            .size(24.dp)
            .padding(top = 4.dp)
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = false)
@Composable
fun PreviewConnectionStatusIconConnected() {
    MedicationReminderTheme {
        ConnectionStatusIcon(isConnected = true)
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = false)
@Composable
fun PreviewConnectionStatusIconDisconnected() {
    MedicationReminderTheme {
        ConnectionStatusIcon(isConnected = false)
    }
}
