package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme

@Composable
fun LowStockReminderScreen(
    viewModel: LowStockReminderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        NumberPickerDialog(
            onDismiss = { showDialog = false },
            onConfirm = {
                viewModel.onDaysChanged(it)
                showDialog = false
            },
            initialValue = uiState.selectedDays,
            range = 1..31
        )
    }

    LowStockReminderScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onDaysChanged = viewModel::onDaysChanged,
        onSave = viewModel::onSave,
        onDisable = viewModel::onDisable,
        onNumberClick = {
            showDialog = true
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LowStockReminderScreenContent(
    uiState: LowStockReminderState,
    onNavigateBack: () -> Unit,
    onDaysChanged: (Int) -> Unit,
    onSave: () -> Unit,
    onDisable: () -> Unit,
    onNumberClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* No title */ },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.low_stock_reminder_title),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box (modifier = Modifier.fillMaxWidth(0.4f)) {
                            StockReminderScroller(
                                selectedDays = uiState.selectedDays,
                                range = 1..31,
                                onDaysChanged = onDaysChanged,
                                onNumberClick = onNumberClick
                            )
                        }
                        Text(
                            text = stringResource(R.string.days_before),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    uiState.runsOutInDays?.let {
                        val medicationFirstName = uiState.medicationName.substringBefore(" ")
                        Text(
                            text = stringResource(R.string.low_stock_reminder_runs_out, medicationFirstName, it),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            onDisable()
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(ButtonDefaults.MediumContainerHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(stringResource(R.string.disable))
                    }
                    Button(
                        onClick = {
                            onSave()
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .weight(2f)
                            .heightIn(ButtonDefaults.MediumContainerHeight)
                    ) {
                        Text(stringResource(R.string.dialog_done_button))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LowStockReminderScreenPreview() {
    AppTheme {
        val previewState = LowStockReminderState(
            isLoading = false,
            medicationName = "Mestinon",
            runsOutInDays = 18,
            selectedDays = 9
        )
        LowStockReminderScreenContent(
            uiState = previewState,
            onNavigateBack = {},
            onDaysChanged = {},
            onSave = {},
            onDisable = {},
            onNumberClick = {}
        )
    }
}
