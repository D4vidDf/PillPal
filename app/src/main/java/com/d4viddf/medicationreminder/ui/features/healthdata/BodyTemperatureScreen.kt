package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.healthdata.component.DateRangeSelector
import com.d4viddf.medicationreminder.ui.features.healthdata.component.LineChart
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyTemperatureScreen(
    onBackPressed: () -> Unit,
    viewModel: BodyTemperatureViewModel = hiltViewModel()
) {
    val temperatureUiState by viewModel.temperatureUiState.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.body_temperature_tracker)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            DateRangeSelector(
                selectedRange = timeRange,
                onRangeSelected = { viewModel.setTimeRange(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            LineChart(
                data = temperatureUiState.chartData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                showLines = false, // This will make it a point chart
                showPoints = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.history), style = MaterialTheme.typography.titleLarge)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(temperatureUiState.temperatureLogs) { tempEntry ->
                    ListItem(
                        headlineContent = { Text("${tempEntry.temperature}°C") },
                        supportingContent = {
                            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                            Text(tempEntry.date.format(formatter))
                        }
                    )
                }
            }
        }
    }
}
