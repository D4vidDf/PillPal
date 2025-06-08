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
    navController: NavController, // Main app's NavController for back navigation from the list screen
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val isTwoPane = widthSizeClass > WindowWidthSizeClass.Compact
    var selectedCategoryRoute by rememberSaveable { mutableStateOf<String?>(null) }

    // For single-pane layout, we use a local NavController
    val localSettingsNavController = rememberNavController()

    if (isTwoPane) {
        // Two-pane layout (tablet)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(300.dp).fillMaxHeight()) { // Left pane
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
                    // Optionally show a placeholder or the first category by default
                    // For now, let's show General settings by default in two-pane if nothing selected
                    GeneralSettingsScreen(onNavigateBack = {}, viewModel = settingsViewModel)
                } else {
                    when (selectedCategoryRoute) {
                        SettingsDestinations.GENERAL -> GeneralSettingsScreen(onNavigateBack = {}, viewModel = settingsViewModel) // No back nav needed from detail in 2-pane
                        SettingsDestinations.SOUND -> SoundSettingsScreen(onNavigateBack = {}, viewModel = settingsViewModel)
                        SettingsDestinations.DEVELOPER -> DeveloperSettingsScreen(onNavigateBack = {}, viewModel = settingsViewModel)
                    }
                }
            }
        }
    } else {
        // Single-pane layout (phone)
        Scaffold(
            topBar = {
                // TopAppBar for the settings list screen when on phone
                // Detail screens have their own TopAppBars with back navigation
                val currentRoute = localSettingsNavController.currentBackStackEntry?.destination?.route
                if (currentRoute == SettingsDestinations.LIST) {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.settings_screen_title)) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) { // Uses main app NavController
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(id = R.string.back)
                                )
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
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
                        viewModel = settingsViewModel
                    )
                }
                composable(SettingsDestinations.SOUND) {
                    SoundSettingsScreen(
                        onNavigateBack = { localSettingsNavController.popBackStack() },
                        viewModel = settingsViewModel
                    )
                }
                composable(SettingsDestinations.DEVELOPER) {
                    DeveloperSettingsScreen(
                        onNavigateBack = { localSettingsNavController.popBackStack() },
                        viewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

// Previewing this responsive scaffold is complex due to NavController and WindowSizeClass.
// Basic previews for each mode might be added later if needed.
