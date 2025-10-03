package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme

@Composable
fun StockReminderScreen(
    viewModel: StockReminderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToLowStockSettings: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    StockReminderScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToLowStockSettings = { onNavigateToLowStockSettings(viewModel.medicationId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockReminderScreenContent(
    uiState: StockReminderState,
    onNavigateBack: () -> Unit,
    onNavigateToLowStockSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stock_reminder_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ReminderRow(
                        title = stringResource(R.string.stock_reminder_low_stock_title),
                        subtitle = stringResource(R.string.stock_reminder_low_stock_subtitle),
                        value = uiState.lowStockReminderValue,
                        onClick = onNavigateToLowStockSettings
                    )
                    ReminderRow(
                        title = stringResource(R.string.stock_reminder_empty_stock_title),
                        subtitle = stringResource(R.string.stock_reminder_empty_stock_subtitle),
                        value = uiState.emptyStockReminderValue,
                        onClick = { /* Not implemented */ }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StockReminderScreenPreview() {
    AppTheme {
        val previewState = StockReminderState(
            lowStockReminderValue = "7 days",
            emptyStockReminderValue = "None"
        )
        StockReminderScreenContent(
            uiState = previewState,
            onNavigateBack = {},
            onNavigateToLowStockSettings = {}
        )
    }
}

@Composable
private fun ReminderRow(
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}