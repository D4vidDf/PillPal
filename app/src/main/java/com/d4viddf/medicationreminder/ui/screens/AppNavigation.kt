package com.d4viddf.medicationreminder.ui.screens

// Removed ExperimentalSharedTransitionApi from here as it's now file-level (or it might be kept if other specific experimental APIs are used later)
// For this task, specifically moving the SharedTransition one.
// Keeping it is fine if it's the only experimental API. If there were others, this specific one would be part of the file-level.
// Let's assume it's fine to just have the file-level one for this API.
// import androidx.compose.animation.ExperimentalSharedTransitionApi // Added - This line can be removed if no other composable in this file needs it individually
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout // Added
import androidx.compose.animation.SharedTransitionScope // Added import
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType // Added import
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument // Added import

// Define the routes for navigation
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddMedication : Screen("addMedication")
    object MedicationDetails : Screen("medicationDetails/{id}?enableSharedTransition={enableSharedTransition}") {
        fun createRoute(id: Int, enableSharedTransition: Boolean = true) = "medicationDetails/$id?enableSharedTransition=$enableSharedTransition"
    }
    object Settings : Screen("settings")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")

}

// Removed @OptIn(ExperimentalSharedTransitionApi::class) from here
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    widthSizeClass: WindowWidthSizeClass,
    paddingValues: PaddingValues = PaddingValues(), // Added parameter
    isMainScaffold: Boolean, // Added parameter
    modifier: Modifier = Modifier // Add this line
) {
    SharedTransitionLayout { // `this` is SharedTransitionScope
        val currentSharedTransitionScope = this // Capture SharedTransitionScope

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier.then(if (isMainScaffold) Modifier.fillMaxSize() else Modifier) // Apply incoming modifier and then conditional padding
        ) {
            composable(Screen.Home.route) {
                // `this` is an AnimatedVisibilityScope
                HomeScreen(
                    onAddMedicationClick = { navController.navigate(Screen.AddMedication.route) }, // Kept
                    onMedicationClick = { medicationId -> // Kept
                        navController.navigate(Screen.MedicationDetails.createRoute(medicationId))
                    },
                    // Removed onNavigateToSettings, onNavigateToCalendar, onNavigateToProfile
                    widthSizeClass = widthSizeClass, // Kept
                    sharedTransitionScope = currentSharedTransitionScope, // Pass captured scope
                    animatedVisibilityScope = this // Pass scope
                )
            }
            composable(Screen.AddMedication.route) {
                // `this` is an AnimatedVisibilityScope
                AddMedicationScreen(
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
                        enableSharedTransition = enableSharedTransition // Pass the new argument
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
