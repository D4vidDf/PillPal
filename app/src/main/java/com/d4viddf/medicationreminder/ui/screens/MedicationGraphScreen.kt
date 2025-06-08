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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R // Moved import to top
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme exists
import com.d4viddf.medicationreminder.viewmodel.MedicationGraphViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class GraphViewType {
    WEEK, MONTH, YEAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationGraphScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MedicationGraphViewModel? = null // Made nullable for preview
) {
    var selectedViewType by remember { mutableStateOf(GraphViewType.WEEK) }
    // Provide sample data for preview if viewModel is null
    val medicationName by viewModel?.medicationName?.collectAsState() ?: remember { mutableStateOf("Sample Medication (Preview)") }
    val graphData by viewModel?.graphData?.collectAsState() ?: remember {
        mutableStateOf(
            mapOf(
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to 2,
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(1).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to 3,
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(2).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to 1,
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(3).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to 4,
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(4).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to 2,
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(5).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to 0,
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(6).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to 1
            )
        )
    }

    LaunchedEffect(medicationId, selectedViewType, viewModel) { // Added viewModel to keys
        if (viewModel != null) { // Only load if viewModel is available
            when (selectedViewType) {
                GraphViewType.WEEK -> {
                    val today = LocalDate.now()
                    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val currentWeekDays = List(7) { i -> monday.plusDays(i.toLong()) }
                    viewModel.loadWeeklyGraphData(medicationId, currentWeekDays)
                }
                GraphViewType.MONTH -> {
                    viewModel.loadWeeklyGraphData(medicationId, emptyList()) // Clear data for now
                }
                GraphViewType.YEAR -> {
                    viewModel.loadWeeklyGraphData(medicationId, emptyList()) // Clear data for now
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(medicationName.ifEmpty { stringResource(id = R.string.medication_statistics_title) }) },
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
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

            // Bar Chart Implementation
            val maxCount = graphData.values.maxOrNull() ?: 1 // Avoid division by zero, ensure at least 1
            val todayShortName = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            if (selectedViewType == GraphViewType.WEEK && graphData.isEmpty()) {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp) // Match approx height of chart
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(id = R.string.loading_graph_data)) // Or "No data for this week"
                }
            } else if (selectedViewType == GraphViewType.WEEK) {
                Column(modifier = Modifier.fillMaxWidth()) {
                     Text(
                        stringResource(id = R.string.weekly_doses_taken_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp) // Increased height for labels + bars
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom // Align bars to bottom
                    ) {
                        graphData.forEach { (dayLabel, count) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f) // Distribute space equally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(if (maxCount > 0) ((count.toFloat() / maxCount.toFloat()) * 180).dp else 0.dp) // Corrected Dp calculation
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            if (dayLabel.equals(todayShortName, ignoreCase = true))
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.secondaryContainer
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(dayLabel, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                 // Placeholder for Month/Year views
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val viewText = when (selectedViewType) {
                        GraphViewType.MONTH -> stringResource(id = R.string.graph_view_monthly)
                        GraphViewType.YEAR -> stringResource(id = R.string.graph_view_yearly)
                        else -> "" // Should not happen
                    }
                    Text(
                        text = stringResource(id = R.string.graph_placeholder_text, viewText, medicationId),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
            onNavigateBack = {},
            viewModel = null // Pass null for preview to use sample data
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Medication Graph Screen - Month View")
@Composable
fun MedicationGraphScreenPreviewMonth() {
    // This preview will also use the default data from MedicationGraphScreen when viewModel is null,
    // but we can simulate the 'Month' selection for the placeholder text.
    // For a true 'Month' data preview, a more complex setup or a dedicated preview composable would be needed.
    AppTheme {
        var selectedViewType by remember { mutableStateOf(GraphViewType.MONTH) } // To control the placeholder text
        val medicationName by remember { mutableStateOf("Sample Medication (Preview)") }
        val graphData by remember { mutableStateOf(emptyMap<String, Int>()) } // Empty for month/year for now

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(medicationName) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.med_stats_navigate_back_cd))
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
                    GraphViewButton(stringResource(id = R.string.graph_view_weekly), selectedViewType == GraphViewType.WEEK) { selectedViewType = GraphViewType.WEEK }
                    GraphViewButton(stringResource(id = R.string.graph_view_monthly), selectedViewType == GraphViewType.MONTH) { selectedViewType = GraphViewType.MONTH }
                    GraphViewButton(stringResource(id = R.string.graph_view_yearly), selectedViewType == GraphViewType.YEAR) { selectedViewType = GraphViewType.YEAR }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Placeholder for Month/Year views as per original logic
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val viewText = when (selectedViewType) {
                        GraphViewType.MONTH -> stringResource(id = R.string.graph_view_monthly)
                        GraphViewType.YEAR -> stringResource(id = R.string.graph_view_yearly)
                        else -> stringResource(id = R.string.graph_view_weekly) // Default to weekly if state is somehow WEEK
                    }
                    Text(
                        text = stringResource(id = R.string.graph_placeholder_text, viewText, 1), // medicationId for preview is 1
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
