package com.d4viddf.medicationreminder.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val DOSAGE_MIGRATION_DIALOG_SHOWN = booleanPreferencesKey("dosage_migration_dialog_shown")
    }

    val dosageMigrationDialogShown: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DOSAGE_MIGRATION_DIALOG_SHOWN] ?: false
        }

    suspend fun setDosageMigrationDialogShown(wasShown: Boolean) {
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.DOSAGE_MIGRATION_DIALOG_SHOWN] = wasShown
        }
    }
}