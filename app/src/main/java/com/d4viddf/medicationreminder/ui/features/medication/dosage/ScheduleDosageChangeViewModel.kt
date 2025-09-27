package com.d4viddf.medicationreminder.ui.features.medication.dosage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.MedicationDosage
import com.d4viddf.medicationreminder.data.repository.MedicationDosageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ScheduleDosageChangeViewModel @Inject constructor(
    private val dosageRepository: MedicationDosageRepository
) : ViewModel() {

    fun scheduleDosageChange(medicationId: Int, newDosage: String, newStartDate: LocalDate) {
        viewModelScope.launch {
            // 1. Find the current active dosage
            val activeDosage = dosageRepository.getActiveDosage(medicationId)

            // 2. Deactivate the current dosage by setting its end date
            activeDosage?.let {
                val newDosageStartDate = newStartDate.minusDays(1)
                dosageRepository.deactivateDosage(it.id, newDosageStartDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            }

            // 3. Insert the new dosage record
            dosageRepository.insert(
                MedicationDosage(
                    medicationId = medicationId,
                    dosage = newDosage,
                    startDate = newStartDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    endDate = null
                )
            )
        }
    }
}