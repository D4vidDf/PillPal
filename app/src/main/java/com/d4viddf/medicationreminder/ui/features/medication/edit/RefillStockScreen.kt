package com.d4viddf.medicationreminder.ui.features.medication.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme

@Composable
fun RefillStockScreen(
    viewModel: RefillStockViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    RefillStockScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAmountToAddChanged = viewModel::onAmountToAddChanged,
        onSave = viewModel::onSave
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefillStockScreenContent(
    uiState: RefillStockState,
    onNavigateBack: () -> Unit,
    onAmountToAddChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* No title */ },
                navigationIcon = {
                    FilledTonalIconButton (onClick = onNavigateBack, shapes = IconButtonDefaults.shapes(),
                    ) {
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.refill_stock_title, uiState.medicationUnit),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.refill_stock_current_stock, uiState.currentStock, uiState.medicationUnit),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(64.dp))
                    BasicTextField(
                        value = uiState.amountToAdd,
                        onValueChange = onAmountToAddChanged,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 64.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.amountToAdd.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.refill_stock_placeholder, uiState.medicationUnit),
                                        style = LocalTextStyle.current.copy(
                                            fontSize = 64.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val total = uiState.currentStock + (uiState.amountToAdd.toIntOrNull() ?: 0)
                    Text(
                        text = stringResource(R.string.refill_stock_total, total, uiState.medicationUnit),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    onClick = {
                        onSave()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(ButtonDefaults.MediumContainerHeight),
                    shapes = ButtonDefaults.shapes(),
                    enabled = uiState.amountToAdd.isNotBlank() && (uiState.amountToAdd.toIntOrNull()
                        ?: 0) > 0
                ) {
                    Text(text = stringResource(R.string.add))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RefillStockScreenPreview() {
    AppTheme {
        val previewState = RefillStockState(
            isLoading = false,
            currentStock = 90,
            amountToAdd = "10",
            medicationUnit = "pills"
        )
        RefillStockScreenContent(
            uiState = previewState,
            onNavigateBack = {},
            onAmountToAddChanged = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RefillEmptyStockScreenPreview() {
    AppTheme {
        val previewState = RefillStockState(
            isLoading = false,
            currentStock = 90,
            amountToAdd = "0",
            medicationUnit = "pills"
        )
        RefillStockScreenContent(
            uiState = previewState,
            onNavigateBack = {},
            onAmountToAddChanged = {},
            onSave = {}
        )
    }
}