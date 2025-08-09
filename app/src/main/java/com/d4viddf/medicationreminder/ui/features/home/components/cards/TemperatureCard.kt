package com.d4viddf.medicationreminder.ui.features.home.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TemperatureCard(
    temperatureRecord: BodyTemperature?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Body Temperature", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (temperatureRecord != null) {
                Text(
                    text = "%.1f °C".format(temperatureRecord.temperatureCelsius),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last recorded: ${formatInstant(temperatureRecord.time)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("No data yet", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatInstant(instant: Instant): String {
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    return zonedDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
}