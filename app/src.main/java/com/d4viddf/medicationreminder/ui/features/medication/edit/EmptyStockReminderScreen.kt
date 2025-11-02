package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import kotlin.math.abs

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
            initialValue = uiState.selectedDays
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
                StockReminderScroller(
                    title = stringResource(R.string.empty_stock_reminder_title),
                    subtitle = stringResource(R.string.days),
                    selectedDays = uiState.selectedDays,
                    range = 0..31,
                    onDaysChanged = onDaysChanged,
                    onNumberClick = onNumberClick
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            onDisable()
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(stringResource(R.string.disable))
                    }
                    Button(
                        onClick = {
                            onSave()
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(2f)
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
            selectedDays = 7
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
