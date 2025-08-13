package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaterIntakeViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _waterIntakeRecords = MutableStateFlow<List<WaterIntake>>(emptyList())
    val waterIntakeRecords: StateFlow<List<WaterIntake>> = _waterIntakeRecords.asStateFlow()

    private val _timeRange = MutableStateFlow(TimeRange.WEEK)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    init {
        fetchWaterIntakeRecords()
    }

    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
        fetchWaterIntakeRecords()
    }

    private fun fetchWaterIntakeRecords() {
        viewModelScope.launch {
            val (startTime, endTime) = _timeRange.value.getStartAndEndTimes()
            healthDataRepository.getWaterIntakeBetween(startTime, endTime)
                .collect { records ->
                    _waterIntakeRecords.value = records
                }
        }
    }
}
