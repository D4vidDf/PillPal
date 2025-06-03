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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.LocalSharedTransitionScope
import androidx.compose.animation.rememberSharedContentState
import androidx.compose.animation.sharedElement
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class) // ExperimentalPermissionsApi removed if not needed elsewhere
@Composable
fun HomeScreen(
    onAddMedicationClick: () -> Unit,
    onMedicationClick: (Int) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
    animatedVisibilityScope: AnimatedVisibilityScope?, // Make nullable
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
            // modifier = modifier, // Modifier from NavHost is now applied to the Column below
        ) {
            // Apply the modifier from NavHost (which includes padding from MedicationReminderApp's Scaffold)
            // AND the scaffoldInnerPadding from this HomeScreen's Scaffold to the Column.
            Column() {
                SearchBar(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (searchActive) 0.dp else 16.dp, vertical = 8.dp),
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = currentSearchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onSearch = {
                                searchActive = false
                                // Keyboard hidden automatically by onSearch
                            },
                            expanded = searchActive, // Pass the active state here
                            onExpandedChange = { isActive -> // This seems to be missing in SearchBarDefaults.InputField, handle in SearchBar
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
                                val localContext = LocalContext.current // Renamed to avoid conflict in lambdas
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
                                                // TODO: Implement a user-facing rationale (e.g., Snackbar)
                                                Log.i("HomeScreen", "RECORD_AUDIO permission rationale needed. User should be informed.")
                                                // Toast.makeText(localContext, "Microphone access is needed for voice search.", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        Log.e("HomeScreen", "Could not find Activity context to request RECORD_AUDIO permission.")
                                        // Toast.makeText(localContext, "Error: Could not perform voice search.", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = stringResource(id = R.string.microphone_icon_content_description)
                                    )
                                }
                            },
                        )
                    },
                    expanded = searchActive,
                    onExpandedChange = { isActive ->
                        searchActive = isActive
                        if (!isActive) {
                            viewModel.updateSearchQuery("") // Clear query when search becomes inactive
                        }
                    }
                ) {
                    // Content for search results - displayed when searchActive (expanded) is true
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { medication ->
                            val sharedTransitionScope = LocalSharedTransitionScope.current
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { medicationListClickHandler(medication.id) }
                                    .then(
                                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                            with(sharedTransitionScope) {
                                                Modifier.sharedElement(
                                                    state = rememberSharedContentState(key = "medication-background-${medication.id}"),
                                                    animatedVisibilityScope = animatedVisibilityScope!!
                                                )
                                            }
                                        } else Modifier
                                    )
                            ) {
                                Text(
                                    text = medication.name,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .then(
                                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                                with(sharedTransitionScope) {
                                                    Modifier.sharedElement(
                                                        state = rememberSharedContentState(key = "medication-name-${medication.id}"),
                                                        animatedVisibilityScope = animatedVisibilityScope!!
                                                    )
                                                }
                                            } else Modifier
                                        ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                        animatedVisibilityScope = animatedVisibilityScope, // Pass scope
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (searchActive) 0.dp else 16.dp, vertical = 8.dp),
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = currentSearchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onSearch = { searchActive = false },
                            expanded = searchActive, // Pass the active state here
                            onExpandedChange = { isActive -> // This seems to be missing in SearchBarDefaults.InputField, handle in SearchBar
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
                                val localContext = LocalContext.current // Renamed to avoid conflict in lambdas
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
                                                // TODO: Implement a user-facing rationale (e.g., Snackbar)
                                                Log.i("HomeScreen", "RECORD_AUDIO permission rationale needed. User should be informed.")
                                                // Toast.makeText(localContext, "Microphone access is needed for voice search.", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        Log.e("HomeScreen", "Could not find Activity context to request RECORD_AUDIO permission.")
                                        // Toast.makeText(localContext, "Error: Could not perform voice search.", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = stringResource(id = R.string.microphone_icon_content_description)
                                    )
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
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { medication ->
                            val sharedTransitionScope = LocalSharedTransitionScope.current
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { medicationListClickHandler(medication.id) }
                                    .then(
                                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                            with(sharedTransitionScope) {
                                                Modifier.sharedElement(
                                                    state = rememberSharedContentState(key = "medication-background-${medication.id}"),
                                                    animatedVisibilityScope = animatedVisibilityScope!!
                                                )
                                            }
                                        } else Modifier
                                    )
                            ) {
                                Text(
                                    text = medication.name,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .then(
                                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                                with(sharedTransitionScope) {
                                                    Modifier.sharedElement(
                                                        state = rememberSharedContentState(key = "medication-name-${medication.id}"),
                                                        animatedVisibilityScope = animatedVisibilityScope!!
                                                    )
                                                }
                                            } else Modifier
                                        ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                if (!searchActive) {
                    MedicationList(
                        medications = medications, // Display all medications
                        onItemClick = { medication -> medicationListClickHandler(medication.id) },
                        isLoading = isLoading,
                        onRefresh = { viewModel.refreshMedications() },
                        animatedVisibilityScope = animatedVisibilityScope, // Pass scope
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
                        },
                        animatedVisibilityScope = animatedVisibilityScope // Pass the scope received by HomeScreen
                    )
                }
            }
        }
    }
}
// Helper import, will be moved by IDE if not used explicitly but good for clarity
// import androidx.compose.foundation.clickable

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
            onAddMedicationClick = {},
            onMedicationClick = {},
            widthSizeClass = WindowWidthSizeClass.Compact,
            animatedVisibilityScope = null // Preview won't have a real scope
        )
    }
}

@Preview(showBackground = true, name = "Medium HomeScreen", widthDp = 700)
@Composable
fun HomeScreenMediumPreview() {
    AppTheme {
        HomeScreen(
            onAddMedicationClick = {},
            onMedicationClick = {},
            widthSizeClass = WindowWidthSizeClass.Medium,
            animatedVisibilityScope = null // Preview won't have a real scope
        )
    }
}

@Preview(showBackground = true, name = "Expanded HomeScreen", widthDp = 1024)
@Composable
fun HomeScreenExpandedPreview() {
    AppTheme {
        HomeScreen(
            onAddMedicationClick = {},
            onMedicationClick = {},
            widthSizeClass = WindowWidthSizeClass.Expanded,
            animatedVisibilityScope = null // Preview won't have a real scope
        )
    }
}