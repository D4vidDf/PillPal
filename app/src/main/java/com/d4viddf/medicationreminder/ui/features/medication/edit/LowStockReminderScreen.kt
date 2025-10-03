package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.ExperimentalFoundationApi
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
fun LowStockReminderScreen(
    viewModel: LowStockReminderViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    LowStockReminderScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onDaysChanged = viewModel::onDaysChanged,
        onSave = viewModel::onSave,
        onDisable = viewModel::onDisable
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LowStockReminderScreenContent(
    uiState: LowStockReminderState,
    onNavigateBack: () -> Unit,
    onDaysChanged: (Int) -> Unit,
    onSave: () -> Unit,
    onDisable: () -> Unit
) {
    val listState = rememberLazyListState()

    val selectedDay by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                uiState.selectedDays
            } else {
                val viewportCenter = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                val centerItem = visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                centerItem?.index?.coerceIn(1, 31) ?: uiState.selectedDays
            }
        }
    }

    LaunchedEffect(selectedDay) {
        if (selectedDay != uiState.selectedDays) {
            onDaysChanged(selectedDay)
        }
    }

    LaunchedEffect(Unit) {
        listState.scrollToItem(uiState.selectedDays)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* No title */ },
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
                        text = stringResource(R.string.low_stock_reminder_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 80.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
                            ) {
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                                items(31) { index ->
                                    val day = index + 1
                                    Text(
                                        text = day.toString(),
                                        fontSize = if (selectedDay == day) 48.sp else 32.sp,
                                        color = if (selectedDay == day) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                        Text(
                            text = "days before",
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
fun LowStockReminderScreenPreview() {
    AppTheme {
        val previewState = LowStockReminderState(
            isLoading = false,
            medicationName = "Mestinon",
            runsOutInDays = 18,
            selectedDays = 7
        )
        LowStockReminderScreenContent(
            uiState = previewState,
            onNavigateBack = {},
            onDaysChanged = {},
            onSave = {},
            onDisable = {}
        )
    }
}