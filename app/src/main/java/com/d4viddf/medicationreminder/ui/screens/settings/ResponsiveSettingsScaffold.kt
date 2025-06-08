package com.d4viddf.medicationreminder.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.viewmodel.SettingsViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.padding // Required for Modifier.padding
import androidx.compose.foundation.layout.Column // Ensure Column is imported

// Define routes for sub-settings screens
object SettingsDestinations {
    const val LIST = "settings_list"
    const val GENERAL = "settings_general"
    const val SOUND = "settings_sound"
    const val DEVELOPER = "settings_developer"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveSettingsScaffold(
    widthSizeClass: WindowWidthSizeClass,
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier // New parameter
) {
    val isTwoPane = widthSizeClass > WindowWidthSizeClass.Compact
    var selectedCategoryRoute by rememberSaveable { mutableStateOf<String?>(null) }

    // For single-pane layout, we use a local NavController
    val localSettingsNavController = rememberNavController()

    if (isTwoPane) {
        // Two-pane layout (tablet)
        Column(modifier = modifier.fillMaxSize()) { // Applied modifier
            TopAppBar(title = { Text(stringResource(id = R.string.settings_screen_title)) })
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(300.dp).fillMaxHeight()) {
                    SettingsListScreen(
                        onNavigateToGeneral = { selectedCategoryRoute = SettingsDestinations.GENERAL },
                        onNavigateToSound = { selectedCategoryRoute = SettingsDestinations.SOUND },
                        onNavigateToDeveloper = { selectedCategoryRoute = SettingsDestinations.DEVELOPER },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.weight(1f)) { // Right pane
                    if (selectedCategoryRoute == null) {
                        GeneralSettingsScreen(onNavigateBack = {}, viewModel = settingsViewModel, showTopAppBar = false)
                    } else {
                        when (selectedCategoryRoute) {
                            SettingsDestinations.GENERAL -> GeneralSettingsScreen(onNavigateBack = {}, viewModel = settingsViewModel, showTopAppBar = false)
                            SettingsDestinations.SOUND -> SoundSettingsScreen(onNavigateBack = {}, viewModel = settingsViewModel, showTopAppBar = false)
                            SettingsDestinations.DEVELOPER -> DeveloperSettingsScreen(onNavigateBack = {}, viewModel = settingsViewModel, showTopAppBar = false)
                        }
                    }
                }
            }
        }
    } else {
        // Single-pane layout (phone)
        Scaffold(modifier = modifier) { innerPadding -> // Applied modifier and removed topBar
            NavHost(
                navController = localSettingsNavController,
                startDestination = SettingsDestinations.LIST,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(SettingsDestinations.LIST) {
                    SettingsListScreen(
                        onNavigateToGeneral = { localSettingsNavController.navigate(SettingsDestinations.GENERAL) },
                        onNavigateToSound = { localSettingsNavController.navigate(SettingsDestinations.SOUND) },
                        onNavigateToDeveloper = { localSettingsNavController.navigate(SettingsDestinations.DEVELOPER) }
                    )
                }
                composable(SettingsDestinations.GENERAL) {
                    GeneralSettingsScreen(
                        onNavigateBack = { localSettingsNavController.popBackStack() },
                        viewModel = settingsViewModel,
                        showTopAppBar = true // Pass true here
                    )
                }
                composable(SettingsDestinations.SOUND) {
                    SoundSettingsScreen(
                        onNavigateBack = { localSettingsNavController.popBackStack() },
                        viewModel = settingsViewModel,
                        showTopAppBar = true // Pass true here
                    )
                }
                composable(SettingsDestinations.DEVELOPER) {
                    DeveloperSettingsScreen(
                        onNavigateBack = { localSettingsNavController.popBackStack() },
                        viewModel = settingsViewModel,
                        showTopAppBar = true // Pass true here
                    )
                }
            }
        }
    }
}

// Previewing this responsive scaffold is complex due to NavController and WindowSizeClass.
// Basic previews for each mode might be added later if needed.
