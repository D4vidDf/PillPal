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

// Define the routes for navigation
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddMedication : Screen("addMedication")
    object AddMedicationChoice : Screen("addMedicationChoice") // New line
    object MedicationDetails : Screen("medicationDetails/{id}?enableSharedTransition={enableSharedTransition}") {
        fun createRoute(id: Int, enableSharedTransition: Boolean = true) = "medicationDetails/$id?enableSharedTransition=$enableSharedTransition"
    }
    object Settings : Screen("settings")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    data object Onboarding : Screen("onboarding_screen") // Added Onboarding screen
}

// Removed @OptIn(ExperimentalSharedTransitionApi::class) from here
@OptIn(ExperimentalSharedTransitionApi::class)
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
                val medicationId = backStackEntry.arguments?.getString("id")?.toIntOrNull()
                val enableSharedTransition = backStackEntry.arguments?.getBoolean("enableSharedTransition") ?: true
                if (medicationId != null) {
                    MedicationDetailsScreen(
                        medicationId = medicationId,
                        onNavigateBack = { navController.popBackStack() },
                        sharedTransitionScope = currentSharedTransitionScope, // Pass captured scope
                        animatedVisibilityScope = this, // Pass scope
                        isHostedInPane = false
                        // Pass the new argument
                    )
                }
            }
            composable(Screen.Settings.route) {
                if (widthSizeClass == WindowWidthSizeClass.Compact) {
                    // Phone layout: AppNavigation provides the Scaffold and TopAppBar for the settings module.
                    Scaffold(
                        topBar = {
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
                    ) { innerPadding ->
                        ResponsiveSettingsScaffold(
                            widthSizeClass = widthSizeClass,
                            navController = navController,
                            // Apply padding from this Scaffold to the content area of ResponsiveSettingsScaffold
                            // ResponsiveSettingsScaffold's internal NavHost will use its own Scaffold's innerPadding
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                } else {
                    // Tablet layout: ResponsiveSettingsScaffold provides its own overall TopAppBar.
                    ResponsiveSettingsScaffold(
                        widthSizeClass = widthSizeClass,
                        navController = navController
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
        }
    }
}
