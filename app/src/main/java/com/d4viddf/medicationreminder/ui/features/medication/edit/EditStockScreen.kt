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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.utils.getMedicationTypeStringResource

@Composable
fun EditStockScreen(
    navController: NavController,
    viewModel: EditStockViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadMedicationData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    EditStockScreenContent(
        uiState = uiState,
        navController = navController,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditStockScreenContent(
    uiState: EditStockUiState,
    navController: NavController,
    onNavigateBack: () -> Unit
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
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.medicationName.split(" ")[0],
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.remainingStock.toString(),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold
                )
                val medicationUnit = uiState.medicationUnit?.let {
                    stringResource(id = getMedicationTypeStringResource(it.toInt()))
                } ?: ""
                Text(
                    text = stringResource(R.string.edit_stock_units_left, medicationUnit),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(48.dp))
                FilledTonalButton(
                    onClick = {
                        navController.navigate(Screen.RefillStock.createRoute(uiState.medicationId))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(ButtonDefaults.LargeContainerHeight),
                    shapes = ButtonDefaults.shapes()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.edit_stock_refill_button))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { /* TODO: Handle Edit Stock click */ },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(ButtonDefaults.MediumContainerHeight),
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.edit))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    FilledTonalIconButton(
                        onClick = {
                            navController.navigate(Screen.StockReminder.createRoute(uiState.medicationId))
                        },
                        modifier = Modifier.weight(1f)
                            .heightIn(ButtonDefaults.MediumContainerHeight),
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                        ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = stringResource(R.string.edit_stock_reminders_button_cd))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditStockScreenPreview() {
    AppTheme {
        val previewState = EditStockUiState(
            isLoading = false,
            medicationId = 1,
            medicationName = "Mestinon",
            remainingStock = 90,
            medicationUnit = "pills"
        )
        EditStockScreenContent(
            uiState = previewState,
            navController = rememberNavController(),
            onNavigateBack = {}
        )
    }
}