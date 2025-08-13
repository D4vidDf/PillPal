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
import javax.inject.Inject

@HiltViewModel
class BodyTemperatureViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _bodyTemperatureRecords = MutableStateFlow<List<BodyTemperature>>(emptyList())
    val bodyTemperatureRecords: StateFlow<List<BodyTemperature>> = _bodyTemperatureRecords.asStateFlow()

    private val _timeRange = MutableStateFlow(TimeRange.WEEK)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    init {
        fetchBodyTemperatureRecords()
    }

    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
        fetchBodyTemperatureRecords()
    }

    private fun fetchBodyTemperatureRecords() {
        viewModelScope.launch {
            val (startTime, endTime) = _timeRange.value.getStartAndEndTimes()
            healthDataRepository.getBodyTemperatureBetween(startTime, endTime)
                .collect { records ->
                    _bodyTemperatureRecords.value = records
                }
        }
    }
}
