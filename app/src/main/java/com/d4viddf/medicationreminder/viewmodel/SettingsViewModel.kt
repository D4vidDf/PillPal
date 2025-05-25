package com.d4viddf.medicationreminder.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application // Inject Application to get context
) : ViewModel() {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Volume Control
    private val _currentVolume = MutableStateFlow(0)
    val currentVolume: StateFlow<Int> = _currentVolume.asStateFlow()

    private val _maxVolume = MutableStateFlow(0)
    val maxVolume: StateFlow<Int> = _maxVolume.asStateFlow()

    init {
        _maxVolume.value = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        _currentVolume.value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    fun setVolume(volume: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        _currentVolume.value = volume // Update StateFlow after setting
    }

    // Function to refresh current volume if changed externally, though not strictly required by task
    // but good for robustness if app stays open and volume changes outside.
    fun refreshCurrentVolume() {
        _currentVolume.value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

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
