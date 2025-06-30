package com.d4viddf.medicationreminder.ui.features.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun NextDoseCard(reminder: MedicationReminder) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.width(150.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Med ID: ${reminder.medicationId}", fontWeight = FontWeight.Bold) // Placeholder for Med Name
            Text("1 tablet") // Placeholder for Dosage
            val time = try {
                LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                "N/A"
            }
            Text(time, style = MaterialTheme.typography.bodySmall)
        }
    }
}
