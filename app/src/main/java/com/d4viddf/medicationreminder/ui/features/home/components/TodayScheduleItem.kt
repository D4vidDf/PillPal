package com.d4viddf.medicationreminder.ui.features.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScheduleItem(
    reminder: MedicationReminder,
    onMarkAsTaken: () -> Unit,
    timeFormatter: DateTimeFormatter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = if (reminder.isTaken) BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isTaken) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = reminder.isTaken,
                    onCheckedChange = { if (it) onMarkAsTaken() },
                    enabled = !reminder.isTaken,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Medication ID: ${reminder.medicationId}", // Placeholder
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (reminder.isTaken) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "1 tablet at ${
                            try {
                                LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter).format(timeFormatter)
                            } catch (e: Exception) { "" }
                        }", // Placeholder for dosage
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (reminder.isTaken) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Taken",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
