package com.d4viddf.medicationreminder.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.ui.common.component.AppHorizontalFloatingToolbar
import com.d4viddf.medicationreminder.ui.common.component.AppNavigationRail
import com.d4viddf.medicationreminder.ui.common.component.bottomsheet.AddDataBottomSheet
import com.d4viddf.medicationreminder.ui.common.component.bottomsheet.getBottomSheetData
import com.d4viddf.medicationreminder.ui.navigation.AppNavigation
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationReminderApp(
    themePreference: String,
    widthSizeClass: WindowWidthSizeClass,
    userPreferencesRepository: UserPreferencesRepository,
    onboardingCompleted: Boolean
) {
    val startRoute = if (onboardingCompleted) Screen.Home.route else Screen.Onboarding.route

    AppTheme(themePreference = themePreference) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // --- Bottom Sheet State Management ---
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        var showBottomSheet by remember { mutableStateOf(false) }
        val bottomSheetData = getBottomSheetData(navController)
        // --- End of Bottom Sheet State ---

        val mainScreenRoutes = listOf(
            Screen.Home.route,
            Screen.MedicationVault.route,
            Screen.Calendar.route,
            Screen.Analysis.route,
            Screen.Profile.route,
        )
        val isMainScreen = currentRoute in mainScreenRoutes

        val hideAllMainChrome = currentRoute in listOf(
            Screen.Onboarding.route
        ) || currentRoute.orEmpty().startsWith(Screen.AddMedication.route) ||
                currentRoute.orEmpty().startsWith(Screen.MedicationDetails.route.substringBefore("/{")) || currentRoute.orEmpty().startsWith(Screen.LogWater.route) || currentRoute.orEmpty().startsWith(Screen.LogWeight.route) || currentRoute.orEmpty().startsWith(Screen.LogTemperature.route)

        Log.d("MedicationReminderApp", "Current route: $currentRoute, isMainScreen: $isMainScreen, hideAllMainChrome: $hideAllMainChrome, startRoute: $startRoute")

        val showNavElements = !hideAllMainChrome

        Surface(modifier = Modifier.fillMaxSize()) {
            if (showNavElements && widthSizeClass != WindowWidthSizeClass.Compact) {
                Row(modifier = Modifier.fillMaxSize()) {
                    AppNavigationRail(
                        onHomeClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                        onMedicationVaultClick = { navController.navigate(Screen.MedicationVault.route) { popUpTo(Screen.Home.route) } },
                        onCalendarClick = { navController.navigate(Screen.Calendar.route) { popUpTo(Screen.Home.route) } },
                        onAnalysisClick = { navController.navigate(Screen.Analysis.route) { popUpTo(Screen.Home.route) } },
                        onProfileClick = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Home.route) } },
                        // **MODIFIED ACTION**
                        onAddClick = { showBottomSheet = true },
                        currentRoute = currentRoute
                    )
                    AppNavigation(
                        navController = navController,
                        widthSizeClass = widthSizeClass,
                        isMainScaffold = false,
                        userPreferencesRepository = userPreferencesRepository,
                        startDestinationRoute = startRoute
                    )
                }
            } else {
                Scaffold(
                    modifier = Modifier,
                    topBar = {},
                    floatingActionButton = {
                        if (isMainScreen && widthSizeClass == WindowWidthSizeClass.Compact) {
                            AppHorizontalFloatingToolbar(
                                onHomeClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                                onMedicationVaultClick = { navController.navigate(Screen.MedicationVault.route) { popUpTo(Screen.Home.route) } },
                                onCalendarClick = { navController.navigate(Screen.Calendar.route) { popUpTo(Screen.Home.route) } },
                                onAnalysisClick = { navController.navigate(Screen.Analysis.route) { popUpTo(Screen.Home.route) } },
                                onProfileClick = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Home.route) } },
                                // **MODIFIED ACTION**
                                onAddClick = { showBottomSheet = true },
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
                        userPreferencesRepository = userPreferencesRepository,
                        startDestinationRoute = startRoute
                    )
                }
            }

            // --- Display Bottom Sheet Conditionally ---
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState
                ) {
                    AddDataBottomSheet(
                        groups = bottomSheetData.map { group ->
                            group.copy(items = group.items.map { item ->
                                item.copy(action = {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            item.action()
                                            showBottomSheet = false
                                        }
                                    }
                                })
                            })
                        }
                    )
                }
            }
        }
    }
}