package com.d4viddf.medicationreminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// Define the DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings") // Name changed from user_preferences to user_settings to match original

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_LANGUAGE_TAG = stringPreferencesKey("selected_language_tag")
        val SELECTED_THEME_KEY = stringPreferencesKey("selected_theme_key")
    }

    // Language Preference
    val languageTagFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE_TAG] ?: getDefaultLanguageTag()
        }

    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE_TAG] = tag
        }
    }

    private fun getDefaultLanguageTag(): String {
        return Locale.getDefault().toLanguageTag() // Use toLanguageTag() for BCP 47 format
    }

    // Theme Preference
    val themeFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME_KEY] ?: ThemeKeys.SYSTEM // Default to System
        }

    suspend fun setTheme(themeKey: String) { // Parameter name changed to themeKey for clarity
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME_KEY] = themeKey
        }
    }
}
