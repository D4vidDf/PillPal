package com.d4viddf.medicationreminder.ui.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val weightGoal by viewModel.weightGoalMax.collectAsState()
    val heartRateGoal by viewModel.heartRateGoalMax.collectAsState()
    var weightGoalState by remember { mutableStateOf(weightGoal.toString()) }
    var heartRateGoalState by remember { mutableStateOf(heartRateGoal.toString()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(weightGoal) {
        weightGoalState = weightGoal.toString()
    }

    LaunchedEffect(heartRateGoal) {
        heartRateGoalState = heartRateGoal.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.goals)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = weightGoalState,
                onValueChange = { weightGoalState = it },
                label = { Text("Weight Goal (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = heartRateGoalState,
                onValueChange = { heartRateGoalState = it },
                label = { Text("Max Heart Rate Goal (bpm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    scope.launch {
                        viewModel.setWeightGoalMax(weightGoalState.toInt())
                        viewModel.setHeartRateGoalMax(heartRateGoalState.toInt())
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.save))
            }
        }
    }
}
