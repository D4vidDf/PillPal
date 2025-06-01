package com.d4viddf.medicationreminder.viewmodel

import android.app.Application // Keep one import
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.d4viddf.medicationreminder.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository
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
    private val application: Application // Inject Application and store to use contentResolver
) : ViewModel() {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Volume Control
    private val _currentVolume = MutableStateFlow(0)
    val currentVolume: StateFlow<Int> = _currentVolume.asStateFlow()

    private val _maxVolume = MutableStateFlow(0)
    val maxVolume: StateFlow<Int> = _maxVolume.asStateFlow()

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            Log.d("SettingsViewModel", "Volume observer onChange triggered. selfChange: $selfChange")
            val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            Log.d("SettingsViewModel", "New alarm volume read by observer: $newVolume")
            if (_currentVolume.value != newVolume) { // Only update if different to avoid potential loops if setVolume also triggers onChange
                _currentVolume.value = newVolume
                Log.d("SettingsViewModel", "_currentVolume updated to: $newVolume")
            }
        }
    }

    init {
        _maxVolume.value = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        _currentVolume.value = audioManager.getStreamVolume(AudioManager.STREAM_ALARM) // Initial fetch


    }

    fun setVolume(volume: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
        // Update StateFlow immediately for responsive UI.
        // If observer triggers too, the check in onChange should prevent issues.
        if (_currentVolume.value != volume) {
            _currentVolume.value = volume
        }
    }

    // This function might become redundant if the observer works reliably,
    // but can be kept for explicit UI-triggered refresh if desired.
    fun refreshCurrentVolume() {
        _currentVolume.value = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    }

    override fun onCleared() {
        super.onCleared()
        application.contentResolver.unregisterContentObserver(volumeObserver)
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

    // Notification Sound Preference
    val currentNotificationSoundUri: StateFlow<String?> = userPreferencesRepository.notificationSoundUriFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null // Default to null (no specific sound set)
        )

    fun updateNotificationSoundUri(uri: String?) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationSoundUri(uri)
        }
    }

    fun getNotificationSoundName(uriString: String?): String {
        return if (uriString.isNullOrEmpty()) {
            application.getString(R.string.settings_notification_sound_default)
        } else {
            try {
                val uri = Uri.parse(uriString)
                val ringtone = RingtoneManager.getRingtone(application, uri)
                ringtone?.getTitle(application) ?: application.getString(R.string.settings_notification_sound_unknown)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error getting ringtone title", e)
                application.getString(R.string.settings_notification_sound_unknown)
            }
        }
    }
}
