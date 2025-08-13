package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _weightRecords = MutableStateFlow<List<Weight>>(emptyList())
    val weightRecords: StateFlow<List<Weight>> = _weightRecords.asStateFlow()

    private val _timeRange = MutableStateFlow(TimeRange.WEEK)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    init {
        fetchWeightRecords()
    }

    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
        fetchWeightRecords()
    }

    private fun fetchWeightRecords() {
        viewModelScope.launch {
            val (startTime, endTime) = _timeRange.value.getStartAndEndTimes()
            healthDataRepository.getWeightBetween(startTime, endTime)
                .collect { records ->
                    _weightRecords.value = records
                }
        }
    }
}
