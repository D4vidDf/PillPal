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
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onAddMedicationClick = { navController.navigate("addEditMedication") },
                onMedicationClick = { medicationId -> navController.navigate("medicationDetails/$medicationId") }
            )
        }
        composable("addEditMedication") {
            AddMedicationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("medicationDetails/{medicationId}") { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getString("medicationId")?.toIntOrNull()
            if (medicationId != null) {
                MedicationDetailsScreen(
                    medicationId = medicationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
