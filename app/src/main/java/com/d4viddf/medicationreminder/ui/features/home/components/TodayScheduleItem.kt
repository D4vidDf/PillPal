package com.d4viddf.medicationreminder.ui.features.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

@Composable
fun TodayScheduleItem(
    item: TodayScheduleUiItem,
    onMarkAsTaken: (MedicationReminder) -> Unit,
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
    var showMenu by remember { mutableStateOf(false) }

    val itemContentDescription = stringResource(
        R.string.today_schedule_item_card_cd,
        item.medicationName,
        item.medicationDosage,
        item.medicationTypeName ?: "",
        item.formattedReminderTime
    )

    // The ListItem will handle the disabled state visually when enabled = false
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isFuture) { onNavigateToDetails(item.reminder.medicationId) }
            .semantics { contentDescription = itemContentDescription },
        // Set background to transparent to blend with the parent surface
        // The leading content is the medication icon with a colored background
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(medicationThemeColor.backgroundColor), // Color applied here!
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(item.medicationIconUrl ?: R.drawable.medication_filled)
                        .crossfade(true)
                        .error(R.drawable.medication_filled)
                        .build(),
                    placeholder = painterResource(R.drawable.medication_filled),
                    contentDescription = item.medicationTypeName ?: item.medicationName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(32.dp), // Icon is smaller than its background
                    // Tint the icon if it's the default drawable
                    colorFilter = if (item.medicationIconUrl == null) ColorFilter.tint(medicationThemeColor.textColor) else null
                )
            }
        },
        // Main text of the list item
        headlineContent = {
            Text(
                text = firstWordMedicationName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        // Subtext below the main text
        supportingContent = {
            Text(
                text = "${item.medicationDosage} - ${item.formattedReminderTime}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        // The three-dots menu at the end of the item

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
                onMarkAsTaken = {},
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
                onMarkAsTaken = {},
                onSkip = {},
                onNavigateToDetails = {},
                isFuture = true, // Item is for the future and will appear disabled
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}