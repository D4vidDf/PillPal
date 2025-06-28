package com.d4viddf.medicationreminder

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.ui.common.components.AppHorizontalFloatingToolbar
import com.d4viddf.medicationreminder.ui.common.components.AppNavigationRail
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.navigation.AppNavigation
import com.d4viddf.medicationreminder.ui.navigation.Screen

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class) // Required for TopAppBarDefaults scroll behaviors
@Composable
fun MedicationReminderApp(
    themePreference: String,
    widthSizeClass: WindowWidthSizeClass,
    userPreferencesRepository: UserPreferencesRepository, // Keep this for OnboardingScreen
    onboardingCompleted: Boolean // Add this, make it non-null
) {

    // This logic will now be at the top level inside MedicationReminderApp, after parameters
    val startRoute = if (onboardingCompleted) Screen.Home.route else Screen.Onboarding.route

    AppTheme(themePreference = themePreference) {
        val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Determine if the current screen is a "main" screen that should show common navigation elements
            val isMainScreen = currentRoute in listOf(Screen.Home.route, Screen.Calendar.route, Screen.Profile.route)

            // Specific routes that should not show the main navigation (Scaffold with FAB or NavRail)
            val hideAllMainChrome = currentRoute in listOf(
                // Screen.Settings.route, // Removed this line
                Screen.AddMedication.route,
                Screen.AddMedicationChoice.route,
                Screen.Onboarding.route
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

