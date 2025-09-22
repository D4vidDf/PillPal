package com.d4viddf.medicationreminder.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SaludConectadaEnHoyViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val showHealthConnectData = userPreferencesRepository.showHealthConnectDataFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onShowHealthConnectDataChange(show: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowHealthConnectData(show)
        }
    }
}
