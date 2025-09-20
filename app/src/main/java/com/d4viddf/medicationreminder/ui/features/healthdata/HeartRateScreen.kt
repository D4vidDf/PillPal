package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.healthdata.component.LineChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateScreen(
    navController: NavController,
    viewModel: HeartRateViewModel = hiltViewModel()
) {
    val chartData by viewModel.chartData.collectAsState()
    val heartRateRecords by viewModel.heartRateRecords.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Heart Rate") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Heart Rate", style = MaterialTheme.typography.headlineSmall)
                    Text(text = "Last 7 days", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    LineChart(
                        data = chartData.lineChartData,
                        labels = chartData.labels,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
            items(heartRateRecords) { record ->
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "BPM: ${record.beatsPerMinute}")
                    Text(text = "Time: ${record.time.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm"))}")
                }
            }
        }
    }
}
