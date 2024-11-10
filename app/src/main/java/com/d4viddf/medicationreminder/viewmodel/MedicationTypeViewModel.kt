package com.d4viddf.medicationreminder.viewmodel

import androidx.lifecycle.ViewModel
import com.d4viddf.medicationreminder.data.MedicationTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MedicationTypeViewModel @Inject constructor(
    private val repository: MedicationTypeRepository
) : ViewModel() {

    val medicationTypes = repository.getAllMedicationTypes()
}
