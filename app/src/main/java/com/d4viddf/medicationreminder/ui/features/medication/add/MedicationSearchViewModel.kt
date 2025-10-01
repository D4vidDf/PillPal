package com.d4viddf.medicationreminder.ui.features.medication.add

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.MedicationSearchResult
import com.d4viddf.medicationreminder.data.repository.MedicationInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationSearchViewModel @Inject constructor(
    private val medicationInfoRepository: MedicationInfoRepository
) : ViewModel() {

    private val _medicationSearchResults = MutableStateFlow<List<MedicationSearchResult>>(emptyList())
    val medicationSearchResults: StateFlow<List<MedicationSearchResult>> = _medicationSearchResults.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val TAG = "MedSearchVM"

    fun searchMedication(query: String) {
        if (query.length < 3) {
            _medicationSearchResults.value = emptyList()
            // _isLoading.value = false // Ensure loading is false if query is too short
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "Searching for: $query")
            _isLoading.value = true
            try {
                val results = medicationInfoRepository.searchMedication(query)
                _medicationSearchResults.value = results
                Log.d(TAG, "Results count: ${results.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching medication: $query", e)
                _medicationSearchResults.value = emptyList() // Clear results on error
            } finally {
                _isLoading.value = false
                Log.d(TAG, "Search finished for: $query, isLoading: false")
            }
        }
    }

    fun clearSearchResults() {
        Log.d(TAG, "Clearing search results")
        _medicationSearchResults.value = emptyList()
        _isLoading.value = false // Also reset loading state
    }
}
