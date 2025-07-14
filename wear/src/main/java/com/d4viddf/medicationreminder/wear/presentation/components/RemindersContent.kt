package com.d4viddf.medicationreminder.wear.presentation.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.tooling.preview.devices.WearDevices
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.data.WearReminder
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
// Import the newly created MedicationListItem
import com.d4viddf.medicationreminder.wear.presentation.components.MedicationListItem
import com.google.android.horologist.compose.layout.ColumnItemType

@Composable
fun RemindersContent(
    reminders: List<WearReminder>,
    onMarkAsTaken: (WearReminder) -> Unit,
    onMoreClick: () -> Unit
) {

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        contentPadding =  rememberResponsiveColumnPadding(
            first = ColumnItemType.ListHeader,
            last = ColumnItemType.IconButton
        ),
        edgeButton = {
            EdgeButton(
                onClick = onMoreClick,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text(stringResource(R.string.more_options), textAlign = TextAlign.Center)
            }
        },
        timeText = { TimeText() }
    ) { contentPadding ->
        TransformingLazyColumn (
            state = listState,
            contentPadding = contentPadding,
        ) {
            item {
                ListHeader(
                    modifier =
                        Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(
                        text = stringResource(R.string.todays_reminders_header),
                    )
                }
            }
            items(reminders.size) { index ->
                val reminder = reminders[index]
                MedicationReminderChip(
                    reminder = reminder,
                    onChipClick = { onMarkAsTaken(reminder) }
                )
            }
            item{
                if (reminders.isEmpty()) {

                        Text(
                            text = stringResource(R.string.no_reminders_today),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Reminders Content With Data")
@Composable
fun PreviewRemindersContentWithData() {
    val sampleReminders = listOf(
        WearReminder("rem1",1, 1L, 101L, "Lisinopril", "08:00", false, "10mg", null),
        WearReminder("rem2",2, 2L, 102L, "Metformin", "08:00", true, "500mg", "2023-01-01T08:00:00Z"),
        WearReminder("rem3",3, 3L, 103L, "Aspirin", "12:00", false, "81mg", null)
    )
    MedicationReminderTheme {
        RemindersContent(
            reminders = sampleReminders, onMarkAsTaken = {},
            onMoreClick = {}
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Reminders Content Empty")
@Composable
fun PreviewRemindersContentEmpty() {
    MedicationReminderTheme {
        RemindersContent(
            reminders = emptyList(), onMarkAsTaken = {},
            onMoreClick = {}
        )
    }
}
