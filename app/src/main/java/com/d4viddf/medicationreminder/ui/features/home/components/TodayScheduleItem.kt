package com.d4viddf.medicationreminder.ui.features.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.features.home.model.TodayScheduleUiItem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
import androidx.compose.foundation.clickable // Added for clickable

@Composable
fun TodayScheduleItem(
    item: TodayScheduleUiItem,
    onMarkAsTaken: (MedicationReminder) -> Unit,
    onNavigateToDetails: (medicationId: Int) -> Unit, // Added navigation callback
    modifier: Modifier = Modifier
) {
    val medicationThemeColor = try {
        MedicationColor.valueOf(item.medicationColorName)
    } catch (e: IllegalArgumentException) {
        MedicationColor.LIGHT_ORANGE // Default color
    }

    val firstWordMedicationName = item.medicationName.split(" ").firstOrNull() ?: item.medicationName

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // Add some vertical padding between items
            .clickable { onNavigateToDetails(item.reminder.medicationId) }, // Added clickable
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = medicationThemeColor.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Medication Type Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape) // Optional: make it circular if the icon itself is not
                // .background(medicationThemeColor.textColor.copy(alpha = 0.1f)) // Optional subtle background for icon
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.medicationIconUrl ?: R.drawable.medication_filled) // Fallback to a default pill icon
                        .crossfade(true)
                        .error(R.drawable.medication_filled) // Also use fallback on error
                        .build(),
                    placeholder = painterResource(R.drawable.medication_outline), // Placeholder while loading
                    contentDescription = item.medicationTypeName ?: item.medicationName,
                    contentScale = ContentScale.Crop, // Or ContentScale.Fit
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = if (item.medicationIconUrl == null) ColorFilter.tint(medicationThemeColor.textColor) else null // Tint if using fallback drawable
                )
            }

            Spacer(Modifier.width(12.dp))

            // Medication Info: Name, Dosage, Time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = firstWordMedicationName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = medicationThemeColor.textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.medicationDosage} - ${item.formattedReminderTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = medicationThemeColor.textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Toggle to mark as taken
            Switch(
                checked = item.reminder.isTaken,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        onMarkAsTaken(item.reminder)
                    }
                },
                enabled = !item.reminder.isTaken,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = ContentAlpha.disabled),
                    disabledUncheckedThumbColor = MaterialTheme.colorScheme.outline.copy(alpha = ContentAlpha.disabled)
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "TodayScheduleItem Light")
@Composable
fun TodayScheduleItemPreview() {
    val sampleReminder = MedicationReminder(1, 101, 1, LocalDateTime.now().format(ReminderCalculator.storableDateTimeFormatter), false, null, null)
    val sampleItem = TodayScheduleUiItem(
        reminder = sampleReminder,
        medicationName = "Amoxicillin Long Name",
        medicationDosage = "250mg Capsule",
        medicationColorName = "LIGHT_BLUE",
        medicationIconUrl = null, // Preview with fallback icon
        medicationTypeName = "Capsule",
        formattedReminderTime = "08:00"
    )
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TodayScheduleItem(item = sampleItem, onMarkAsTaken = {}, onNavigateToDetails = {})
        }
    }
}

@Preview(showBackground = true, name = "TodayScheduleItem Taken Light")
@Composable
fun TodayScheduleItemTakenPreview() {
    val sampleReminderTaken = MedicationReminder(2, 102, 2, LocalDateTime.now().minusHours(1).format(ReminderCalculator.storableDateTimeFormatter), true, LocalDateTime.now().minusHours(1).format(ReminderCalculator.storableDateTimeFormatter), null)
    val sampleItemTaken = TodayScheduleUiItem(
        reminder = sampleReminderTaken,
        medicationName = "Ibuprofen",
        medicationDosage = "200mg Tablet",
        medicationColorName = "LIGHT_RED",
        medicationIconUrl = "https://example.com/dummy_icon.png", // Preview with AsyncImage (will show placeholder/error in preview)
        medicationTypeName = "Tablet",
        formattedReminderTime = "07:00"
    )
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TodayScheduleItem(item = sampleItemTaken, onMarkAsTaken = {}, onNavigateToDetails = {})
        }
    }
}
