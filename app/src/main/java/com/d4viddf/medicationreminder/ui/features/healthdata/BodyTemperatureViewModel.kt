package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
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
class BodyTemperatureViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _bodyTemperatureRecords = MutableStateFlow<List<BodyTemperature>>(emptyList())
    val bodyTemperatureRecords: StateFlow<List<BodyTemperature>> = _bodyTemperatureRecords.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    init {
        fetchBodyTemperatureRecords()
    }

    fun onPreviousDayClick() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
        fetchBodyTemperatureRecords()
    }

    fun onNextDayClick() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
        fetchBodyTemperatureRecords()
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        fetchBodyTemperatureRecords()
    }

    private fun fetchBodyTemperatureRecords() {
        viewModelScope.launch {
            val startOfDay = _selectedDate.value.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = _selectedDate.value.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusNanos(1)
            healthDataRepository.getBodyTemperatureBetween(startOfDay, endOfDay)
                .collect { records ->
                    _bodyTemperatureRecords.value = records
                }
        }
    }
}
