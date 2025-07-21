package com.d4viddf.medicationreminder.wear.presentation.components

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.d4viddf.medicationreminder.wear.presentation.theme.MedicationReminderTheme
import com.google.android.gms.wearable.Wearable
import com.d4viddf.medicationreminder.wear.presentation.PhoneAppStatus
import com.d4viddf.medicationreminder.wear.presentation.WearViewModel
import com.d4viddf.medicationreminder.wear.presentation.checkAlarmPermission
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState

const val PREFS_NAME = "MedicationReminderPrefs"
const val KEY_FIRST_LAUNCH = "isFirstLaunch"

@Composable
fun WearApp(wearViewModel: WearViewModel) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var isFirstLaunch by remember { mutableStateOf(sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)) }
    var hasAlarmPermission by remember { mutableStateOf(checkAlarmPermission(context)) }

    // Collect all necessary states from the ViewModel
    val reminders by wearViewModel.reminders.collectAsStateWithLifecycle()
    val isLoading by wearViewModel.isLoading.collectAsStateWithLifecycle()
    val phoneAppStatus by wearViewModel.phoneAppStatus.collectAsStateWithLifecycle()
    val selectedReminder by wearViewModel.selectedReminder.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasAlarmPermission = isGranted
        if (!isGranted) {
            Log.w("WearApp", "Schedule exact alarm permission denied by user.")
        }
    }

    MedicationReminderTheme {
        val navController = rememberSwipeDismissableNavController()
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "reminders"
        ) {
            composable("reminders") {
                val listState = rememberTransformingLazyColumnState()
                ScreenScaffold(
                    scrollState = listState,
                ) {
                    // This effect triggers a check for the phone app and syncs data on launch
                    LaunchedEffect(Unit) {
                        wearViewModel.triggerPhoneAppCheckAndSync(
                            RemoteActivityHelper(context),
                            Wearable.getMessageClient(context)
                        )
                    }

                    if (isFirstLaunch) {
                        OnboardingScreen(
                            onDismiss = {
                                sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                                isFirstLaunch = false
                                // Request permission after onboarding if needed
                                if (!hasAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                                }
                            },
                            hasAlarmPermission = hasAlarmPermission,
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
                                }
                            }
                        )
                    } else {
                        // Main content based on the phone app's status
                        when (phoneAppStatus) {
                            PhoneAppStatus.UNKNOWN, PhoneAppStatus.CHECKING -> {
                                CircularProgressIndicator()
                            }
                            PhoneAppStatus.NOT_INSTALLED_ANDROID -> {
                                if (reminders.isNotEmpty()) {
                                    // Show reminders even if phone is not connected
                                    RemindersContent(
                                        reminders = reminders,
                                        onMarkAsTaken = { wearViewModel.markReminderAsTakenOnWatch(it) },
                                        onMoreClick = { navController.navigate("more") },
                                        onReminderClick = { reminder ->
                                            wearViewModel.selectReminder(reminder)
                                            navController.navigate("medicationDetail/${reminder.medicationId}")
                                        },
                                        phoneAppStatus = phoneAppStatus,
                                        onRetry = { wearViewModel.triggerPhoneAppCheckAndSync(RemoteActivityHelper(context), Wearable.getMessageClient(context)) }
                                    )
                                } else {
                                    PhoneAppNotInstalledScreen(
                                        onOpenPlayStore = { wearViewModel.openPlayStoreOnPhone(RemoteActivityHelper(context)) },
                                        onRetry = { wearViewModel.triggerPhoneAppCheckAndSync(RemoteActivityHelper(context), Wearable.getMessageClient(context)) }
                                    )
                                }
                            }
                            PhoneAppStatus.INSTALLED_NO_DATA, PhoneAppStatus.INSTALLED_DATA_REQUESTED -> {
                                if (isLoading && reminders.isEmpty()) {
                                    CircularProgressIndicator()
                                } else {
                                    RemindersContent(
                                        reminders = reminders,
                                        onMarkAsTaken = { wearViewModel.markReminderAsTakenOnWatch(it) },
                                        onMoreClick = { navController.navigate("more") },
                                        onReminderClick = { reminder ->
                                            wearViewModel.selectReminder(reminder)
                                            navController.navigate("medicationDetail/${reminder.medicationId}")
                                        },
                                        phoneAppStatus = phoneAppStatus,
                                        onRetry = { wearViewModel.triggerPhoneAppCheckAndSync(RemoteActivityHelper(context), Wearable.getMessageClient(context)) }
                                    )
                                }
                            }
                            PhoneAppStatus.INSTALLED_WITH_DATA -> {
                                RemindersContent(
                                    reminders = reminders,
                                    onMarkAsTaken = { wearViewModel.markReminderAsTakenOnWatch(it) },
                                    onMoreClick = { navController.navigate("more") },
                                    onReminderClick = { reminder ->
                                        wearViewModel.selectReminder(reminder)
                                        navController.navigate("medicationDetail/${reminder.medicationId}")
                                    },
                                    phoneAppStatus = phoneAppStatus,
                                    onRetry = { wearViewModel.triggerPhoneAppCheckAndSync(RemoteActivityHelper(context), Wearable.getMessageClient(context)) }
                                )
                            }
                        }
                    }
                }
            }
            composable("more") {
                MoreScreen(
                    onSyncClick = {
                        wearViewModel.triggerPhoneAppCheckAndSync(
                            RemoteActivityHelper(context),
                            Wearable.getMessageClient(context)
                        )
                        navController.popBackStack()
                    },
                    onOpenAppClick = {
                        // Correctly calls the function to open the app
                        wearViewModel.openAppOnPhone()
                    },
                    onSettingsClick = { /* TODO: Implement settings navigation */ }
                )
            }
            composable("medicationDetail/{medicationId}") { backStackEntry ->
                val medicationId = backStackEntry.arguments?.getString("medicationId")?.toIntOrNull()
                MedicationDetailScreen(
                    medicationId = medicationId,
                    viewModel = wearViewModel,
                    onOpenOnPhone = {
                        // Uses the collected selectedReminder state
                        wearViewModel.openMedicationDetailsOnPhone(
                            RemoteActivityHelper(context),
                            selectedReminder
                        )
                    }
                )
            }
        }
    }
}