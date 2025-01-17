package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.ui.components.BottomNavBar
import com.d4viddf.medicationreminder.ui.components.MedicationList
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddMedicationClick: () -> Unit,
    onMedicationClick: (Int) -> Unit,
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val medications = viewModel.medications.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Medications") })
        },

        bottomBar = {
            BottomNavBar(
                onHomeClick = { /* Handle Home button click */ },
                onSettingsClick = { /* Handle Settings button click */ },
                onAddClick = onAddMedicationClick,
                selectedIndex = 0
            )
        }
    ) { innerPadding ->
        MedicationList(
            medications = medications,
            onItemClick = { medication -> onMedicationClick(medication.id) },
            modifier = Modifier.padding(innerPadding)
        )
    }
}
