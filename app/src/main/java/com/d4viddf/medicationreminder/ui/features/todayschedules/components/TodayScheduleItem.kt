package com.d4viddf.medicationreminder.ui.features.todayschedules.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.domain.usecase.ReminderCalculator
import com.d4viddf.medicationreminder.ui.features.todayschedules.model.TodayScheduleUiItem
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import java.time.LocalDateTime

@Composable
fun TodayScheduleItem(
    item: TodayScheduleUiItem,
    onMarkAsTaken: (reminder: MedicationReminder, isTaken: Boolean) -> Unit,
    onSkip: (MedicationReminder) -> Unit,
    onNavigateToDetails: (medicationId: Int) -> Unit,
    isFuture: Boolean,
    modifier: Modifier = Modifier
) {
    val medicationThemeColor = try {
        MedicationColor.valueOf(item.medicationColorName)
    } catch (e: IllegalArgumentException) {
        MedicationColor.LIGHT_ORANGE // Default color
    }

    val firstWordMedicationName = item.medicationName.split(" ").firstOrNull() ?: item.medicationName

    val itemContentDescription = stringResource(
        R.string.today_schedule_item_card_cd,
        item.medicationName,
        item.medicationDosage,
        item.medicationTypeName ?: "",
        item.formattedReminderTime
    )

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetails(item.reminder.medicationId) }
            .semantics { contentDescription = itemContentDescription },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(medicationThemeColor.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.medication_filled),
                    contentDescription = item.medicationTypeName ?: item.medicationName,
                    modifier = Modifier.size(32.dp),
                    tint = medicationThemeColor.textColor
                )
            }
        },
        headlineContent = {
            Text(
                text = firstWordMedicationName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            // The text style is now simpler and won't change when taken.
            Text(
                text = "${item.medicationDosage} - ${item.formattedReminderTime}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = item.reminder.isTaken,
                onCheckedChange = { newCheckedState ->
                    onMarkAsTaken(item.reminder, newCheckedState)
                },
                enabled = !isFuture
            )
        }
    )
}

// Previews are updated to show the new ListItem style

@Preview(showBackground = true, name = "TodayScheduleItem Light")
@Composable
fun TodayScheduleItemPreview() {
    val sampleReminder = MedicationReminder(1, 101, 1, LocalDateTime.now().format(ReminderCalculator.storableDateTimeFormatter), false, null, null)
    val sampleItem = TodayScheduleUiItem(
        reminder = sampleReminder,
        medicationName = "Amoxicillin Long Name",
        medicationDosage = "250mg Capsule",
        medicationColorName = "LIGHT_BLUE",
        medicationIconUrl = null,
        medicationTypeName = "Capsule",
        formattedReminderTime = "08:00"
    )
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TodayScheduleItem(
                item = sampleItem,
                onMarkAsTaken = {} as (MedicationReminder, Boolean) -> Unit,
                onSkip = {},
                onNavigateToDetails = {},
                isFuture = false,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "TodayScheduleItem Future")
@Composable
fun TodayScheduleItemFuturePreview() {
    val sampleReminderTaken = MedicationReminder(2, 102, 2, LocalDateTime.now().plusHours(2).format(ReminderCalculator.storableDateTimeFormatter), true, LocalDateTime.now().minusHours(1).format(ReminderCalculator.storableDateTimeFormatter), null)
    val sampleItemTaken = TodayScheduleUiItem(
        reminder = sampleReminderTaken,
        medicationName = "Ibuprofen",
        medicationDosage = "200mg Tablet",
        medicationColorName = "LIGHT_RED",
        medicationIconUrl = "https://example.com/dummy_icon.png",
        medicationTypeName = "Tablet",
        formattedReminderTime = "14:00"
    )
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TodayScheduleItem(
                item = sampleItemTaken,
                onMarkAsTaken = {} as (MedicationReminder, Boolean) -> Unit,
                onSkip = {},
                onNavigateToDetails = {},
                isFuture = true, // Item is for the future and will appear disabled
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}