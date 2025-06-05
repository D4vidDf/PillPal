package com.d4viddf.medicationreminder.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState // Added import
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment // Added
import androidx.compose.foundation.layout.Box // Added
import androidx.compose.material3.CircularProgressIndicator // Added
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository // Added import
import com.d4viddf.medicationreminder.ui.screens.AppNavigation
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
// import androidx.compose.material3.rememberTopAppBarState // Comment out or remove
import androidx.navigation.compose.currentBackStackEntryAsState
import com.d4viddf.medicationreminder.ui.components.AppHorizontalFloatingToolbar
import com.d4viddf.medicationreminder.ui.components.AppNavigationRail
import com.d4viddf.medicationreminder.ui.screens.Screen
import android.util.Log

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class) // Required for TopAppBarDefaults scroll behaviors
@Composable
fun MedicationReminderApp(
    themePreference: String,
    widthSizeClass: WindowWidthSizeClass,
    userPreferencesRepository: UserPreferencesRepository // Add this parameter
) {
    val onboardingStatus by userPreferencesRepository.onboardingCompletedFlow.collectAsState(initial = null)

    if (onboardingStatus == null) {
        // Display a loading indicator, centered
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Once loaded, determine startRoute and proceed with AppTheme and AppNavigation
        val startRoute = if (onboardingStatus == true) Screen.Home.route else Screen.Onboarding.route

        AppTheme(themePreference = themePreference) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Determine if the current screen is a "main" screen that should show common navigation elements
            val isMainScreen = currentRoute in listOf(Screen.Home.route, Screen.Calendar.route, Screen.Profile.route)

            // Specific routes that should not show the main navigation (Scaffold with FAB or NavRail)
            val hideAllMainChrome = currentRoute in listOf(
                Screen.Settings.route,
                Screen.AddMedication.route,
                Screen.AddMedicationChoice.route, // Added this line
                Screen.Onboarding.route // Add Onboarding here
            ) || currentRoute.orEmpty().startsWith(Screen.MedicationDetails.route.substringBefore("/{"))


            Log.d("MedicationReminderApp", "Current route: $currentRoute, isMainScreen: $isMainScreen, hideAllMainChrome: $hideAllMainChrome, startRoute: $startRoute")


            // Simplified logic for showing navigation elements
            val showNavElements = !hideAllMainChrome

            Surface(modifier = Modifier.fillMaxSize()) {
                if (showNavElements && widthSizeClass != WindowWidthSizeClass.Compact) {
                    // Large screens: Use Row with NavigationRail
                    Row(modifier = Modifier.fillMaxSize()) {
                        AppNavigationRail(
                            onHomeClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                            onCalendarClick = { navController.navigate(Screen.Calendar.route) { popUpTo(Screen.Home.route) } },
                            onProfileClick = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Home.route) } },
                            onSettingsClick = { navController.navigate(Screen.Settings.route) }, // Removed popUpTo for settings
                            onAddClick = { navController.navigate(Screen.AddMedicationChoice.route) }, // Changed here
                            currentRoute = currentRoute
                        )
                        AppNavigation(
                            navController = navController,
                            widthSizeClass = widthSizeClass,
                            isMainScaffold = false,
                            modifier = Modifier.fillMaxSize(),
                            userPreferencesRepository = userPreferencesRepository,
                            startDestinationRoute = startRoute // Pass determined startRoute
                        )
                    }
                } else {
                    // Compact screens OR screens where main navigation is hidden (but not all chrome)
                    Scaffold(
                        modifier = Modifier,
                        topBar = {},
                        floatingActionButton = {
                            if (isMainScreen && widthSizeClass == WindowWidthSizeClass.Compact) {
                                AppHorizontalFloatingToolbar(
                                    onHomeClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                                    onCalendarClick = { navController.navigate(Screen.Calendar.route) { popUpTo(Screen.Home.route) } },
                                    onProfileClick = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Home.route) } },
                                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                    onAddClick = { navController.navigate(Screen.AddMedicationChoice.route) },
                                    currentRoute = currentRoute
                                )
                            }
                        },
                        floatingActionButtonPosition = FabPosition.Center
                    ) {
                        AppNavigation(
                            navController = navController,
                            widthSizeClass = widthSizeClass,
                            isMainScaffold = true,
                            modifier = Modifier.fillMaxSize(),
                            userPreferencesRepository = userPreferencesRepository,
                            startDestinationRoute = startRoute // Pass determined startRoute
                        )
                    }
                }
            }
        }
    }
}
