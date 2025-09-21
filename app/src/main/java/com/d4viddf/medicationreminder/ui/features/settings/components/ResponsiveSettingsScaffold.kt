package com.d4viddf.medicationreminder.ui.features.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.settings.developer.DeveloperSettingsScreen
import com.d4viddf.medicationreminder.ui.features.settings.general.GeneralSettingsScreen
import com.d4viddf.medicationreminder.ui.features.settings.SettingsListScreen
import com.d4viddf.medicationreminder.ui.features.settings.sound.SoundSettingsScreen
import com.d4viddf.medicationreminder.ui.features.settings.SettingsViewModel
import com.d4viddf.medicationreminder.ui.navigation.Screen

// Define routes for sub-settings screens
object SettingsDestinations {
    const val LIST = "settings_list"
    const val GENERAL = "settings_general"
    const val SOUND = "settings_sound"
    const val DEVELOPER = "settings_developer"
    const val HEALTH_CONNECT = "settings_health_connect"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveSettingsScaffold(
    modifier: Modifier = Modifier,
    widthSizeClass: WindowWidthSizeClass,
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    updateTopBarActions: (titleResId: Int, backAction: () -> Unit) -> Unit, // New parameter
    contentPadding: PaddingValues = PaddingValues(0.dp) // New parameter
) {
    val isTwoPane = widthSizeClass > WindowWidthSizeClass.Compact
    var selectedCategoryRoute by rememberSaveable { mutableStateOf<String?>(SettingsDestinations.GENERAL) } // Default to General for two-pane

    // For single-pane layout, we use a local NavController
    val localSettingsNavController = rememberNavController()

    if (isTwoPane) {
        // Two-pane layout (tablet)
        Column(modifier = modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_screen_title)) },
                navigationIcon = {
                    if (selectedCategoryRoute != null && selectedCategoryRoute != SettingsDestinations.GENERAL) {
                        IconButton(onClick = {
                            selectedCategoryRoute = SettingsDestinations.GENERAL
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_arrow_back_ios_24),
                                contentDescription = stringResource(id = R.string.back)
                            )
                        }
                    }
                    // If condition is false, lambda is empty, icon slot is empty.
                }
            )
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(300.dp).fillMaxHeight()) {
                    SettingsListScreen(
                        onNavigateToGeneral = {
                            selectedCategoryRoute = SettingsDestinations.GENERAL
                        },
                        onNavigateToSound = { selectedCategoryRoute = SettingsDestinations.SOUND },
                        onNavigateToDeveloper = {
                            selectedCategoryRoute = SettingsDestinations.DEVELOPER
                        },
                        onNavigateToConnectedDevices = { navController.navigate(Screen.ConnectedDevices.route) },
                        onNavigateToHealthConnect = {
                            selectedCategoryRoute = SettingsDestinations.HEALTH_CONNECT
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.weight(1f)) {
                    // selectedCategoryRoute is initialized to GENERAL, so this will always have a value.
                    when (selectedCategoryRoute) {
                        SettingsDestinations.GENERAL -> GeneralSettingsScreen(
                            onNavigateBack = {},
                            viewModel = settingsViewModel
                        ) // No showTopAppBar, onNavigateBack is {}
                        SettingsDestinations.SOUND -> SoundSettingsScreen(
                            onNavigateBack = {},
                            viewModel = settingsViewModel
                        )

                        SettingsDestinations.DEVELOPER -> DeveloperSettingsScreen(
                            onNavigateBack = {},
                            viewModel = settingsViewModel
                        )

                        SettingsDestinations.HEALTH_CONNECT -> com.d4viddf.medicationreminder.ui.features.settings.HealthConnectSettingsScreen(
                            navController = navController
                        )

                        else -> GeneralSettingsScreen(
                            onNavigateBack = {},
                            viewModel = settingsViewModel
                        ) // Fallback, though selectedCategoryRoute is non-null
                    }
                }
            }
        }
    } else {
        // Single-pane layout (phone)
        // Scaffold removed, NavHost is now the root for this branch
        val navBackStackEntry by localSettingsNavController.currentBackStackEntryAsState()
        LaunchedEffect(navBackStackEntry) {
            val currentRoute = navBackStackEntry?.destination?.route
            when (currentRoute) {
                SettingsDestinations.LIST -> {
                    updateTopBarActions(R.string.settings_screen_title) { navController.popBackStack() }
                }

                SettingsDestinations.GENERAL -> {
                    updateTopBarActions(R.string.settings_category_general) { localSettingsNavController.popBackStack() }
                }

                SettingsDestinations.SOUND -> {
                    updateTopBarActions(R.string.settings_category_sound) { localSettingsNavController.popBackStack() }
                }

                SettingsDestinations.DEVELOPER -> {
                    updateTopBarActions(R.string.settings_category_developer) { localSettingsNavController.popBackStack() }
                }

                SettingsDestinations.HEALTH_CONNECT -> {
                    updateTopBarActions(R.string.health_connect_settings) { localSettingsNavController.popBackStack() }
                }
            }
        }

        NavHost(
            navController = localSettingsNavController,
            startDestination = SettingsDestinations.LIST,
            modifier = modifier.fillMaxSize().padding(contentPadding) // Apply contentPadding
        ) {
            composable(SettingsDestinations.LIST) {
                SettingsListScreen(
                    onNavigateToGeneral = { localSettingsNavController.navigate(SettingsDestinations.GENERAL) },
                    onNavigateToSound = { localSettingsNavController.navigate(SettingsDestinations.SOUND) },
                    onNavigateToDeveloper = {
                        localSettingsNavController.navigate(
                            SettingsDestinations.DEVELOPER
                        )
                    },
                    onNavigateToConnectedDevices = { navController.navigate(Screen.ConnectedDevices.route) },
                    onNavigateToHealthConnect = {
                        localSettingsNavController.navigate(
                            SettingsDestinations.HEALTH_CONNECT
                        )
                    }
                )
            }
            composable(SettingsDestinations.GENERAL) {
                GeneralSettingsScreen(
                    onNavigateBack = { localSettingsNavController.popBackStack() }, // onNavigateBack is for internal logic if screen has one, not for TopAppBar
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
            composable(SettingsDestinations.HEALTH_CONNECT) {
                com.d4viddf.medicationreminder.ui.features.settings.HealthConnectSettingsScreen(
                    navController = navController
                )
            }
        }
    }
}

// Previewing this responsive scaffold is complex due to NavController and WindowSizeClass.
// Basic previews for each mode might be added later if needed.
