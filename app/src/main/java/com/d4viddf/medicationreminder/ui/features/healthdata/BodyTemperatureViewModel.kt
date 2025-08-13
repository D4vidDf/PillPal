package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class BodyTemperatureViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _bodyTemperatureRecords = MutableStateFlow<List<BodyTemperature>>(emptyList())
    val bodyTemperatureRecords: StateFlow<List<BodyTemperature>> = _bodyTemperatureRecords.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _timeRange = MutableStateFlow(TimeRange.DAY)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    private val _dateRangeText = MutableStateFlow("")
    val dateRangeText: StateFlow<String> = _dateRangeText.asStateFlow()

    init {
        fetchBodyTemperatureRecords()
    }

    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
        _selectedDate.value = LocalDate.now()
        fetchBodyTemperatureRecords()
    }

    fun onPreviousClick() {
        _selectedDate.value = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.minusDays(1)
            TimeRange.WEEK -> _selectedDate.value.minusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.minusMonths(1)
            TimeRange.YEAR -> _selectedDate.value.minusYears(1)
        }
        fetchBodyTemperatureRecords()
    }

    fun onNextClick() {
        val nextDate = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.plusDays(1)
            TimeRange.WEEK -> _selectedDate.value.plusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.plusMonths(1)
            TimeRange.YEAR -> _selectedDate.value.plusYears(1)
        }
        if (nextDate.isAfter(LocalDate.now())) return
        _selectedDate.value = nextDate
        fetchBodyTemperatureRecords()
    }

    private fun fetchBodyTemperatureRecords() {
        viewModelScope.launch {
            val (startTime, endTime) = _timeRange.value.getStartAndEndTimes(_selectedDate.value)
            healthDataRepository.getBodyTemperatureBetween(startTime, endTime)
                .collect { records ->
                    _bodyTemperatureRecords.value = records
                }
            updateDateRangeText()
        }
    }

    private fun updateDateRangeText() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        _dateRangeText.value = when (_timeRange.value) {
            TimeRange.DAY -> when (_selectedDate.value) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> _selectedDate.value.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
            }
            TimeRange.WEEK -> {
                val startOfWeek = _selectedDate.value.with(java.time.DayOfWeek.MONDAY)
                val endOfWeek = _selectedDate.value.with(java.time.DayOfWeek.SUNDAY)
                "${startOfWeek.format(DateTimeFormatter.ofPattern("d MMM"))} - ${endOfWeek.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
            }
            TimeRange.MONTH -> _selectedDate.value.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            TimeRange.YEAR -> _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    }
}
