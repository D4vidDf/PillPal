package com.d4viddf.medicationreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // Language Preference
    val currentLanguageTag: StateFlow<String> = userPreferencesRepository.languageTagFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun updateLanguageTag(newTag: String) {
        viewModelScope.launch {
            userPreferencesRepository.setLanguageTag(newTag)
        }
    }

    // Theme Preference
    val currentTheme: StateFlow<String> = userPreferencesRepository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.d4viddf.medicationreminder.data.ThemeKeys.SYSTEM // Default to System
        )

    fun updateTheme(themeKey: String) {
        viewModelScope.launch {
            userPreferencesRepository.setTheme(themeKey)
        }
    }
}
