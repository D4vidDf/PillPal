package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun EmptyStockReminderScreen(
    viewModel: EmptyStockReminderViewModel = hiltViewModel(),
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
            range = 0..31
        )
    }

    EmptyStockReminderScreenContent(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyStockReminderScreenContent(
    uiState: EmptyStockReminderState,
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
                CircularProgressIndicator()
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
                        text = stringResource(R.string.empty_stock_reminder_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val noneString = stringResource(id = R.string.none)
                        Box(modifier = Modifier.fillMaxWidth(0.4f)) {
                            StockReminderScroller(
                                selectedDays = uiState.selectedDays,
                                range = 0..31,
                                onDaysChanged = onDaysChanged,
                                onNumberClick = onNumberClick,
                                labelFormatter = { day ->
                                    if (day == 0) noneString else day.toString()
                                }
                            )
                        }
                        Text(
                            text = stringResource(R.string.days),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 16.dp)
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
                            .heightIn(ButtonDefaults.MinHeight),
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
                            .heightIn(ButtonDefaults.MinHeight)
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
fun EmptyStockReminderScreenPreview() {
    AppTheme {
        val previewState = EmptyStockReminderState(
            isLoading = false,
            medicationName = "Mestinon",
            selectedDays = 2
        )
        EmptyStockReminderScreenContent(
            uiState = previewState,
            onNavigateBack = {},
            onDaysChanged = {},
            onSave = {},
            onDisable = {},
            onNumberClick = {}
        )
    }
}
