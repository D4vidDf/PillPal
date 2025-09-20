package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HeartRateViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _timeRange = MutableStateFlow(TimeRange.DAY)
    val timeRange = _timeRange.asStateFlow()

    private val _startTime = MutableStateFlow(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
    val startTime = _startTime.asStateFlow()

    private val _endTime = MutableStateFlow(Instant.now())
    val endTime = _endTime.asStateFlow()

    private val _heartRateData = MutableStateFlow<List<com.d4viddf.medicationreminder.data.model.healthdata.HeartRate>>(emptyList())
    val heartRateData = _heartRateData.asStateFlow()

    init {
        viewModelScope.launch {
            timeRange.collectLatest {
                updateDateRange()
            }
        }
        observeHeartRateData()
    }

    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
    }

    private fun updateDateRange() {
        val now = LocalDate.now()
        val start: Instant
        val end: Instant
        when (_timeRange.value) {
            TimeRange.DAY -> {
                start = now.atStartOfDay(ZoneId.systemDefault()).toInstant()
                end = now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            }
            TimeRange.WEEK -> {
                start = now.minusDays(now.dayOfWeek.value.toLong() - 1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                end = start.plusSeconds(7 * 24 * 3600)
            }
            TimeRange.MONTH -> {
                start = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                end = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            }
            TimeRange.YEAR -> {
                start = now.withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                end = now.plusYears(1).withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            }
        }
        _startTime.value = start
        _endTime.value = end
    }

    private fun observeHeartRateData() {
        viewModelScope.launch {
            healthDataRepository.getHeartRateBetween(startTime.value, endTime.value).collectLatest {
                _heartRateData.value = it
            }
        }
    }
}
