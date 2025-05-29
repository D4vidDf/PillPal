package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    val currentSearchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    var selectedMedicationId by rememberSaveable { mutableStateOf<Int?>(null) }

    // Local state for SearchBar active state
    var searchActive by rememberSaveable { mutableStateOf(false) }

    val medicationListClickHandler: (Int) -> Unit = { medicationId ->
        // When a medication is clicked, whether from main list or search results,
        // deactivate search and clear query to return to normal view.
        searchActive = false
        viewModel.updateSearchQuery("") // Clear search query
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
            modifier = modifier,
        ) { scaffoldInnerPadding ->
            Column(modifier = Modifier.padding(scaffoldInnerPadding).fillMaxSize()) {
                SearchBar(
                    query = currentSearchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = {
                        searchActive = false
                        // Keyboard hidden automatically by onSearch
                    },
                    active = searchActive,
                    onActiveChange = { isActive ->
                        searchActive = isActive
                        if (!isActive) {
                            viewModel.updateSearchQuery("") // Clear query when search becomes inactive
                        }
                    },
                    placeholder = { Text(stringResource(id = R.string.search_medications_placeholder)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (searchActive) 0.dp else 16.dp, vertical = 8.dp)
                ) {
                    // Content for search results - displayed when searchActive is true
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { medication ->
                            ListItem(
                                headlineContent = { Text(medication.name) },
                                // Add more details if needed, like dosage
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { medicationListClickHandler(medication.id) }
                            )
                        }
                    }
                }

                // Main medication list - only shown if search is not active
                if (!searchActive) {
                    // Define TopAppBar height for bottom padding in LazyColumn
                    val topAppBarHeight = 84.dp // This might need adjustment if SearchBar changes effective TopAppBar space
                    MedicationList(
                        medications = medications, // Display all medications from viewModel
                        onItemClick = { medication -> medicationListClickHandler(medication.id) },
                        isLoading = isLoading,
                        onRefresh = { viewModel.refreshMedications() },
                        modifier = Modifier.fillMaxSize(),
                        bottomContentPadding = topAppBarHeight
                    )
                }
            }
        }
    } else { // Medium or Expanded - List/Detail View
        Row(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                SearchBar(
                    query = currentSearchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { searchActive = false },
                    active = searchActive,
                    onActiveChange = { isActive ->
                        searchActive = isActive
                        if (!isActive) {
                            viewModel.updateSearchQuery("")
                        }
                    },
                    placeholder = { Text(stringResource(id = R.string.search_medications_placeholder)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (searchActive) 0.dp else 16.dp, vertical = 8.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { medication ->
                            ListItem(
                                headlineContent = { Text(medication.name) },
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { medicationListClickHandler(medication.id) }
                            )
                        }
                    }
                }
                if (!searchActive) {
                    MedicationList(
                        medications = medications, // Display all medications
                        onItemClick = { medication -> medicationListClickHandler(medication.id) },
                        isLoading = isLoading,
                        onRefresh = { viewModel.refreshMedications() },
                        modifier = Modifier.fillMaxSize(),
                        bottomContentPadding = 0.dp
                    )
                }
            }

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
                        onNavigateBack = {
                            selectedMedicationId = null
                            // Optionally, also ensure search is reset if a detail view is dismissed
                            // searchActive = false
                            // viewModel.updateSearchQuery("")
                        }
                    )
                }
            }
        }
    }
}
// Helper import, will be moved by IDE if not used explicitly but good for clarity
// import androidx.compose.foundation.clickable