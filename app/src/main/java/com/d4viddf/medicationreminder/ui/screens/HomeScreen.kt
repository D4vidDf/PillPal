package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.ui.components.MedicationList
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import com.d4viddf.medicationreminder.ui.components.AppHorizontalFloatingToolbar
import com.d4viddf.medicationreminder.ui.components.AppNavigationRail
import androidx.compose.material3.FabPosition
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddMedicationClick: () -> Unit,
    onMedicationClick: (Int) -> Unit, // This will be the navigation action for compact
    onNavigateToSettings: () -> Unit,
    onNavigateToCalendar: () -> Unit, // Added
    onNavigateToProfile: () -> Unit,  // Added
    widthSizeClass: WindowWidthSizeClass,
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val medications = viewModel.medications.collectAsState().value
    val currentRoute = "home" // Placeholder for actual route detection
    var selectedMedicationId by rememberSaveable { mutableStateOf<Int?>(null) }

    val medicationListClickHandler: (Int) -> Unit = { medicationId ->
        if (widthSizeClass == WindowWidthSizeClass.Compact) {
            onMedicationClick(medicationId) // Navigate for compact
        } else {
            selectedMedicationId = medicationId // Update state for larger screens
        }
    }

    if (widthSizeClass == WindowWidthSizeClass.Compact) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(id = R.string.medications_title), fontWeight = FontWeight.Bold,style = MaterialTheme.typography.headlineLarge) })
            },
            floatingActionButton = {
                AppHorizontalFloatingToolbar(
                    onHomeClick = { Log.d("HomeScreen", "Home clicked") /* Already home */ },
                    onCalendarClick = onNavigateToCalendar, // Updated
                    onProfileClick = onNavigateToProfile,   // Updated
                    onSettingsClick = onNavigateToSettings,
                    onAddClick = onAddMedicationClick
                )
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { innerPadding ->
            MedicationList(
                medications = medications,
                onItemClick = { medication -> medicationListClickHandler(medication.id) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    } else { // Medium or Expanded
        Row(Modifier.fillMaxSize()) {
            AppNavigationRail(
                onHomeClick = { Log.d("HomeScreen", "Home clicked via Rail") /* Already home */ },
                onCalendarClick = onNavigateToCalendar, // Updated
                onProfileClick = onNavigateToProfile,   // Updated
                onSettingsClick = onNavigateToSettings,
                onAddClick = onAddMedicationClick,
                currentRoute = currentRoute
            )
            // Medication List Pane
            Box(modifier = Modifier.weight(1f)) { // Or Modifier.width(360.dp)
                // The Scaffold for the list can be simpler or removed if TopAppBar is not needed here specifically
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text(stringResource(id = R.string.medications_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge) })
                    }
                ) { innerPadding ->
                    MedicationList(
                        medications = medications,
                        onItemClick = { medication -> medicationListClickHandler(medication.id) },
                        modifier = Modifier.padding(innerPadding).fillMaxHeight()
                    )
                }
            }

            // Detail Pane
            Box(modifier = Modifier.weight(2f).fillMaxHeight()) {
                if (selectedMedicationId == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(id = R.string.select_medication_placeholder))
                    }
                } else {
                    MedicationDetailsScreen(
                        medicationId = selectedMedicationId!!,
                        onNavigateBack = { selectedMedicationId = null } // Clear selection
                        // Consider adding isEmbedded = true if TopAppBar needs to change
                    )
                }
            }
        }
    }
}
