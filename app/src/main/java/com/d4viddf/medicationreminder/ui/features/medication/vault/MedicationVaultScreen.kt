package com.d4viddf.medicationreminder.ui.features.medication.vault

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
// Updated import for MedicationList
import com.d4viddf.medicationreminder.ui.features.medication.vault.components.MedicationList
import com.d4viddf.medicationreminder.ui.features.medication.details.MedicationDetailsScreen
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.utils.PermissionUtils
import kotlinx.coroutines.launch


// Context.findActivity() extension function
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MedicationVaultScreen(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass,
    sharedTransitionScope: SharedTransitionScope?, // For potential transitions if navigating out
    animatedVisibilityScope: AnimatedVisibilityScope?, // Same as above
    viewModel: MedicationVaultViewModel = hiltViewModel(),
    hostPaddingValues: PaddingValues = PaddingValues(0.dp) // New parameter for padding from hosting Scaffold
) {
    val medicationState by viewModel.medicationsState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentSearchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Int?>()
    val mainMedicationListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var searchActive by rememberSaveable { mutableStateOf(false) }

    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText: ArrayList<String>? = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenText.isNullOrEmpty()) {
                viewModel.updateSearchQuery(spokenText[0])
                searchActive = true
            }
        }
    }

    val onMedicationClick: (Int) -> Unit = { medicationId ->
        searchActive = false // Close search bar
        viewModel.updateSearchQuery("") // Clear query

        if (widthSizeClass == WindowWidthSizeClass.Compact) {
            // For compact, navigate to full screen detail using NavController
            // Enable shared transition if scope is available
            val enableTransition = sharedTransitionScope != null && animatedVisibilityScope != null
            navController.navigate(Screen.MedicationDetails.createRoute(medicationId, enableSharedTransition = enableTransition))
        } else {
            // For larger screens, show in detail pane
            coroutineScope.launch {
                scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, medicationId)
            }
        }
    }

    Scaffold(
    ) { paddingValues -> // Consume paddingValues
        NavigableListDetailPaneScaffold(
            modifier = Modifier.fillMaxSize(), // Apply padding from MedicationVaultScreen's own Scaffold
            navigator = scaffoldNavigator,
            listPane = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding() // For status bar
                        .padding(bottom = paddingValues.calculateBottomPadding()) // For host's bottom bar (e.g., HorizontalFloatingToolbar)
                ) {
                    SearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Apply padding similar to old HomeScreen.kt
                            .padding(
                                horizontal = if (searchActive && widthSizeClass == WindowWidthSizeClass.Compact) 0.dp else 16.dp,
                                vertical = 8.dp
                            ),
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = currentSearchQuery,
                                onQueryChange = { viewModel.updateSearchQuery(it) },
                                onSearch = { searchActive = false },
                                expanded = searchActive,
                                onExpandedChange = { isActive ->
                                    searchActive = isActive
                                    if (!isActive) viewModel.updateSearchQuery("")
                                },
                                placeholder = { Text(stringResource(id = R.string.search_medications_placeholder)) },
                                leadingIcon = {
                                    Icon(painter = painterResource(id = R.drawable.ic_search), contentDescription = stringResource(id = R.string.search_icon_content_description))
                                },
                                trailingIcon = {
                                    if (currentSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(painter = painterResource(id = R.drawable.rounded_close_24), contentDescription = stringResource(id = R.string.clear_search_query_button_description))
                                        }
                                    } else {
                                        val localContext = LocalContext.current
                                        IconButton(onClick = {
                                            val activity = localContext.findActivity()
                                            if (activity != null) {
                                                PermissionUtils.requestRecordAudioPermission(
                                                    activity = activity,
                                                    onAlreadyGranted = {
                                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                            putExtra(RecognizerIntent.EXTRA_PROMPT, localContext.getString(R.string.speech_prompt_text))
                                                        }
                                                        speechRecognitionLauncher.launch(intent)
                                                    },
                                                    onRationaleNeeded = { Log.i("MedVaultScreen", "Record audio permission rationale needed.") }
                                                )
                                            } else { Log.e("MedVaultScreen", "Could not find Activity context.") }
                                        }) {
                                            Icon(painter = painterResource(id = R.drawable.rounded_mic_24), contentDescription = stringResource(id = R.string.microphone_icon_content_description))
                                        }
                                    }
                                },
                            )
                        },
                        expanded = searchActive,
                        onExpandedChange = { isActive ->
                            searchActive = isActive
                            if (!isActive) viewModel.updateSearchQuery("")
                        }
                    ) { // Search results content
                        val searchResultsListState = rememberLazyListState()
                        LazyColumn(
                            state = searchResultsListState,
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                        ) {
                            itemsIndexed(searchResults, key = { _, medWithDosage -> medWithDosage.medication.id }) { index, medWithDosage ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clickable {
                                            coroutineScope.launch {
                                                searchResultsListState.animateScrollToItem(index)
                                                onMedicationClick(medWithDosage.medication.id)
                                            }
                                        }
                                        .then(
                                            if (sharedTransitionScope != null && animatedVisibilityScope != null && widthSizeClass == WindowWidthSizeClass.Compact) {
                                                with(sharedTransitionScope) {
                                                    Modifier.sharedElement(
                                                        rememberSharedContentState(key = "medication-vault-bg-${medWithDosage.medication.id}"),
                                                        animatedVisibilityScope
                                                    )
                                                }
                                            } else Modifier
                                        )
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text(
                                            text = medWithDosage.medication.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.then(
                                                if (sharedTransitionScope != null && animatedVisibilityScope != null && widthSizeClass == WindowWidthSizeClass.Compact) {
                                                    with(sharedTransitionScope) {
                                                        Modifier.sharedElement(
                                                            rememberSharedContentState(key = "medication-vault-name-${medWithDosage.medication.id}"),
                                                            animatedVisibilityScope
                                                        )
                                                    }
                                                } else Modifier
                                            )
                                        )
                                        if (!medWithDosage.dosage?.dosage.isNullOrBlank()) {
                                            Text(
                                                text = medWithDosage.dosage!!.dosage,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } // End of SearchBar content

                    if (!searchActive) {
                        MedicationList(
                            medicationState = medicationState, // Show all medications when not searching
                            isRefreshing = isRefreshing,
                            onItemClick = { medWithDosage, index ->
                                if (widthSizeClass == WindowWidthSizeClass.Compact && sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    coroutineScope.launch {
                                        mainMedicationListState.animateScrollToItem(index)
                                        onMedicationClick(medWithDosage.medication.id)
                                    }
                                } else {
                                    onMedicationClick(medWithDosage.medication.id)
                                }
                            },
                            onRefresh = viewModel::refreshMedications,
                            enableCardTransitions = (widthSizeClass == WindowWidthSizeClass.Compact),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            modifier = Modifier.fillMaxSize(),
                            listState = mainMedicationListState,
                            bottomContentPadding = 0.dp // Added: Adjust if necessary for FAB overlap
                            // itemSharedElementKeyPrefix removed as it's not a valid parameter
                        )
                    }
                }
            },
            detailPane = {
                val selectedMedicationIdForDetail = scaffoldNavigator.currentDestination?.contentKey
                if (selectedMedicationIdForDetail != null) {
                    MedicationDetailsScreen(
                        medicationId = selectedMedicationIdForDetail,
                        navController = navController,
                        onNavigateBack = { coroutineScope.launch { scaffoldNavigator.navigateBack() } },
                        // Shared transition scope for detail pane is typically null or different
                        sharedTransitionScope = null,
                        animatedVisibilityScope = null, // Detail pane manages its own animations
                        isHostedInPane = true,
                        widthSizeClass = widthSizeClass,
                         onNavigateToAllSchedules = { medId, colorName ->
                            navController.navigate(Screen.AllSchedules.createRoute(medId, colorName, true))
                        },
                        onNavigateToMedicationHistory = { medId, colorName ->
                            navController.navigate(Screen.MedicationHistory.createRoute(medId, colorName))
                        },
                        onNavigateToMedicationGraph = { medId, colorName ->
                            navController.navigate(Screen.MedicationGraph.createRoute(medId, colorName))
                        },
                        onNavigateToMedicationInfo = { medId, colorName ->
                            navController.navigate(Screen.MedicationInfo.createRoute(medId, colorName))
                        }
                        // Ensure MedicationDetailsScreen can accept a graphViewModel if needed or remove if not
                        // graphViewModel = hiltViewModel() // If MedicationDetailsScreen needs its own GraphVM
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(id = R.string.select_medication_placeholder))
                    }
                }
            }
        )
    }
}

// Context.findActivity() extension function (already defined in HomeScreen.kt, ensure it's accessible or defined commonly)
// For brevity, assuming it's available. If not, it should be added here or in a common utils file.
/*
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
*/

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, name = "Compact MedicationVaultScreen")
@Composable
fun MedicationVaultScreenCompactPreview() {
    AppTheme {
        MedicationVaultScreen(
            navController = rememberNavController(),
            widthSizeClass = WindowWidthSizeClass.Compact,
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            hostPaddingValues = PaddingValues(bottom = 56.dp) // Simulate bottom bar padding
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, name = "Medium MedicationVaultScreen", widthDp = 700)
@Composable
fun MedicationVaultScreenMediumPreview() {
    AppTheme {
        MedicationVaultScreen(
            navController = rememberNavController(),
            widthSizeClass = WindowWidthSizeClass.Medium,
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            hostPaddingValues = PaddingValues() // No bottom bar if rail is shown
        )
    }
}