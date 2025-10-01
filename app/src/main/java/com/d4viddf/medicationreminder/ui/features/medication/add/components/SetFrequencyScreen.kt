package com.d4viddf.medicationreminder.ui.features.medication.add.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R

@Composable
fun SetFrequencyScreen(
    onFrequencySelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(id = R.string.set_frequency_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = stringResource(id = R.string.daily_frequency_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FrequencyOptionButton(text = stringResource(id = R.string.once_daily)) { onFrequencySelected("once_daily") }
        FrequencyOptionButton(text = stringResource(id = R.string.twice_daily)) { onFrequencySelected("twice_daily") }
        FrequencyOptionButton(text = stringResource(id = R.string.thrice_daily)) { onFrequencySelected("thrice_daily") }
        FrequencyOptionButton(text = stringResource(id = R.string.more_than_thrice_daily)) { onFrequencySelected("more_than_thrice_daily") }
        FrequencyOptionButton(text = stringResource(id = R.string.custom_daily_pattern)) { onFrequencySelected("custom_daily") }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.periodic_frequency_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FrequencyOptionButton(text = stringResource(id = R.string.specific_days_of_week)) { onFrequencySelected("specific_days") }
        FrequencyOptionButton(text = stringResource(id = R.string.every_x_hours)) { onFrequencySelected("every_x_hours") }
        FrequencyOptionButton(text = stringResource(id = R.string.every_x_days_weeks_months)) { onFrequencySelected("every_x_days_weeks_months") }
        FrequencyOptionButton(text = stringResource(id = R.string.on_a_recurring_cycle)) { onFrequencySelected("recurring_cycle") }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.as_needed_frequency_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FrequencyOptionButton(text = stringResource(id = R.string.as_needed)) { onFrequencySelected("as_needed") }
        FrequencyOptionButton(text = stringResource(id = R.string.as_needed_then_every_x_hrs)) { onFrequencySelected("as_needed_interval") }
    }
}

@Composable
fun FrequencyOptionButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text)
    }
}