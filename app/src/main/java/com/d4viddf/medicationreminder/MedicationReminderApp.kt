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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState // Ensure this is present
// import androidx.compose.material3.rememberTopAppBarState // Comment out or remove
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.compose.currentBackStackEntryAsState
import com.d4viddf.medicationreminder.ui.components.AppHorizontalFloatingToolbar
import com.d4viddf.medicationreminder.ui.components.AppNavigationRail
import com.d4viddf.medicationreminder.ui.screens.Screen
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.R // Assuming R class is available for string resources

@OptIn(ExperimentalMaterial3Api::class) // Required for TopAppBarDefaults scroll behaviors
@Composable
fun MedicationReminderApp(
    themePreference: String,
    widthSizeClass: WindowWidthSizeClass
) {
    AppTheme(themePreference = themePreference) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Determine if the current screen is a "main" screen that should show common navigation elements
        val isMainScreen = currentRoute in listOf(Screen.Home.route, Screen.Calendar.route, Screen.Profile.route)

        // Specific routes that should not show the main navigation (Scaffold with FAB or NavRail)
        // This now more clearly defines when to hide all main chrome.
        val hideAllMainChrome = currentRoute in listOf(
            Screen.Settings.route,
            Screen.AddMedication.route
        ) || currentRoute.orEmpty().startsWith(Screen.MedicationDetails.route.substringBefore("/{"))


        Log.d("MedicationReminderApp", "Current route: $currentRoute, isMainScreen: $isMainScreen, hideAllMainChrome: $hideAllMainChrome")

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(remember { TopAppBarState(0f, 0f, 0f) })


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
                        onAddClick = { navController.navigate(Screen.AddMedication.route) }, // Removed popUpTo for add
                        currentRoute = currentRoute
                    )
                    // AppNavigation for large screens does not need a Scaffold with TopAppBar from here,
                    // individual screens might have their own if needed.
                    AppNavigation(
                        navController = navController,
                        widthSizeClass = widthSizeClass,
                        paddingValues = PaddingValues(), // No padding from a parent scaffold
                        isMainScaffold = false, // Does not use this app's main scaffold
                        modifier = Modifier.fillMaxSize() // Ensure AppNavigation fills its space
                    )
                }
            } else {
                // Compact screens OR screens where main navigation is hidden (but not all chrome)
                Scaffold(
                    modifier = if (isMainScreen && widthSizeClass == WindowWidthSizeClass.Compact) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier,
                    topBar = {
                        if (isMainScreen && widthSizeClass == WindowWidthSizeClass.Compact) { // Only show TopAppBar on main compact screens
                            TopAppBar(
                                title = {
                                    Text(text = when (currentRoute) {
                                        Screen.Home.route -> stringResource(id = R.string.home_screen_title)
                                        Screen.Calendar.route -> stringResource(id = R.string.calendar_screen_title)
                                        Screen.Profile.route -> stringResource(id = R.string.profile_screen_title)
                                        else -> ""
                                    })
                                },
                                scrollBehavior = scrollBehavior
                            )
                        }
                    },
                    floatingActionButton = {
                        // Show FAB only on main screens in compact mode
                        if (isMainScreen && widthSizeClass == WindowWidthSizeClass.Compact) {
                            AppHorizontalFloatingToolbar(
                                onHomeClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                                onCalendarClick = { navController.navigate(Screen.Calendar.route) { popUpTo(Screen.Home.route) } },
                                onProfileClick = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Home.route) } },
                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                onAddClick = { navController.navigate(Screen.AddMedication.route) },
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
                        // isMainScaffold is true if this Scaffold is effectively the main one for the content
                        isMainScaffold = true,
                        modifier = Modifier.fillMaxSize() // Ensure AppNavigation uses the padding
                    )
                }
            }
        }
    }
}
