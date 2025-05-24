package com.d4viddf.medicationreminder.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.ui.screens.AppNavigation
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FabPosition
import androidx.navigation.compose.currentBackStackEntryAsState
import com.d4viddf.medicationreminder.ui.components.AppHorizontalFloatingToolbar
import com.d4viddf.medicationreminder.ui.components.AppNavigationRail
import com.d4viddf.medicationreminder.ui.screens.Screen
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues

@Composable
fun MedicationReminderApp(
    themePreference: String,
    widthSizeClass: WindowWidthSizeClass
) {
    AppTheme(themePreference = themePreference) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val showMainNavigation = currentRoute !in listOf(
            Screen.Settings.route,
            Screen.AddMedication.route,
            Screen.MedicationDetails.route // For full screen details on compact
            // Screen.MedicationDetails.createRoute(0).substringBefore("/") + "/{id}" // Alternative for matching pattern
        ) && !currentRoute.orEmpty().startsWith(Screen.MedicationDetails.route.substringBefore("/{"))


        Log.d("MedicationReminderApp", "Current route: $currentRoute, showMainNavigation: $showMainNavigation")

        Surface(modifier = Modifier.fillMaxSize()) {
            if (showMainNavigation && widthSizeClass != WindowWidthSizeClass.Compact) {
                // Large screens with main navigation visible: Use Row with NavigationRail
                Row(modifier = Modifier.fillMaxSize()) {
                    AppNavigationRail(
                        onHomeClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                        onCalendarClick = { navController.navigate(Screen.Calendar.route) { popUpTo(Screen.Home.route)  } },
                        onProfileClick = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Home.route) } },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Home.route) } },
                        onAddClick = { navController.navigate(Screen.AddMedication.route) { popUpTo(Screen.Home.route) } },
                        currentRoute = currentRoute
                    )
                    AppNavigation(
                        navController = navController,
                        widthSizeClass = widthSizeClass,
                        paddingValues = PaddingValues(), // No padding from a parent scaffold
                        isMainScaffold = false
                    )
                }
            } else {
                // Compact screens OR screens where main navigation should be hidden
                Scaffold(
                    floatingActionButton = {
                        if (showMainNavigation && widthSizeClass == WindowWidthSizeClass.Compact) { // Only show toolbar on compact screens needing main nav
                            AppHorizontalFloatingToolbar(
                                onHomeClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                                onCalendarClick = { navController.navigate(Screen.Calendar.route) { popUpTo(Screen.Home.route) } },
                                onProfileClick = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Home.route) } },
                                onSettingsClick = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Home.route) } },
                                onAddClick = { navController.navigate(Screen.AddMedication.route) { popUpTo(Screen.Home.route) } },
                                currentRoute = currentRoute
                            )
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Center
                ) { innerPadding ->
                    AppNavigation(
                        navController = navController,
                        widthSizeClass = widthSizeClass,
                        paddingValues = innerPadding, // Pass padding from this scaffold
                        isMainScaffold = true
                    )
                }
            }
        }
    }
}
