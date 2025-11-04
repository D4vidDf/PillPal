package com.d4viddf.medicationreminder.ui.features.medication.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.d4viddf.medicationreminder.data.model.MedicationHistoryEntry
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HistoryScheduleItem(
    item: MedicationHistoryEntry,
    onNavigateToDetails: () -> Unit,
    onTakenStatusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val medicationThemeColor = try {
        MedicationColor.valueOf(item.medicationColorName)
    } catch (e: IllegalArgumentException) {
        MedicationColor.LIGHT_ORANGE // Default color
    }

    val formattedTime = item.timeTaken.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val dayOfWeek = item.dateTaken.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))

    val itemContentDescription = stringResource(
        R.string.today_schedule_item_card_cd,
        item.medicationName,
        item.medicationDosage,
        item.medicationTypeName ?: "",
        formattedTime
    )

    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetails() }
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
                text = item.medicationName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "${item.medicationDosage} - $dayOfWeek, $formattedTime",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            val switchContentDescription = if (item.isTaken) {
                stringResource(R.string.switch_state_taken)
            } else {
                stringResource(R.string.switch_state_not_taken)
            }
            Switch(
                checked = item.isTaken,
                onCheckedChange = onTakenStatusChange,
                modifier = Modifier.semantics {
                    contentDescription = switchContentDescription
                }
            )
        }
    )
}

@Preview(showBackground = true, name = "HistoryScheduleItem Light")
@Composable
fun HistoryScheduleItemPreview() {
    val sampleItem = MedicationHistoryEntry(
        id = "1",
        medicationName = "Amoxicillin Long Name",
        medicationDosage = "250mg Capsule",
        medicationColorName = "LIGHT_BLUE",
        medicationTypeName = "Capsule",
        dateTaken = LocalDate.now(),
        timeTaken = LocalTime.of(8, 0),
        originalDateTimeTaken = LocalDateTime.now(),
        isTaken = true
    )
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            HistoryScheduleItem(
                item = sampleItem,
                onNavigateToDetails = {},
                onTakenStatusChange = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
