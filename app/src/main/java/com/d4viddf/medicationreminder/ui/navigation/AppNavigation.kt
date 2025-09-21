package com.d4viddf.medicationreminder.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.features.medication.add.AddMedicationChoiceScreen
import com.d4viddf.medicationreminder.ui.features.medication.add.AddMedicationScreen
import com.d4viddf.medicationreminder.ui.features.calendar.CalendarScreen
import com.d4viddf.medicationreminder.ui.features.healthdata.BodyTemperatureScreen
import com.d4viddf.medicationreminder.ui.features.healthdata.LogTemperatureScreen
import com.d4viddf.medicationreminder.ui.features.healthdata.LogWaterScreen
import com.d4viddf.medicationreminder.ui.features.healthdata.LogWeightScreen
import com.d4viddf.medicationreminder.ui.features.healthdata.ManageWaterPresetsScreen
import com.d4viddf.medicationreminder.ui.features.healthdata.WaterIntakeScreen
import com.d4viddf.medicationreminder.ui.features.healthdata.WeightScreen
import com.d4viddf.medicationreminder.ui.features.home.HomeScreen
import com.d4viddf.medicationreminder.ui.features.settings.NutritionWeightSettingsScreen
import com.d4viddf.medicationreminder.ui.features.settings.WaterIntakeGoalScreen
import com.d4viddf.medicationreminder.ui.features.medication.details.MedicationDetailsScreen
import com.d4viddf.medicationreminder.ui.features.medication.details.MedicationInfoScreen
import com.d4viddf.medicationreminder.ui.features.medication.schedules.AllSchedulesScreen
import com.d4viddf.medicationreminder.ui.features.medication.graph.MedicationGraphScreen
import com.d4viddf.medicationreminder.ui.features.medication.history.MedicationHistoryScreen
import com.d4viddf.medicationreminder.ui.features.medication.vault.MedicationVaultScreen
import com.d4viddf.medicationreminder.ui.features.onboarding.OnboardingScreen
import com.d4viddf.medicationreminder.ui.features.profile.screen.ProfileScreen
import com.d4viddf.medicationreminder.ui.features.settings.components.ResponsiveSettingsScaffold
import com.d4viddf.medicationreminder.ui.features.synceddevices.screen.ConnectedDevicesScreen
import com.d4viddf.medicationreminder.ui.features.todayschedules.TodaySchedulesScreen
import com.d4viddf.medicationreminder.ui.features.medication.graph.MedicationGraphViewModel
import com.d4viddf.medicationreminder.ui.features.personalizehome.PersonalizeHomeScreen
import java.time.format.DateTimeFormatter

