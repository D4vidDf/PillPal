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
// Removed AppHorizontalFloatingToolbar and AppNavigationRail imports
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
// Removed Log import as navigation lambdas are removed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
// Removed width import as it's not used directly for fixed width here
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
// Removed unit.dp import as it's not used directly for fixed width here
import com.d4viddf.medicationreminder.R
// Removed unused Material 1 pull-to-refresh imports:
// import androidx.compose.material.pullrefresh.PullRefreshIndicator
// import androidx.compose.material.pullrefresh.pullRefresh
// import androidx.compose.material.pullrefresh.rememberPullRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    // Removed onNavigateToSettings, onNavigateToCalendar, onNavigateToProfile
    onAddMedicationClick: () -> Unit, // Kept from previous logic, though now handled by App level nav components
    onMedicationClick: (Int) -> Unit, // For compact navigation or detail view trigger
    widthSizeClass: WindowWidthSizeClass,
    viewModel: MedicationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier // Added modifier to be used by NavHost,

) {
    val medications = viewModel.medications.collectAsState().value
    val isLoading by viewModel.isLoading.collectAsState() // Collect isLoading state
    var selectedMedicationId by rememberSaveable { mutableStateOf<Int?>(null) }

    val medicationListClickHandler: (Int) -> Unit = { medicationId ->
        if (widthSizeClass == WindowWidthSizeClass.Compact) {
            onMedicationClick(medicationId) // Navigate to full details screen
        } else {
            selectedMedicationId = medicationId // Show in detail pane
        }
    }

    if (widthSizeClass == WindowWidthSizeClass.Compact) {
        // For compact, just the list. The Scaffold with Toolbar is in MedicationReminderApp
        // Apply the modifier which includes padding from the parent Scaffold
        MedicationList(
            medications = medications,
            onItemClick = { medication -> medicationListClickHandler(medication.id) },
            isLoading = isLoading, // Pass isLoading
            onRefresh = { viewModel.refreshMedications() }, // Pass refresh callback
            modifier = modifier.fillMaxSize() // Ensure it fills the space given by NavHost
        )
    } else { // Medium or Expanded - List/Detail View
        // The Row itself will be the content of the NavHost's composable for HomeScreen.
        // The parent padding (if any, though not expected here due to isMainScaffold=false)
        // would be applied to the NavHost, so this Row should fill the NavHost's content area.
        Row(modifier = modifier.fillMaxSize()) {
            // Medication List Pane
            // No separate Scaffold or TopAppBar here, as they are handled by MedicationReminderApp or not shown for list part
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                MedicationList(
                    medications = medications,
                    onItemClick = { medication -> medicationListClickHandler(medication.id) },
                    isLoading = isLoading, // Pass isLoading
                    onRefresh = { viewModel.refreshMedications() }, // Pass refresh callback
                    modifier = Modifier.fillMaxSize() // Fill the Box
                )
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
                    )
                }
            }
        }
    }
}
