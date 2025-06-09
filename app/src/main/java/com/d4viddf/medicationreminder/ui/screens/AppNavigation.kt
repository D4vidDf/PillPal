package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout // Added
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController // Corrected order
import androidx.navigation.NavType
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.d4viddf.medicationreminder.ui.screens.settings.ResponsiveSettingsScaffold
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.R
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue // For state
import androidx.compose.runtime.mutableStateOf // For state
import androidx.compose.runtime.remember // For state
import androidx.compose.runtime.setValue // For state

// Define the routes for navigation

const val MEDICATION_ID_ARG = "medicationId" // Common argument name
const val SHOW_TODAY_ARG = "showToday" // Argument for AllSchedulesScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddMedication : Screen("addMedication")
    object AddMedicationChoice : Screen("addMedicationChoice")
    object MedicationDetails : Screen("medicationDetails/{$MEDICATION_ID_ARG}?enableSharedTransition={enableSharedTransition}") {
        fun createRoute(id: Int, enableSharedTransition: Boolean = true) = "medicationDetails/$id?enableSharedTransition=$enableSharedTransition"
    }
    object Settings : Screen("settings")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    data object Onboarding : Screen("onboarding_screen")

    object AllSchedules : Screen("all_schedules_screen/{$MEDICATION_ID_ARG}?$SHOW_TODAY_ARG={$SHOW_TODAY_ARG}") {
        fun createRoute(medicationId: Int, showToday: Boolean = false) = "all_schedules_screen/$medicationId?$SHOW_TODAY_ARG=$showToday"
    }
    object MedicationHistory : Screen("medication_history_screen/{$MEDICATION_ID_ARG}") {
        fun createRoute(medicationId: Int) = "medication_history_screen/$medicationId"
    }
    object MedicationGraph : Screen("medication_graph_screen/{$MEDICATION_ID_ARG}") {
        fun createRoute(medicationId: Int) = "medication_graph_screen/$medicationId"
    }
    object MedicationInfo : Screen("medication_info_screen/{$MEDICATION_ID_ARG}") {
        fun createRoute(medicationId: Int) = "medication_info_screen/$medicationId"
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier, // Add this line
    navController: NavHostController,
    widthSizeClass: WindowWidthSizeClass,
    isMainScaffold: Boolean, // Added parameter
    userPreferencesRepository: UserPreferencesRepository, // Still needed for OnboardingScreen
    startDestinationRoute: String // Add this new parameter
) {
    SharedTransitionLayout { // `this` is SharedTransitionScope
        val currentSharedTransitionScope = this // Capture SharedTransitionScope

        // val onboardingCompleted by userPreferencesRepository.onboardingCompletedFlow.collectAsState(initial = false) // REMOVE
        // val startDestination = if (onboardingCompleted) Screen.Home.route else Screen.Onboarding.route // REMOVE

        NavHost(
            navController = navController,
            startDestination = startDestinationRoute, // USE THE PARAMETER HERE
            modifier = modifier.then(if (isMainScaffold) Modifier.fillMaxSize() else Modifier) // Apply incoming modifier and then conditional padding
        ) {
            composable(Screen.Onboarding.route) { // Added route for OnboardingScreen
                OnboardingScreen(
                    navController = navController,
                    userPreferencesRepository = userPreferencesRepository // Pass it here
                )
            }
            composable(Screen.Home.route) {
                // `this` is an AnimatedVisibilityScope
                HomeScreen(
                    // Kept
                    onMedicationClick = { medicationId -> // Kept
                        navController.navigate(Screen.MedicationDetails.createRoute(medicationId, enableSharedTransition = widthSizeClass == WindowWidthSizeClass.Compact))
                    },
                    // Removed onNavigateToSettings, onNavigateToCalendar, onNavigateToProfile
                    widthSizeClass = widthSizeClass, // Kept
                    sharedTransitionScope = currentSharedTransitionScope, // Pass captured scope
                    animatedVisibilityScope = this // Pass scope
                )
            }
            composable(Screen.AddMedicationChoice.route) { // New entry
                AddMedicationChoiceScreen(
                    onSearchMedication = { navController.navigate(Screen.AddMedication.route) },
                    onUseCamera = { /* Functionality to be added later */ },
                    onClose = { navController.popBackStack() } // Add this line
                )
            }
            composable(Screen.AddMedication.route) {
                // `this` is an AnimatedVisibilityScope
                AddMedicationScreen(
                    // onNavigateBack = { navController.popBackStack() }, // Remove this
                    navController = navController, // Add this
                    widthSizeClass = widthSizeClass // Pass the widthSizeClass
                    // No animatedVisibilityScope passed
                )
            }
            composable(
                Screen.MedicationDetails.route,
                arguments = listOf(navArgument("enableSharedTransition") { type = NavType.BoolType; defaultValue = true })
            ) { backStackEntry ->
                // `this` is an AnimatedVisibilityScope
                val medicationId = backStackEntry.arguments?.getString(MEDICATION_ID_ARG)?.toIntOrNull()
                val enableSharedTransition = backStackEntry.arguments?.getBoolean("enableSharedTransition") ?: true
                if (medicationId != null) {
                    MedicationDetailsScreen(
                        medicationId = medicationId,
                        onNavigateBack = { navController.popBackStack() },
                        sharedTransitionScope = currentSharedTransitionScope, // Pass captured scope
                        animatedVisibilityScope = this, // Pass scope
                        isHostedInPane = false,
                        // Navigation callbacks for new screens from MedicationDetailScreen
                        onNavigateToAllSchedules = { medId -> navController.navigate(Screen.AllSchedules.createRoute(medicationId = medId, showToday = true)) }, // Pass showToday = true from details screen

                        onNavigateToMedicationHistory = { medId -> navController.navigate(Screen.MedicationHistory.createRoute(medId)) },
                        onNavigateToMedicationGraph = { medId -> navController.navigate(Screen.MedicationGraph.createRoute(medId)) },
                        onNavigateToMedicationInfo = { medId -> navController.navigate(Screen.MedicationInfo.createRoute(medId)) }
                    )
                }
            }
            composable(Screen.Settings.route) {
                var currentSettingsTitleResId by remember { mutableStateOf(R.string.settings_screen_title) }
                var currentSettingsBackAction by remember { mutableStateOf<() -> Unit>({ navController.popBackStack() }) } // Explicit lambda wrapping

                if (widthSizeClass == WindowWidthSizeClass.Compact) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(stringResource(id = currentSettingsTitleResId)) },
                                navigationIcon = {
                                    IconButton(onClick = { currentSettingsBackAction.invoke() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(id = R.string.back) // Generic back description
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        ResponsiveSettingsScaffold(
                            widthSizeClass = widthSizeClass,
                            navController = navController,
                            updateTopBarActions = { titleResId, backAction ->
                                currentSettingsTitleResId = titleResId
                                currentSettingsBackAction = backAction
                            },
                            contentPadding = innerPadding, // Pass Scaffold's innerPadding
                            modifier = Modifier.fillMaxSize() // Responsive scaffold should fill the content area
                        )
                    }
                } else {
                    // Tablet layout: ResponsiveSettingsScaffold manages its own TopAppBar.
                    // updateTopBarActions and contentPadding are not strictly needed here as it manages its own Scaffold/TopAppBar.
                    // However, to keep the call signature consistent or if some root modifier is needed:
                    ResponsiveSettingsScaffold(
                        widthSizeClass = widthSizeClass,
                        navController = navController,
                        updateTopBarActions = { _, _ -> /* No-op for tablet as it handles its own top bar */ },
                        // contentPadding can be default or specific if ResponsiveSettingsScaffold needs it
                        modifier = Modifier.fillMaxSize() // Ensure it fills the designated area
                    )
                }
            }
            composable(Screen.Calendar.route) {
                // `this` is an AnimatedVisibilityScope
                CalendarScreen(
                    onNavigateBack = { navController.popBackStack() }, // Removed
                    onNavigateToMedicationDetail = { medicationId ->
                        navController.navigate(Screen.MedicationDetails.createRoute(medicationId, enableSharedTransition = false))
                    }
                    // No sharedTransitionScope or animatedVisibilityScope passed
                )
            }
            composable(Screen.Profile.route) {
                // `this` is an AnimatedVisibilityScope
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                    // No animatedVisibilityScope passed
                )
            }

            // Routes for the new screens
            composable(
                Screen.AllSchedules.route,
                arguments = listOf(
                    navArgument(MEDICATION_ID_ARG) { type = NavType.IntType },
                    navArgument(SHOW_TODAY_ARG) { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getInt(MEDICATION_ID_ARG) ?: -1
                val showToday = backStackEntry.arguments?.getBoolean(SHOW_TODAY_ARG) ?: false
                AllSchedulesScreen(
                    medicationId = medicationId,
                    showToday = showToday, // Pass showToday argument

                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.MedicationHistory.route,
                arguments = listOf(navArgument(MEDICATION_ID_ARG) { type = NavType.IntType })
            ) { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getInt(MEDICATION_ID_ARG) ?: -1
                MedicationHistoryScreen(
                    medicationId = medicationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.MedicationGraph.route,
                arguments = listOf(navArgument(MEDICATION_ID_ARG) { type = NavType.IntType })
            ) { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getInt(MEDICATION_ID_ARG) ?: -1
                MedicationGraphScreen(
                    medicationId = medicationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.MedicationInfo.route,
                arguments = listOf(navArgument(MEDICATION_ID_ARG) { type = NavType.IntType })
            ) { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getInt(MEDICATION_ID_ARG) ?: -1
                MedicationInfoScreen(
                    medicationId = medicationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
