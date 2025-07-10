package com.d4viddf.medicationreminder.wear.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WearViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WearViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WearViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
