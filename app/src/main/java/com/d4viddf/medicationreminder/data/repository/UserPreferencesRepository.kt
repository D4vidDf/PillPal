package com.d4viddf.medicationreminder.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.d4viddf.medicationreminder.data.model.ThemeKeys
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeSection
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
// import java.util.Locale // Removed
import javax.inject.Inject
import javax.inject.Singleton

// Define the DataStore instance at the top level of the file
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private companion object {
        val SELECTED_NOTIFICATION_SOUND_URI_KEY = stringPreferencesKey("selected_notification_sound_uri")
    }

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        // --- NEW KEY FOR HOME LAYOUT ---
        val HOME_LAYOUT_CONFIG = stringPreferencesKey("home_layout_config")
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    val themePreferenceFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] ?: "System"
        }

    suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] = theme
        }
    }
    val notificationSoundUriFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SELECTED_NOTIFICATION_SOUND_URI_KEY]
        }

    suspend fun setNotificationSoundUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[SELECTED_NOTIFICATION_SOUND_URI_KEY] = uri
            } else {
                preferences.remove(SELECTED_NOTIFICATION_SOUND_URI_KEY)
            }
        }
    }


    // private fun getDefaultLanguageTag(): String { // Remove this entire block
    //     return Locale.getDefault().toLanguageTag()
    // }

    /**
     * Flow to observe changes to the home screen layout configuration.
     */
    val homeLayoutFlow: Flow<List<HomeSection>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[PreferencesKeys.HOME_LAYOUT_CONFIG]
            if (jsonString.isNullOrEmpty()) {
                emptyList() // Return empty list if no config is saved
            } else {
                // Deserialize the JSON string back into a list of HomeSection objects
                val type = object : TypeToken<List<HomeSection>>() {}.type
                gson.fromJson(jsonString, type)
            }
        }

    /**
     * Saves the user's custom home screen layout configuration.
     * @param sections The list of sections to save.
     */
    suspend fun saveHomeLayout(sections: List<HomeSection>) {
        val jsonString = gson.toJson(sections)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HOME_LAYOUT_CONFIG] = jsonString
        }
    }
}
