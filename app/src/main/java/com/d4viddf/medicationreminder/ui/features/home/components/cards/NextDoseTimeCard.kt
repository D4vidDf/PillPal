package com.d4viddf.medicationreminder.ui.features.home.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun NextDoseTimeCard(
    timeToNextDoseInSeconds: Long,
    displayUnit: String
) {
    var remainingTime by remember { mutableStateOf(timeToNextDoseInSeconds) }

    // This effect ensures that if the initial time changes (e.g., due to a data refresh),
    // the card's internal state is updated.
    LaunchedEffect(key1 = timeToNextDoseInSeconds) {
        remainingTime = timeToNextDoseInSeconds
    }

    // This effect runs the one-second countdown tick.
    LaunchedEffect(key1 = remainingTime) {
        if (remainingTime > 0) {
            delay(1000)
            remainingTime--
        }
    }

    val formattedTime = when (displayUnit) {
        "seconds" -> "$remainingTime s"
        else -> {
            // --- UPDATED: Logic to handle hours, minutes, and seconds ---
            val hours = TimeUnit.SECONDS.toHours(remainingTime)
            val minutes = TimeUnit.SECONDS.toMinutes(remainingTime) % 60
            val seconds = remainingTime % 60

            if (hours > 0) {
                String.format("%02d:%02d:%02d h", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d m", minutes, seconds)
            }
        }
    }

    Card {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(text = "Next Dose In", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = formattedTime, style = MaterialTheme.typography.displaySmall)
        }
    }
}