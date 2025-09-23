package com.d4viddf.medicationreminder.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.d4viddf.medicationreminder.data.model.ThemeKeys
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeSection
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
        val WATER_INTAKE_GOAL = intPreferencesKey("water_intake_goal")
        val WEIGHT_GOAL_TYPE = stringPreferencesKey("weight_goal_type")
        val WEIGHT_GOAL_VALUE = floatPreferencesKey("weight_goal_value")
        // --- NEW KEY FOR HOME LAYOUT ---
        val HOME_LAYOUT_CONFIG = stringPreferencesKey("home_layout_config")
        val SHOW_HEALTH_CONNECT_DATA = booleanPreferencesKey("show_health_connect_data")
        val HEART_RATE_GOAL_MAX = intPreferencesKey("heart_rate_goal_max")
        val WEIGHT_GOAL_MAX = intPreferencesKey("weight_goal_max")
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
                try {
                    val type = object : TypeToken<List<HomeSection>>() {}.type
                    val sections = gson.fromJson<List<HomeSection>>(jsonString, type)
                    // Check if any nameRes is 0, which indicates old format
                    if (sections.any { s -> s.nameRes == 0 || s.items.any { it.nameRes == 0 } }) {
                        emptyList() // Force reset
                    } else {
                        sections
                    }
                } catch (e: Exception) {
                    // If any other error occurs during deserialization, reset to default
                    emptyList()
                }
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

    val waterIntakeGoalFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WATER_INTAKE_GOAL] ?: 4000
        }

    suspend fun setWaterIntakeGoal(goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WATER_INTAKE_GOAL] = goal
        }
    }

    val weightGoalTypeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WEIGHT_GOAL_TYPE] ?: "lose"
        }

    suspend fun setWeightGoalType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEIGHT_GOAL_TYPE] = type
        }
    }

    val weightGoalValueFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WEIGHT_GOAL_VALUE] ?: 0f
        }

    suspend fun setWeightGoalValue(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEIGHT_GOAL_VALUE] = value
        }
    }

    suspend fun deleteWaterIntakeGoal() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.WATER_INTAKE_GOAL)
        }
    }

    suspend fun deleteWeightGoal() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.WEIGHT_GOAL_TYPE)
            preferences.remove(PreferencesKeys.WEIGHT_GOAL_VALUE)
        }
    }

    val showHealthConnectDataFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_HEALTH_CONNECT_DATA] ?: true
        }

    suspend fun setShowHealthConnectData(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_HEALTH_CONNECT_DATA] = show
        }
    }

    val heartRateGoalMaxFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HEART_RATE_GOAL_MAX] ?: 120
        }

    suspend fun setHeartRateGoalMax(goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HEART_RATE_GOAL_MAX] = goal
        }
    }

    val weightGoalMaxFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WEIGHT_GOAL_MAX] ?: 100
        }

    suspend fun setWeightGoalMax(goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEIGHT_GOAL_MAX] = goal
        }
    }
}
