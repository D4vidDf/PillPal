package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme, adjust if MedicationReminderTheme is used

@Composable
fun NotificationToggleWidget(
    isNotificationEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(id = com.d4viddf.medicationreminder.R.string.notifications_toggle_label), modifier = Modifier.weight(1f))
        Switch(
            checked = isNotificationEnabled,
            onCheckedChange = { onToggle(it) }
        )
    }
}

@Preview(showBackground = true, name = "Notifications Enabled")
@Composable
fun NotificationToggleWidgetEnabledPreview() {
    AppTheme {
        NotificationToggleWidget(
            isNotificationEnabled = true,
            onToggle = {}
        )
    }
}

@Preview(showBackground = true, name = "Notifications Disabled")
@Composable
fun NotificationToggleWidgetDisabledPreview() {
    AppTheme {
        NotificationToggleWidget(
            isNotificationEnabled = false,
            onToggle = {}
        )
    }
}
