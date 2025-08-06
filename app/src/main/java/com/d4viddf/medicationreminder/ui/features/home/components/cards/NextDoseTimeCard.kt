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

    LaunchedEffect(key1 = remainingTime) {
        if (remainingTime > 0) {
            delay(1000)
            remainingTime--
        }
    }

    val formattedTime = when (displayUnit) {
        "seconds" -> "$remainingTime s"
        else -> {
            val minutes = TimeUnit.SECONDS.toMinutes(remainingTime)
            val seconds = remainingTime % 60
            String.format("%02d:%02d m", minutes, seconds)
        }
    }

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Next Dose In", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = formattedTime, style = MaterialTheme.typography.displaySmall)
        }
    }
}