const val MEDICATION_ID_ARG = "medicationId"
const val SHOW_TODAY_ARG = "showToday"
// ** NEW: Argument key for the missed filter **
const val SHOW_MISSED_ARG = "showMissed"

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object MedicationVault : Screen("medicationVault")
    object AddMedication : Screen("addMedication")
    object AddMedicationChoice : Screen("addMedicationChoice")
    object MedicationDetails : Screen("medicationDetails/{$MEDICATION_ID_ARG}?enableSharedTransition={enableSharedTransition}") {
        fun createRoute(id: Int, enableSharedTransition: Boolean = true) = "medicationDetails/$id?enableSharedTransition=$enableSharedTransition"
    }
    object Settings : Screen("settings")
    object Analysis : Screen("analysis")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    data object Onboarding : Screen("onboarding_screen")

    object AllSchedules : Screen("all_schedules_screen/{$MEDICATION_ID_ARG}/{colorName}?$SHOW_TODAY_ARG={$SHOW_TODAY_ARG}") {
        fun createRoute(medicationId: Int, colorName: String, showToday: Boolean = false) = "all_schedules_screen/$medicationId/$colorName?$SHOW_TODAY_ARG=$showToday"
    }
    object MedicationHistory : Screen("medication_history_screen/{$MEDICATION_ID_ARG}/{colorName}") {
        fun createRoute(medicationId: Int, colorName: String, selectedDate: String? = null, selectedMonth: String? = null): String {
            var route = "medication_history_screen/$medicationId/$colorName"
            val queryParams = mutableListOf<String>()
            selectedDate?.let { queryParams.add("selectedDate=$it") }
            selectedMonth?.let { queryParams.add("selectedMonth=$it") }
            if (queryParams.isNotEmpty()) {
                route += "?" + queryParams.joinToString("&")
            }
            return route
        }
    }
    object MedicationGraph : Screen("medication_graph_screen/{$MEDICATION_ID_ARG}/{colorName}") {
        fun createRoute(medicationId: Int, colorName: String) = "medication_graph_screen/$medicationId/$colorName"
    }
    object MedicationInfo : Screen("medication_info_screen/{$MEDICATION_ID_ARG}/{colorName}") {
        fun createRoute(medicationId: Int, colorName: String) = "medication_info_screen/$medicationId/$colorName"
    }
    object ConnectedDevices : Screen("connected_devices_screen")

    // ** MODIFIED: Updated route definition to accept the optional 'showMissed' argument **
    object TodaySchedules : Screen("today_schedules?$SHOW_MISSED_ARG={$SHOW_MISSED_ARG}") {
        fun createRoute(showMissed: Boolean = false) = "today_schedules?$SHOW_MISSED_ARG=$showMissed"
    }
    object PersonalizeHome : Screen("personalizeHome")
    object LogWater : Screen("logWater")
    object LogWeight : Screen("logWeight")
    object LogTemperature : Screen("logTemperature")
    object Weight : Screen("weight")
    object WaterIntake : Screen("waterIntake")
    object BodyTemperature : Screen("bodyTemperature")
    object ManageWaterPresets : Screen("manageWaterPresets")
    object NutritionWeightSettings : Screen("nutritionWeightSettings")
    object WaterIntakeGoal : Screen("waterIntakeGoal")
    object WeightGoal : Screen("weightGoal")
    object HeartRate : Screen("heartRate")
    object HealthConnectSettings : Screen("healthConnectSettings")
    object PrivacyPolicy : Screen("privacyPolicy")
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    widthSizeClass: WindowWidthSizeClass,
    isMainScaffold: Boolean,
    userPreferencesRepository: UserPreferencesRepository,
    startDestinationRoute: String,
    hostScaffoldPadding: PaddingValues = PaddingValues()
) {
    SharedTransitionLayout {
        val currentSharedTransitionScope = this

        NavHost(
            navController = navController,
            startDestination = startDestinationRoute,
            modifier = modifier.then(if (isMainScaffold) Modifier.fillMaxSize() else Modifier),
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    navController = navController,
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    widthSizeClass = widthSizeClass,
                )
            }
            composable(Screen.MedicationVault.route) {
                MedicationVaultScreen(
                    navController = navController,
                    widthSizeClass = widthSizeClass,
                    sharedTransitionScope = currentSharedTransitionScope,
                    animatedVisibilityScope = this,
                    hostPaddingValues = hostScaffoldPadding
                )
            }
            composable(Screen.AddMedicationChoice.route) {
                AddMedicationChoiceScreen(
                    onSearchMedication = { navController.navigate(Screen.AddMedication.route) },
                    onUseCamera = { /* TODO */ },
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Screen.AddMedication.route) {
                AddMedicationScreen(
                    navController = navController,
                    widthSizeClass = widthSizeClass
                )
            }
            composable(
                Screen.MedicationDetails.route,
                arguments = listOf(navArgument("enableSharedTransition") { type = NavType.BoolType; defaultValue = true })
            ) { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getString(MEDICATION_ID_ARG)?.toIntOrNull()
                val enableSharedTransition = backStackEntry.arguments?.getBoolean("enableSharedTransition") ?: true
                if (medicationId != null) {
                    MedicationDetailsScreen(
                        medicationId = medicationId,
                        navController = navController,
                        onNavigateBack = {
                            navController.previousBackStackEntry?.savedStateHandle?.set("medicationDetailClosed", true)
                            navController.popBackStack()
                        },
                        sharedTransitionScope = currentSharedTransitionScope,
                        animatedVisibilityScope = this,
                        isHostedInPane = false,
                        widthSizeClass = widthSizeClass,
                        onNavigateToAllSchedules = { medId, colorName ->
                            navController.navigate(
                                Screen.AllSchedules.createRoute(medId, colorName, true)
                            )
                        },
                        onNavigateToMedicationHistory = { medId, colorName ->
                            navController.navigate(
                                Screen.MedicationHistory.createRoute(medId, colorName)
                            )
                        },
                        onNavigateToMedicationGraph = { medId, colorName ->
                            navController.navigate(
                                Screen.MedicationGraph.createRoute(medId, colorName)
                            )
                        },
                        onNavigateToMedicationInfo = { medId, colorName ->
                            navController.navigate(
                                Screen.MedicationInfo.createRoute(medId, colorName)
                            )
                        }
                    )
                }
            }
            composable(Screen.Settings.route) {
                var currentSettingsTitleResId by remember { mutableStateOf(R.string.settings_screen_title) }
                var currentSettingsBackAction by remember { mutableStateOf<() -> Unit>({ navController.popBackStack() }) }

                if (widthSizeClass == WindowWidthSizeClass.Compact) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(stringResource(id = currentSettingsTitleResId)) },
                                navigationIcon = {
                                    IconButton(onClick = { currentSettingsBackAction.invoke() }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_arrow_back_ios_24),
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
                            updateTopBarActions = { titleResId, backAction ->
                                currentSettingsTitleResId = titleResId
                                currentSettingsBackAction = backAction
                            },
                            contentPadding = innerPadding,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    ResponsiveSettingsScaffold(
                        widthSizeClass = widthSizeClass,
                        navController = navController,
                        updateTopBarActions = { _, _ -> /* No-op */ },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    navController = navController,
                    widthSizeClass = widthSizeClass,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMedicationDetail = { medicationId ->
                        navController.navigate(Screen.MedicationDetails.createRoute(medicationId, enableSharedTransition = false))
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Analysis.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Analysis Screen (Placeholder)")
                }
            }

            composable(
                Screen.AllSchedules.route,
                arguments = listOf(
                    navArgument(MEDICATION_ID_ARG) { type = NavType.IntType },
                    navArgument("colorName") { type = NavType.StringType },
                    navArgument(SHOW_TODAY_ARG) { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getInt(MEDICATION_ID_ARG) ?: -1
                val colorName = backStackEntry.arguments?.getString("colorName")
                val showToday = backStackEntry.arguments?.getBoolean(SHOW_TODAY_ARG) ?: false
                AllSchedulesScreen(
                    medicationId = medicationId,
                    showToday = showToday,
                    onNavigateBack = { navController.popBackStack() },
                    colorName = colorName ?: MedicationColor.LIGHT_ORANGE.name
                )
            }

            composable(
                Screen.MedicationHistory.route,
                arguments = listOf(
                    navArgument(MEDICATION_ID_ARG) { type = NavType.IntType },
                    navArgument("colorName") { type = NavType.StringType },
                    navArgument("selectedDate") { type = NavType.StringType; nullable = true },
                    navArgument("selectedMonth") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getInt(MEDICATION_ID_ARG) ?: -1
                val colorName = backStackEntry.arguments?.getString("colorName")
                val selectedDate = backStackEntry.arguments?.getString("selectedDate")
                val selectedMonth = backStackEntry.arguments?.getString("selectedMonth")
                MedicationHistoryScreen(
                    medicationId = medicationId,
                    onNavigateBack = { navController.popBackStack() },
                    colorName = colorName ?: MedicationColor.LIGHT_ORANGE.name,
                    selectedDate = selectedDate,
                    selectedMonth = selectedMonth
                )
            }

            composable(
                Screen.MedicationGraph.route,
                arguments = listOf(
                    navArgument(MEDICATION_ID_ARG) { type = NavType.IntType },
                    navArgument("colorName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getInt(MEDICATION_ID_ARG) ?: -1
                val colorName = backStackEntry.arguments?.getString("colorName")
                val medicationGraphViewModel: MedicationGraphViewModel = hiltViewModel()
                MedicationGraphScreen(
                    medicationId = medicationId,
                    onNavigateBack = { navController.popBackStack() },
                    colorName = colorName ?: MedicationColor.LIGHT_ORANGE.name,
                    viewModel = medicationGraphViewModel,
                    widthSizeClass = widthSizeClass,
                    onNavigateToHistoryForDate = { medId, colorStr, date ->
                        navController.navigate(
                            Screen.MedicationHistory.createRoute(
                                medicationId = medId,
                                colorName = colorStr,
                                selectedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            )
                        )
                    },
                    onNavigateToHistoryForMonth = { medId, colorStr, yearMonth ->
                        navController.navigate(
                            Screen.MedicationHistory.createRoute(
                                medicationId = medId,
                                colorName = colorStr,
                                selectedMonth = yearMonth.toString()
                            )
                        )
                    }
                )
            }

            composable(
                Screen.MedicationInfo.route,
                arguments = listOf(
                    navArgument(MEDICATION_ID_ARG) { type = NavType.IntType },
                    navArgument("colorName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getInt(MEDICATION_ID_ARG) ?: -1
                val colorName = backStackEntry.arguments?.getString("colorName")
                MedicationInfoScreen(
                    medicationId = medicationId,
                    onNavigateBack = { navController.popBackStack() },
                    colorName = colorName ?: MedicationColor.LIGHT_ORANGE.name
                )
            }

            composable(Screen.ConnectedDevices.route) {
                ConnectedDevicesScreen(navController = navController)
            }

            // ** MODIFIED: Updated composable definition to handle the new argument **
            composable(
                route = Screen.TodaySchedules.route,
                arguments = listOf(
                    navArgument(SHOW_MISSED_ARG) {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) {
                // The ViewModel will get the argument from the SavedStateHandle automatically.
                // No need to pass it here.
                TodaySchedulesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetails = { medicationId ->
                        navController.navigate(
                            Screen.MedicationDetails.createRoute(
                                medicationId,
                                enableSharedTransition = true
                            )
                        )
                    }
                )
            }

            composable(Screen.PersonalizeHome.route) {
                PersonalizeHomeScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.LogWater.route) {
                LogWaterScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.LogWeight.route) {
                LogWeightScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.LogTemperature.route) {
                LogTemperatureScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Weight.route) {
                WeightScreen(navController = navController, widthSizeClass = widthSizeClass)
            }
            composable(Screen.WaterIntake.route) {
                WaterIntakeScreen(navController = navController, widthSizeClass = widthSizeClass)
            }
            composable(Screen.BodyTemperature.route) {
                BodyTemperatureScreen(navController = navController, widthSizeClass = widthSizeClass)
            }
            composable(Screen.ManageWaterPresets.route) {
                ManageWaterPresetsScreen(navController = navController)
            }
            composable(Screen.NutritionWeightSettings.route) {
                NutritionWeightSettingsScreen(navController = navController)
            }
            composable(Screen.WaterIntakeGoal.route) {
                WaterIntakeGoalScreen(navController = navController)
            }
            composable(Screen.WeightGoal.route) {
                com.d4viddf.medicationreminder.ui.features.settings.WeightGoalScreen(navController = navController)
            }
            composable(Screen.HeartRate.route) {
                com.d4viddf.medicationreminder.ui.features.healthdata.HeartRateScreen(navController = navController)
            }
            composable(Screen.HealthConnectSettings.route) {
                com.d4viddf.medicationreminder.ui.features.settings.HealthConnectSettingsScreen(navController = navController)
            }
            composable(Screen.PrivacyPolicy.route) {
                com.d4viddf.medicationreminder.ui.features.settings.PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}