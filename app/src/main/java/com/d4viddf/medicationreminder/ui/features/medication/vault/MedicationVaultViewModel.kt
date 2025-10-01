package com.d4viddf.medicationreminder.ui.features.medication.vault

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationDosage
import com.d4viddf.medicationreminder.data.repository.MedicationDosageRepository
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.ui.common.model.UiItemState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicationWithDosage(
    val medication: Medication,
    val dosage: MedicationDosage?
)

@HiltViewModel
class MedicationVaultViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val dosageRepository: MedicationDosageRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _medications = MutableStateFlow<List<MedicationWithDosage>>(emptyList())
    private val _medicationsState = MutableStateFlow<List<UiItemState<MedicationWithDosage>>>(emptyList())
    val medicationsState: StateFlow<List<UiItemState<MedicationWithDosage>>> = _medicationsState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MedicationWithDosage>>(emptyList())
    val searchResults: StateFlow<List<MedicationWithDosage>> = _searchResults.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        initialLoadMedications()
        observeSearchQueryAndMedications()
    }

    private fun initialLoadMedications() {
        viewModelScope.launch {
            val allMedications = medicationRepository.getAllMedications().first()
            _medicationsState.value = List(allMedications.size) { UiItemState.Loading }
            delay(500)

            val medicationWithDosages = allMedications.map { medication ->
                val dosage = dosageRepository.getActiveDosage(medication.id)
                MedicationWithDosage(medication, dosage)
            }

            medicationWithDosages.forEachIndexed { index, medWithDosage ->
                delay(75)
                _medicationsState.update { currentList ->
                    currentList.toMutableList().also { mutableList ->
                        if (index < mutableList.size) {
                            mutableList[index] = UiItemState.Success(medWithDosage)
                        }
                    }
                }
            }
            _medications.value = medicationWithDosages
        }
    }

    fun refreshMedications() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val newMedications = medicationRepository.getAllMedications().first()
            val medicationWithDosages = newMedications.map { medication ->
                val dosage = dosageRepository.getActiveDosage(medication.id)
                MedicationWithDosage(medication, dosage)
            }
            _medicationsState.value = medicationWithDosages.map { UiItemState.Success(it) }
            _medications.value = medicationWithDosages
            _isRefreshing.value = false
        }
    }

    private fun observeSearchQueryAndMedications() {
        viewModelScope.launch {
            combine(_searchQuery, _medications) { query, meds ->
                if (query.isBlank()) {
                    emptyList()
                } else {
                    meds.filter { medWithDosage ->
                        medWithDosage.medication.name.contains(query, ignoreCase = true)
                    }
                }
            }.collect { filteredMedications ->
                _searchResults.value = filteredMedications
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}