package com.d4viddf.medicationreminder.ui.screens

// Removed ExperimentalSharedTransitionApi from here as it's now file-level (or it might be kept if other specific experimental APIs are used later)
// For this task, specifically moving the SharedTransition one.
// Keeping it is fine if it's the only experimental API. If there were others, this specific one would be part of the file-level.
// Let's assume it's fine to just have the file-level one for this API.
// import androidx.compose.animation.ExperimentalSharedTransitionApi // Added - This line can be removed if no other composable in this file needs it individually
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout // Added
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController // Corrected order
import androidx.navigation.NavType // Added import
import androidx.navigation.compose.NavHost // Corrected order
import androidx.navigation.compose.composable // Corrected order
import androidx.navigation.navArgument // Added import
import com.d4viddf.medicationreminder.ui.screens.OnboardingScreen // Added import for OnboardingScreen
import com.d4viddf.medicationreminder.ui.screens.addmedication.AddMedicationScreen // Added import for AddMedicationScreen

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
) {
    SharedTransitionLayout { // `this` is SharedTransitionScope
        val currentSharedTransitionScope = this // Capture SharedTransitionScope

        NavHost(
            navController = navController,
            startDestination = Screen.Onboarding.route, // Changed start destination to Onboarding
            modifier = modifier.then(if (isMainScaffold) Modifier.fillMaxSize() else Modifier) // Apply incoming modifier and then conditional padding
        ) {
            composable(Screen.Onboarding.route) { // Added route for OnboardingScreen
                OnboardingScreen(navController = navController) // Pass the navController
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
                    onUseCamera = { /* Functionality to be added later */ }
                )
            }
            composable(Screen.AddMedication.route) {
                // `this` is an AnimatedVisibilityScope
                com.d4viddf.medicationreminder.ui.screens.addmedication.AddMedicationScreen(
                    onNavigateBack = { navController.popBackStack() }
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
                // `this` is an AnimatedVisibilityScope
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                    // No animatedVisibilityScope passed
                )
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
