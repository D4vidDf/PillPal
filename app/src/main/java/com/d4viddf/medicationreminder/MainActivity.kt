package com.d4viddf.medicationreminder

import android.Manifest // Import Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager // Import PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.data.ThemeKeys
import com.d4viddf.medicationreminder.data.UserPreferencesRepository
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.ui.MedicationReminderApp
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Import AppTheme
import com.d4viddf.medicationreminder.workers.TestSimpleWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    private var initialLocaleTag: String? = null

    // ActivityResultLauncher for the permission request
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MainActivity", "POST_NOTIFICATIONS permission granted.")
            } else {
                Log.w("MainActivity", "POST_NOTIFICATIONS permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        initialLocaleTag = runBlocking { userPreferencesRepository.languageTagFlow.first() }
        if (!initialLocaleTag.isNullOrEmpty()) {
            val localeList = LocaleListCompat.forLanguageTags(initialLocaleTag)
            AppCompatDelegate.setApplicationLocales(localeList)
            Log.d("MainActivity", "Applied initial locale: $initialLocaleTag")
        }

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createNotificationChannels(this)
            Log.d("MainActivity", "Notification channels created.")
        }

        requestPostNotificationPermission()
        checkAndRequestExactAlarmPermission()

        val testWorkRequest = OneTimeWorkRequestBuilder<TestSimpleWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(testWorkRequest)
        Log.d("MainActivity", "Enqueued TestSimpleWorker")

        setContent {
            val currentTagInPrefs by userPreferencesRepository.languageTagFlow.collectAsState(
                initial = initialLocaleTag ?: Locale.getDefault().toLanguageTag()
            )
            val themePreference by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeKeys.SYSTEM)

            LaunchedEffect(currentTagInPrefs) {
                val actualAppliedTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { Locale.getDefault().toLanguageTag() }
                Log.d("MainActivity", "currentTagInPrefs: $currentTagInPrefs, actualAppliedTag: $actualAppliedTag")
                if (currentTagInPrefs.isNotEmpty() && currentTagInPrefs != actualAppliedTag) {
                    Log.d("MainActivity", "Locale changed. Recreating activity.")
                    recreate() // This will also re-apply the theme if themePreference has changed
                }
            }
            AppTheme(themePreference = themePreference) {
                MedicationReminderApp()
            }
        }
    }

    private fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is API 33
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i("MainActivity", "POST_NOTIFICATIONS permission already granted.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // This block is important if the user denied the permission previously.
                    // You should show a UI explaining why you need the permission
                    // and then invoke the launcher again.
                    Log.i("MainActivity", "Showing rationale for POST_NOTIFICATIONS permission.")
                    // Example: Show a dialog explaining the need for notifications for reminders.
                    // After the user interacts with the dialog, you might call:
                    // requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    // For simplicity now, just re-requesting, but a rationale UI is better UX.
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Directly request the permission for the first time or if no rationale needed.
                    Log.i("MainActivity", "Requesting POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    private fun checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // A partir de Android 12 (API 31)
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("MainActivity", "SCHEDULE_EXACT_ALARM permission not granted. Requesting...")
                // Opcional: Muestra un diálogo al usuario explicando por qué necesitas este permiso.
                Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    // Opcional: puedes añadir tu URI de paquete para que el usuario sea llevado
                    // directamente a la configuración de tu app si es posible.
                    // data = Uri.parse("package:$packageName")
                }.also {
                    try {
                        startActivity(it)
                        // No hay un callback directo para este intent, el usuario debe concederlo manualmente.
                        // Podrías verificar de nuevo en onResume() de la actividad.
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Could not open ACTION_REQUEST_SCHEDULE_EXACT_ALARM settings", e)
                        // Informar al usuario que las alarmas podrían no ser precisas.
                    }
                }
            } else {
                Log.d("MainActivity", "SCHEDULE_EXACT_ALARM permission already granted.")
            }
        }
    }
}