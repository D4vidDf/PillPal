package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// import androidx.hilt.navigation.compose.hiltViewModel // For later ViewModel integration
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme exists

enum class GraphViewType {
    WEEK, MONTH, YEAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationGraphScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit
    // viewModel: MedicationGraphViewModel = hiltViewModel() // Placeholder
) {
    var selectedViewType by remember { mutableStateOf(GraphViewType.WEEK) }

import com.d4viddf.medicationreminder.R // Added for R.string access

enum class GraphViewType {
    WEEK, MONTH, YEAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationGraphScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit
    // viewModel: MedicationGraphViewModel = hiltViewModel() // Placeholder
) {
    var selectedViewType by remember { mutableStateOf(GraphViewType.WEEK) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.medication_statistics_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.med_stats_navigate_back_cd)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // View Selector (Segmented Control like)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center // Or SpaceEvenly
            ) {
                GraphViewButton(
                    text = stringResource(id = R.string.graph_view_weekly),
                    isSelected = selectedViewType == GraphViewType.WEEK,
                    onClick = { selectedViewType = GraphViewType.WEEK }
                )
                GraphViewButton(
                    text = stringResource(id = R.string.graph_view_monthly),
                    isSelected = selectedViewType == GraphViewType.MONTH,
                    onClick = { selectedViewType = GraphViewType.MONTH }
                )
                GraphViewButton(
                    text = stringResource(id = R.string.graph_view_yearly),
                    isSelected = selectedViewType == GraphViewType.YEAR,
                    onClick = { selectedViewType = GraphViewType.YEAR }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graph Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Placeholder height
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val viewText = when (selectedViewType) {
                    GraphViewType.WEEK -> stringResource(id = R.string.graph_view_weekly)
                    GraphViewType.MONTH -> stringResource(id = R.string.graph_view_monthly)
                    GraphViewType.YEAR -> stringResource(id = R.string.graph_view_yearly)
                }
                Text(
                    text = stringResource(id = R.string.graph_placeholder_text, viewText, medicationId),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // TODO: Add more specific graph details or controls if needed
        }
    }
}

@Composable
private fun GraphViewButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (isSelected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    }

    val border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder // Corrected method name

    TextButton( // Using TextButton for a flatter look which works well for segmented controls
        onClick = onClick,
        shape = RoundedCornerShape(8.dp), // Consistent shape
        colors = colors,
        border = border,
        modifier = Modifier.padding(horizontal = 4.dp) // Some spacing between buttons
    ) {
        Text(text)
    }
}


@Preview(showBackground = true, name = "Medication Graph Screen - Week View")
@Composable
fun MedicationGraphScreenPreviewWeek() {
    AppTheme {
        MedicationGraphScreen(
            medicationId = 1,
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Medication Graph Screen - Month View")
@Composable
fun MedicationGraphScreenPreviewMonth() {
    var selectedViewType by remember { mutableStateOf(GraphViewType.MONTH) } // Simulate state for preview
    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Medication Statistics (Monthly)") }, // Changed title for preview
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Navigate Back") // Changed content description
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GraphViewButton("Weekly", selectedViewType == GraphViewType.WEEK) { selectedViewType = GraphViewType.WEEK } // Changed text
                    GraphViewButton("Monthly", selectedViewType == GraphViewType.MONTH) { selectedViewType = GraphViewType.MONTH } // Changed text
                    GraphViewButton("Yearly", selectedViewType == GraphViewType.YEAR) { selectedViewType = GraphViewType.YEAR } // Changed text
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                     val viewText = when (selectedViewType) { // Match placeholder text logic
                        GraphViewType.WEEK -> "Weekly"
                        GraphViewType.MONTH -> "Monthly"
                        GraphViewType.YEAR -> "Yearly"
                    }
                    Text(
                        text = "$viewText Graph Placeholder\n(for Medication ID: 1)", // Changed placeholder text
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
