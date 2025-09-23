package com.d4viddf.medicationreminder.ui.features.home.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R

/**
 * Displays the progress of medications taken today.
 *
 * @param taken The number of doses taken.
 * @param total The total number of doses for the day.
 */
@Composable
fun TodayProgressCard(
    taken: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.today_progress_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                // UPDATED: Shows "taken / total" format
                Text(
                    text = "$taken/$total",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (total > 0) {
                CircularProgressIndicator(
                    progress = { taken.toFloat() / total.toFloat() },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp
                )
            }
        }
    }
}


/**
 * Displays a summary of missed reminders.
 * The card will not be displayed if missedDoses is 0.
 *
 * @param missedDoses The total number of missed doses.
 * @param lastMissedMedication The name of the last medication that was missed.
 */
@Composable
fun MissedRemindersCard(
    missedDoses: Int,
    lastMissedMedication: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // This card will only show if there are missed doses
    if (missedDoses == 0) return

    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.missed_reminders_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = missedDoses.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                if (lastMissedMedication != null) {
                    Text(
                        text = "${stringResource(id = R.string.last_missed_label)} $lastMissedMedication",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
            IconButton(onClick = onClick) {Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null, // Should be tied to a navigation action
                modifier = Modifier.size(24.dp)
            ) }
        }
    }
}