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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.d4viddf.medicationreminder.data.ThemeKeys
import com.d4viddf.medicationreminder.data.UserPreferencesRepository
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import com.d4viddf.medicationreminder.ui.MedicationReminderApp
// import com.d4viddf.medicationreminder.ui.theme.AppTheme // AppTheme is now applied within MedicationReminderApp
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
    // initialLocaleTag se inicializará en onCreate después de la inyección
    // private var initialLocaleTag: String? = null // Puedes quitarlo si lo lees y aplicas en el mismo flujo

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MainActivity", "POST_NOTIFICATIONS permission granted.")
            } else {
                Log.w("MainActivity", "POST_NOTIFICATIONS permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // La inyección de Hilt para userPreferencesRepository ocurre aquí.
        // Mover la lógica de configuración de idioma ANTES de super.onCreate()
        // para que la UI se infle con el idioma correcto desde el inicio.
        // runBlocking aquí es para la configuración inicial del tema/idioma antes de que la UI se cree.
        // Considera alternativas si esto causa algún ANR en el arranque (aunque para DataStore suele ser rápido).
        val initialLocale = runBlocking { userPreferencesRepository.languageTagFlow.first() }
        if (initialLocale.isNotEmpty()) {
            val appLocale = LocaleListCompat.forLanguageTags(initialLocale)
            AppCompatDelegate.setApplicationLocales(appLocale)
            Log.d("MainActivity", "Initial locale set to: $initialLocale BEFORE super.onCreate")
        }

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
            val windowSizeClass = calculateWindowSizeClass(this)
            // Leer el estado actual del idioma y tema desde el repositorio de preferencias
            val currentLanguageTag by userPreferencesRepository.languageTagFlow.collectAsState(initial = initialLocale)
            val themePreference by userPreferencesRepository.themeFlow.collectAsState(initial = ThemeKeys.SYSTEM)

            // Este LaunchedEffect es para recrear la actividad si el idioma cambia MIENTRAS la app está corriendo.
            // La configuración inicial del idioma ya se hizo antes de setContent.
            LaunchedEffect(currentLanguageTag) {
                val appLocales = AppCompatDelegate.getApplicationLocales()
                if (!appLocales.isEmpty && appLocales.toLanguageTags() != currentLanguageTag && currentLanguageTag.isNotEmpty()) {
                    Log.d("MainActivity", "Locale in prefs ($currentLanguageTag) differs from app's (${appLocales.toLanguageTags()}). Recreating.")
                    recreate()
                }
            }

            // AppTheme ahora se aplica dentro de MedicationReminderApp
            MedicationReminderApp(
                themePreference = themePreference,
                widthSizeClass = windowSizeClass.widthSizeClass
            )
        }
    }

    private fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i("MainActivity", "POST_NOTIFICATIONS permission already granted.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.i("MainActivity", "Showing rationale for POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    Log.i("MainActivity", "Requesting POST_NOTIFICATIONS permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("MainActivity", "SCHEDULE_EXACT_ALARM permission not granted. Requesting...")
                Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }.also {
                    try {
                        startActivity(it)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Could not open ACTION_REQUEST_SCHEDULE_EXACT_ALARM settings", e)
                    }
                }
            } else {
                Log.d("MainActivity", "SCHEDULE_EXACT_ALARM permission already granted.")
            }
        }
    }
}
