package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.components.MedicationList
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddMedicationClick: () -> Unit,
    onMedicationClick: (Int) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
    viewModel: MedicationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier // This modifier comes from NavHost, potentially with padding
) {
    val medications by viewModel.medications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedMedicationId by rememberSaveable { mutableStateOf<Int?>(null) }

    val medicationListClickHandler: (Int) -> Unit = { medicationId ->
        if (widthSizeClass == WindowWidthSizeClass.Compact) {
            onMedicationClick(medicationId)
        } else {
            selectedMedicationId = medicationId
        }
    }

    if (widthSizeClass == WindowWidthSizeClass.Compact) {
        // For compact screens, HomeScreen provides its own Scaffold for specific TopAppBar and FAB
        // The `modifier` passed in from AppNavigation (which includes padding from MedicationReminderApp's Scaffold)
        // is applied to this Scaffold.
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            modifier = modifier, // Remove .nestedScroll(scrollBehavior.nestedScrollConnection)
            // The TopAppBar itself will use the scrollBehavior,
            // and the connection will be passed to MedicationList.
            // topBar = { TopAppBar(title = { Text("Medications") }, scrollBehavior = scrollBehavior) } // Example TopAppBar
        ) { scaffoldInnerPadding -> // This is the padding provided by THIS HomeScreen's Scaffold
            Box(modifier = Modifier.padding(scaffoldInnerPadding).fillMaxSize()) { // Outer Box handles padding and fillMaxSize
                // Define TopAppBar height for bottom padding in LazyColumn
                val topAppBarHeight = 64.dp 

                MedicationList(
                    medications = medications,
                    onItemClick = { medication -> medicationListClickHandler(medication.id) },
                    isLoading = isLoading,
                    onRefresh = { viewModel.refreshMedications() },
                    modifier = Modifier.fillMaxSize(), // MedicationList (PullToRefreshBox) fills this Box
                    externalNestedScrollConnection = scrollBehavior.nestedScrollConnection, // Pass the connection
                    bottomContentPadding = topAppBarHeight // Pass the height for bottom padding
                )
            }
        }
    } else { // Medium or Expanded - List/Detail View
        Row(modifier = modifier.fillMaxSize()) { // Modifier from NavHost applied to the Row
            // Medication List Pane
            Box(
                modifier = Modifier
                    .weight(1f) // Adjust weight as needed, e.g., 1f or 0.4f for 40%
                    .fillMaxHeight()
            ) {
                MedicationList(
                    medications = medications,
                    onItemClick = { medication -> medicationListClickHandler(medication.id) },
                    isLoading = isLoading,
                    onRefresh = { viewModel.refreshMedications() },
                    modifier = Modifier.fillMaxSize() // MedicationList fills this Box
                )
            }

            // Detail Pane
            Box(
                modifier = Modifier
                    .weight(1.5f) // Adjust weight as needed, e.g., 1.5f or 0.6f for 60%
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (selectedMedicationId == null) {
                    Text(stringResource(id = R.string.select_medication_placeholder))
                } else {
                    MedicationDetailsScreen(
                        medicationId = selectedMedicationId!!,
                        onNavigateBack = { selectedMedicationId = null }
                    )
                }
            }
        }
    }
}