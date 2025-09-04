package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.ui.features.common.charts.ChartDataPoint
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

data class WeightUiState(
    val chartData: List<ChartDataPoint> = emptyList(),
    val weightLogs: List<WeightLogItem> = emptyList()
)

data class WeightLogItem(
    val weight: Double,
    val date: ZonedDateTime
)

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _weightUiState = MutableStateFlow(WeightUiState())
    val weightUiState: StateFlow<WeightUiState> = _weightUiState.asStateFlow()

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
            val (start, end) = _timeRange.value.getStartAndEndTimes(LocalDate.now())
            healthDataRepository.getWeightBetween(start, end)
                .collect { records ->
                    val aggregatedRecords = aggregateRecords(records)
                    val chartData = aggregatedRecords.map {
                        ChartDataPoint(x = it.first.toEpochMilli().toFloat(), y = it.second.toFloat())
                    }
                    val weightLogs = records.map {
                        WeightLogItem(
                            weight = it.weightKilograms,
                            date = it.time.atZone(ZoneId.systemDefault())
                        )
                    }.sortedByDescending { it.date }

                    _weightUiState.value = WeightUiState(
                        chartData = chartData,
                        weightLogs = weightLogs
                    )
                }
        }
    }

    private fun aggregateRecords(records: List<Weight>): List<Pair<Instant, Double>> {
        return when (_timeRange.value) {
            TimeRange.DAY -> records.map { it.time to it.weightKilograms }
            TimeRange.WEEK, TimeRange.MONTH -> aggregateByDay(records)
            TimeRange.YEAR -> aggregateByMonth(records)
        }
    }

    private fun aggregateByDay(records: List<Weight>): List<Pair<Instant, Double>> {
        if (records.isEmpty()) return emptyList()
        return records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .map { (date, dayRecords) ->
                val average = dayRecords.map { it.weightKilograms }.average()
                date.atStartOfDay(ZoneId.systemDefault()).toInstant() to average
            }
    }

    private fun aggregateByMonth(records: List<Weight>): List<Pair<Instant, Double>> {
        if (records.isEmpty()) return emptyList()
        return records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .map { (_, monthRecords) ->
                val average = monthRecords.map { it.weightKilograms }.average()
                val firstDayOfMonth = monthRecords.first().time.atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1)
                firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant() to average
            }
    }
}
