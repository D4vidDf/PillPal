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
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    onBackPressed: () -> Unit,
    viewModel: WeightViewModel = hiltViewModel()
) {
    val weightUiState by viewModel.weightUiState.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weight_tracker)) },
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
                data = weightUiState.chartData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                showPoints = true // We'll enable points on the line chart
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.history), style = MaterialTheme.typography.titleLarge)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(weightUiState.weightLogs) { weightEntry ->
                    ListItem(
                        headlineContent = { Text("${weightEntry.weight} kg") },
                        supportingContent = {
                            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                            Text(weightEntry.date.format(formatter))
                        }
                    )
                }
            }
        }
    }
}
