package com.d4viddf.medicationreminder.viewmodel

import MedicationSearchResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.MedicationInfo
import com.d4viddf.medicationreminder.data.MedicationInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MedicationInfoViewModel @Inject constructor(
    private val medicationInfoRepository: MedicationInfoRepository
) : ViewModel() {

    // StateFlow to expose the current MedicationInfo
    private val _medicationInfo = MutableStateFlow<MedicationInfo?>(null)
    val medicationInfo: StateFlow<MedicationInfo?> = _medicationInfo

    // StateFlow to expose the list of search results from CIMA API
    private val _medicationSearchResults = MutableStateFlow<List<MedicationSearchResult>>(emptyList())
    val medicationSearchResults: StateFlow<List<MedicationSearchResult>> = _medicationSearchResults

    // Insert Medication Info to the database
    fun insertMedicationInfo(medicationInfo: MedicationInfo) {
        viewModelScope.launch {
            medicationInfoRepository.insertMedicationInfo(medicationInfo)
        }
    }

    // Fetch Medication Info by ID and update StateFlow
    fun getMedicationInfoById(medicationId: Int) {
        viewModelScope.launch {
            val info = medicationInfoRepository.getMedicationInfoById(medicationId)
            _medicationInfo.value = info
        }
    }

    // Search for medication using CIMA API and update StateFlow
    fun searchMedication(query: String) {
        if (query.length >= 3) {
            viewModelScope.launch(Dispatchers.IO) { // Run on background thread
                try {
                    val searchResults = medicationInfoRepository.searchMedication(query)
                    withContext(Dispatchers.Main) {
                        _medicationSearchResults.value = searchResults // Update StateFlow safely on the main thread
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        // Handle error gracefully, e.g., update to empty list
                        _medicationSearchResults.value = emptyList()
                    }
                }
            }
        } else {
            _medicationSearchResults.value = emptyList()
        }
    }


}
