package com.d4viddf.medicationreminder.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.d4viddf.medicationreminder.data.ThemeKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// Define the DataStore instance at the top level of the file
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        val SELECTED_LANGUAGE_KEY = stringPreferencesKey("selected_language_tag")
        val SELECTED_THEME_KEY = stringPreferencesKey("selected_theme_key")
        val SELECTED_NOTIFICATION_SOUND_URI_KEY = stringPreferencesKey("selected_notification_sound_uri")
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    }

    val languageTagFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SELECTED_LANGUAGE_KEY] ?: getDefaultLanguageTag()
        }

    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE_KEY] = tag
        }
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: false // Default to false if not set
        }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
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

    val themeFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SELECTED_THEME_KEY] ?: ThemeKeys.SYSTEM
        }

    suspend fun setTheme(themeKey: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_THEME_KEY] = themeKey
        }
    }

    private fun getDefaultLanguageTag(): String {
        return Locale.getDefault().toLanguageTag()
    }
}
