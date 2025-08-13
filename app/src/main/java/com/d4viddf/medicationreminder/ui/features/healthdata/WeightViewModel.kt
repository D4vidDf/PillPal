package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
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
class WeightViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _weightRecords = MutableStateFlow<List<Weight>>(emptyList())
    val weightRecords: StateFlow<List<Weight>> = _weightRecords.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    init {
        fetchWeightRecords()
    }

    fun onPreviousDayClick() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
        fetchWeightRecords()
    }

    fun onNextDayClick() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
        fetchWeightRecords()
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        fetchWeightRecords()
    }

    private fun fetchWeightRecords() {
        viewModelScope.launch {
            val startOfDay = _selectedDate.value.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = _selectedDate.value.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusNanos(1)
            healthDataRepository.getWeightBetween(startOfDay, endOfDay)
                .collect { records ->
                    _weightRecords.value = records
                }
        }
    }
}
