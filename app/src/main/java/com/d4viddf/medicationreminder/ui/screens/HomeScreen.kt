@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
package com.d4viddf.medicationreminder.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope // Already present
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.components.MedicationList
import com.d4viddf.medicationreminder.utils.PermissionUtils
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class) // Removed ExperimentalSharedTransitionApi from here
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier, // This modifier comes from NavHost, potentially with padding
    onMedicationClick: (Int) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
    sharedTransitionScope: SharedTransitionScope?, // Add this
    animatedVisibilityScope: AnimatedVisibilityScope?, // Make nullable
    viewModel: MedicationViewModel = hiltViewModel(),

) {
    val medications by viewModel.medications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentSearchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    // var selectedMedicationId by rememberSaveable { mutableStateOf<Int?>(null) } // Will be managed by scaffoldNavigator

    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Int?>() // Use Int? for nullable medication ID
    val mainMedicationListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope() // For general coroutines, also for scaffold nav

    // Local state for SearchBar active state
    var searchActive by rememberSaveable { mutableStateOf(false) }

    // val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO) // Removed
    // val activity = LocalContext.current as Activity // Will be replaced by findActivity()

    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText: ArrayList<String>? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenText.isNullOrEmpty()) {
                viewModel.updateSearchQuery(spokenText[0])
                searchActive = true // Optionally activate search bar if you want to see results immediately
            }
        }
    }

    val medicationListClickHandler: (Int) -> Unit = { medicationId ->
        // When a medication is clicked, whether from main list or search results,
        searchActive = false
        viewModel.updateSearchQuery("")

        if (widthSizeClass == WindowWidthSizeClass.Compact) {
            onMedicationClick(medicationId) // Full screen navigation via NavController
        } else {
            // Show in detail pane using scaffoldNavigator
            coroutineScope.launch {
                scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, medicationId)
            }
        }
    }

    NavigableListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(),
        navigator = scaffoldNavigator,
        listPane = { // Changed from primaryPane
            // `this` is ThreePaneScaffoldPaneScope
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                SearchBar(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (searchActive && widthSizeClass == WindowWidthSizeClass.Compact) 0.dp else 16.dp, vertical = 8.dp),
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = currentSearchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onSearch = {
                                searchActive = false
                            },
                            expanded = searchActive,
                            onExpandedChange = { isActive ->
                                searchActive = isActive
                                if (!isActive) {
                                    viewModel.updateSearchQuery("")
                                }
                            },
                            placeholder = { Text(stringResource(id = R.string.search_medications_placeholder)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(id = R.string.search_icon_content_description)
                                )
                            },
                            trailingIcon = {
                                if (currentSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = stringResource(id = R.string.clear_search_query_button_description)
                                        )
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
                                                onRationaleNeeded = {
                                                    Log.i("HomeScreen", "RECORD_AUDIO permission rationale needed.")
                                                }
                                            )
                                        } else {
                                            Log.e("HomeScreen", "Could not find Activity context.")
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Mic,
                                            contentDescription = stringResource(id = R.string.microphone_icon_content_description)
                                        )
                                    }
                                }
                            },
                        )
                    },
                    expanded = searchActive,
                    onExpandedChange = { isActive ->
                        searchActive = isActive
                        if (!isActive) {
                            viewModel.updateSearchQuery("")
                        }
                    }
                    // SearchBar content lambda is part of its definition
                    ) { // Search results content
                        val searchResultsListState = rememberLazyListState()
                        LazyColumn(
                        state = searchResultsListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        itemsIndexed(searchResults, key = { _, med -> med.id }) { index, medication ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            searchResultsListState.animateScrollToItem(index)
                                            medicationListClickHandler(medication.id)
                                        }
                                    }
                                    .then(
                                        if (sharedTransitionScope != null && animatedVisibilityScope != null && widthSizeClass == WindowWidthSizeClass.Compact) {
                                            with(sharedTransitionScope) {
                                                Modifier.sharedElement(
                                                    rememberSharedContentState(key = "medication-background-${medication.id}"),
                                                    animatedVisibilityScope
                                                )
                                            }
                                        } else Modifier
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = medication.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.then(
                                            if (sharedTransitionScope != null && animatedVisibilityScope != null && widthSizeClass == WindowWidthSizeClass.Compact) {
                                                with(sharedTransitionScope) {
                                                    Modifier.sharedElement(
                                                        rememberSharedContentState(key = "medication-name-${medication.id}"),
                                                        animatedVisibilityScope
                                                    )
                                                }
                                            } else Modifier
                                        )
                                    )
                                    if (!medication.dosage.isNullOrBlank()) {
                                        Text(
                                            text = medication.dosage,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } // End of SearchBar content lambda

                if (!searchActive) {
                    val topAppBarHeight = 84.dp // Approx height for SearchBar
                    val listToShow = if (currentSearchQuery.isBlank()) medications else searchResults
                    MedicationList(
                        medications = listToShow,
                        onItemClick = { medication, index ->
                            // medicationListClickHandler already has the logic for Compact vs Large screen.
                            // It also handles the coroutine for scaffoldNavigator.
                            // For compact, we might want to scroll before full navigation.
                            if (widthSizeClass == WindowWidthSizeClass.Compact && sharedTransitionScope != null && animatedVisibilityScope != null) {
                                coroutineScope.launch {
                                    mainMedicationListState.animateScrollToItem(index)
                                    medicationListClickHandler(medication.id) // Will call onMedicationClick
                                }
                            } else {
                                // Handles both scaffold navigation (with its own coroutine) and compact (direct call)
                                medicationListClickHandler(medication.id)
                            }
                        },
                        isLoading = isLoading,
                        onRefresh = { viewModel.refreshMedications() },
                        enableCardTransitions = (widthSizeClass == WindowWidthSizeClass.Compact),
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier = Modifier.fillMaxSize(),
                        bottomContentPadding = if (widthSizeClass == WindowWidthSizeClass.Compact) topAppBarHeight else 0.dp,
                        listState = mainMedicationListState
                    )
                }
            }
        },
        detailPane = { // Changed from secondaryPane
            // `this` is ThreePaneScaffoldPaneScope
            val selectedMedicationIdForDetail = scaffoldNavigator.currentDestination?.contentKey
            if (selectedMedicationIdForDetail != null) {
                MedicationDetailsScreen(
                    medicationId = selectedMedicationIdForDetail,
                    onNavigateBack = {
                        coroutineScope.launch {
                            scaffoldNavigator.navigateBack()
                        }
                    },
                    sharedTransitionScope = null, // Correct for detail pane
                    // animatedVisibilityScope for MedicationDetailScreen is tricky here.
                    // 'this' is ThreePaneScaffoldPaneScope.
                    // If MedicationDetailScreen expects an AnimatedVisibilityScope for its own internal animations,
                    // it would need to come from an AnimatedVisibility composable within this detailPane.
                    // For shared elements (which are disabled: enableSharedTransition = false), it would need
                    // the one from AppNavigation. Since shared elements are off, this is less critical.
                    // Passing null if it's not used or if it expects the NavHost's scope which isn't appropriate here.
                    animatedVisibilityScope = null, // Or a specific one if MedicationDetailScreen needs it for internal anims
                    isHostedInPane = true // Added parameter
                )
            } else {
                // Placeholder when no medication is selected in detail pane (medium/expanded screens)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.select_medication_placeholder))
                }
            }
        }
        // We can also define a tertiary pane if needed, but not for this use case.
    )
}
// Top-level extension function
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(showBackground = true, name = "Compact HomeScreen")
@Composable
fun HomeScreenCompactPreview() {
    AppTheme {
        HomeScreen(
            onMedicationClick = {},
            widthSizeClass = WindowWidthSizeClass.Compact,
            sharedTransitionScope = null, // Pass null for preview
            animatedVisibilityScope = null // Preview won't have a real scope
        )
    }
}

@Preview(showBackground = true, name = "Medium HomeScreen", widthDp = 700)
@Composable
fun HomeScreenMediumPreview() {
    AppTheme {
        HomeScreen(
            onMedicationClick = {},
            widthSizeClass = WindowWidthSizeClass.Medium,
            sharedTransitionScope = null, // Pass null for preview
            animatedVisibilityScope = null // Preview won't have a real scope
        )
    }
}

@Preview(showBackground = true, name = "Expanded HomeScreen", widthDp = 1024)
@Composable
fun HomeScreenExpandedPreview() {
    AppTheme {
        HomeScreen(
            onMedicationClick = {},
            widthSizeClass = WindowWidthSizeClass.Expanded,
            sharedTransitionScope = null, // Pass null for preview
            animatedVisibilityScope = null // Preview won't have a real scope
        )
    }
}