package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.ui.features.healthdata.component.LineChartPoint
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

data class TemperatureUiState(
    val chartData: List<LineChartPoint> = emptyList(),
    val temperatureLogs: List<TemperatureLogItem> = emptyList()
)

data class TemperatureLogItem(
    val temperature: Double,
    val date: ZonedDateTime
)

@HiltViewModel
class BodyTemperatureViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _temperatureUiState = MutableStateFlow(TemperatureUiState())
    val temperatureUiState: StateFlow<TemperatureUiState> = _temperatureUiState.asStateFlow()

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
            val (start, end) = _timeRange.value.getStartAndEndTimes(LocalDate.now())
            healthDataRepository.getBodyTemperatureBetween(start, end)
                .collect { records ->
                    val aggregatedRecords = aggregateRecords(records)
                    val chartData = aggregatedRecords.map {
                        LineChartPoint(x = it.first.toEpochMilli().toFloat(), y = it.second.toFloat())
                    }
                    val temperatureLogs = records.map {
                        TemperatureLogItem(
                            temperature = it.temperatureCelsius,
                            date = it.time.atZone(ZoneId.systemDefault())
                        )
                    }.sortedByDescending { it.date }

                    _temperatureUiState.value = TemperatureUiState(
                        chartData = chartData,
                        temperatureLogs = temperatureLogs
                    )
                }
        }
    }

    private fun aggregateRecords(records: List<BodyTemperature>): List<Pair<Instant, Double>> {
        return when (_timeRange.value) {
            TimeRange.DAY -> records.map { it.time to it.temperatureCelsius }
            TimeRange.WEEK, TimeRange.MONTH -> aggregateByDay(records)
            TimeRange.YEAR -> aggregateByMonth(records)
        }
    }

    private fun aggregateByDay(records: List<BodyTemperature>): List<Pair<Instant, Double>> {
        if (records.isEmpty()) return emptyList()
        return records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .map { (date, dayRecords) ->
                val average = dayRecords.map { it.temperatureCelsius }.average()
                date.atStartOfDay(ZoneId.systemDefault()).toInstant() to average
            }
    }

    private fun aggregateByMonth(records: List<BodyTemperature>): List<Pair<Instant, Double>> {
        if (records.isEmpty()) return emptyList()
        return records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .map { (_, monthRecords) ->
                val average = monthRecords.map { it.temperatureCelsius }.average()
                val firstDayOfMonth = monthRecords.first().time.atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1)
                firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant() to average
            }
    }
}
