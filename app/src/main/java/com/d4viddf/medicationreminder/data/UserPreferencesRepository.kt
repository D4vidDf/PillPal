package com.d4viddf.medicationreminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// Define the DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_LANGUAGE_TAG = stringPreferencesKey("selected_language_tag")
        val SELECTED_THEME_KEY = stringPreferencesKey("selected_theme_key") // New key for theme
    }

    // Language Preference
    val languageTagFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE_TAG] ?: getDefaultLanguageTag()
        }

    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE_TAG] = tag
        }
    }

    private fun getDefaultLanguageTag(): String {
        return Locale.getDefault().language
    }

    // Theme Preference
    val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME_KEY] ?: ThemeKeys.SYSTEM // Default to System
        }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_THEME_KEY] = theme
        }
    }
}
