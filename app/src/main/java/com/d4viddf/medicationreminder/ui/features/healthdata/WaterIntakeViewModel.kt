package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class WaterIntakeViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _waterIntakeRecords = MutableStateFlow<List<WaterIntake>>(emptyList())
    val waterIntakeRecords: StateFlow<List<WaterIntake>> = _waterIntakeRecords.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    init {
        fetchWaterIntakeRecords()
    }

    fun onPreviousDayClick() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
        fetchWaterIntakeRecords()
    }

    fun onNextDayClick() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
        fetchWaterIntakeRecords()
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        fetchWaterIntakeRecords()
    }

    private fun fetchWaterIntakeRecords() {
        viewModelScope.launch {
            val startOfDay = _selectedDate.value.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = _selectedDate.value.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusNanos(1)
            healthDataRepository.getWaterIntakeBetween(startOfDay, endOfDay)
                .collect { records ->
                    _waterIntakeRecords.value = records
                }
        }
    }
}
