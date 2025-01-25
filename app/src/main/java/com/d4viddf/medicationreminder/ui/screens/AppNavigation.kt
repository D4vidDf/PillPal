package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

// Define the routes for navigation
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddMedication : Screen("addMedication")
    object MedicationDetails : Screen("medicationDetails/{id}") {
        fun createRoute(id: Int) = "medicationDetails/$id"
    }
}


@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddMedicationClick = { navController.navigate(Screen.AddMedication.route) },
                onMedicationClick = { medicationId -> // Correct lambda for passing medication ID
                    navController.navigate(Screen.MedicationDetails.createRoute(medicationId))
                }
            )
        }
        composable(Screen.AddMedication.route) {
            AddMedicationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MedicationDetails.route) { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getString("id")?.toIntOrNull()
            if (medicationId != null) {
                MedicationDetailsScreen(
                    medicationId = medicationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
