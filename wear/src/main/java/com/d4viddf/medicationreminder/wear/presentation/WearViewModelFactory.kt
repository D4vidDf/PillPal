package com.d4viddf.medicationreminder.wear.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.d4viddf.medicationreminder.wear.data.WearRepository
import javax.inject.Inject

class WearViewModelFactory @Inject constructor(
    private val application: Application,
    private val wearRepository: WearRepository
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WearViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WearViewModel(application, wearRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
