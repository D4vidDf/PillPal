package com.d4viddf.medicationreminder.ui.features.medication.dosage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDosageChangeScreen(
    navController: NavController,
    medicationId: Int,
    viewModel: ScheduleDosageChangeViewModel = hiltViewModel()
) {
    var newDosage by remember { mutableStateOf("") }
    var newStartDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Dosage Change") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            OutlinedTextField(
                value = newDosage,
                onValueChange = { newDosage = it },
                label = { Text("New Dosage") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            DatePicker(state = datePickerState, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedDate = LocalDate.ofEpochDay(it / (1000 * 60 * 60 * 24))
                        viewModel.scheduleDosageChange(medicationId, newDosage, selectedDate)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = newDosage.isNotBlank()
            ) {
                Text("Confirm")
            }
        }
    }
}