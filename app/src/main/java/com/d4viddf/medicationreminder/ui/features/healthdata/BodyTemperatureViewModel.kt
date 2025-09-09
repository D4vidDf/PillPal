package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.ui.features.healthdata.component.LineChartPoint
import com.d4viddf.medicationreminder.ui.features.healthdata.component.RangeChartPoint
import com.d4viddf.medicationreminder.ui.features.healthdata.util.DateRangeText
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class TemperatureChartData(
    val lineChartData: List<LineChartPoint> = emptyList(),
    val rangeChartData: List<RangeChartPoint> = emptyList()
)

data class TemperatureUiState(
    val chartData: TemperatureChartData = TemperatureChartData(),
    val temperatureLogs: List<TemperatureLogItem> = emptyList(),
    val yAxisRange: ClosedFloatingPointRange<Float> = 36f..40f
)

data class TemperatureLogItem(
    val temperature: Double,
    val date: ZonedDateTime
)

@HiltViewModel
class BodyTemperatureViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _selectedBar = MutableStateFlow<RangeChartPoint?>(null)
    val selectedBar: StateFlow<RangeChartPoint?> = _selectedBar.asStateFlow()

    fun onBarSelected(bar: RangeChartPoint?) {
        _selectedBar.value = bar
    }

    private val _temperatureUiState = MutableStateFlow(TemperatureUiState())
    val temperatureUiState: StateFlow<TemperatureUiState> = _temperatureUiState.asStateFlow()

    private val _timeRange = MutableStateFlow(TimeRange.WEEK)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _dateRangeText = MutableStateFlow<DateRangeText?>(null)
    val dateRangeText: StateFlow<DateRangeText?> = _dateRangeText.asStateFlow()

    private val _isNextEnabled = MutableStateFlow(false)
    val isNextEnabled: StateFlow<Boolean> = _isNextEnabled.asStateFlow()

    init {
        updateDateAndButtonStates()
        fetchBodyTemperatureRecords()
    }

    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
        _selectedDate.value = LocalDate.now()
        updateDateAndButtonStates()
        fetchBodyTemperatureRecords()
    }

    fun onPreviousClick() {
        _selectedDate.value = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.minusDays(1)
            TimeRange.WEEK -> _selectedDate.value.minusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.minusMonths(1)
            TimeRange.YEAR -> _selectedDate.value.minusYears(1)
        }
        updateDateAndButtonStates()
        fetchBodyTemperatureRecords()
    }

    fun onNextClick() {
        val nextDate = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.plusDays(1)
            TimeRange.WEEK -> _selectedDate.value.plusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.plusMonths(1)
            TimeRange.YEAR -> _selectedDate.value.plusYears(1)
        }
        if (!nextDate.isAfter(LocalDate.now())) {
            _selectedDate.value = nextDate
            updateDateAndButtonStates()
            fetchBodyTemperatureRecords()
        }
    }

    private fun fetchBodyTemperatureRecords() {
        viewModelScope.launch {
            val (start, end) = _timeRange.value.getStartAndEndTimes(_selectedDate.value)
            healthDataRepository.getBodyTemperatureBetween(start, end)
                .collect { records ->
                    val chartData = if (_timeRange.value == TimeRange.DAY) {
                        TemperatureChartData(lineChartData = aggregateForLineChart(records))
                    } else {
                        TemperatureChartData(rangeChartData = aggregateForRangeChart(records))
                    }

                    val temperatureLogs = records.map {
                        TemperatureLogItem(
                            temperature = it.temperatureCelsius,
                            date = it.time.atZone(ZoneId.systemDefault())
                        )
                    }.sortedByDescending { it.date }

                    val minTemp = records.minOfOrNull { it.temperatureCelsius }?.toFloat() ?: 36f
                    val maxTemp = records.maxOfOrNull { it.temperatureCelsius }?.toFloat() ?: 40f

                    val yMin = minOf(36f, minTemp)
                    val yMax = maxOf(40f, maxTemp)

                    _temperatureUiState.value = TemperatureUiState(
                        chartData = chartData,
                        temperatureLogs = temperatureLogs,
                        yAxisRange = yMin..yMax
                    )
                }
        }
    }

    private fun aggregateForLineChart(records: List<BodyTemperature>): List<LineChartPoint> {
        return records.map {
            val zonedDateTime = it.time.atZone(ZoneId.systemDefault())
            LineChartPoint(
                x = zonedDateTime.hour.toFloat(),
                y = it.temperatureCelsius.toFloat(),
                label = zonedDateTime.hour.toString()
            )
        }
    }

    private fun aggregateForRangeChart(records: List<BodyTemperature>): List<RangeChartPoint> {
        return when (_timeRange.value) {
            TimeRange.WEEK -> aggregateByDayOfWeek(records)
            TimeRange.MONTH -> aggregateByDayOfMonth(records)
            TimeRange.YEAR -> aggregateByMonth(records)
            else -> emptyList()
        }
    }

    private fun aggregateByDayOfWeek(records: List<BodyTemperature>): List<RangeChartPoint> {
        val weekFields = java.time.temporal.WeekFields.of(Locale.getDefault())
        val startOfWeek = _selectedDate.value.with(weekFields.dayOfWeek(), 1)

        val weekData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.minOf { it.temperatureCelsius }.toFloat() to dayRecords.maxOf { it.temperatureCelsius }.toFloat()
            }

        return (0..6).map {
            val date = startOfWeek.plusDays(it.toLong())
            val (min, max) = weekData[date] ?: (0f to 0f)
            RangeChartPoint(
                x = date.dayOfWeek.value.toFloat(),
                min = min,
                max = max,
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            )
        }
    }

    private fun aggregateByDayOfMonth(records: List<BodyTemperature>): List<RangeChartPoint> {
        val startOfMonth = _selectedDate.value.withDayOfMonth(1)
        val daysInMonth = _selectedDate.value.lengthOfMonth()

        val monthData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.minOf { it.temperatureCelsius }.toFloat() to dayRecords.maxOf { it.temperatureCelsius }.toFloat()
            }

        return (0 until daysInMonth).map {
            val date = startOfMonth.plusDays(it.toLong())
            val (min, max) = monthData[date] ?: (0f to 0f)
            RangeChartPoint(
                x = date.dayOfMonth.toFloat(),
                min = min,
                max = max,
                label = date.dayOfMonth.toString()
            )
        }
    }

    private fun aggregateByMonth(records: List<BodyTemperature>): List<RangeChartPoint> {
        val startOfYear = _selectedDate.value.withDayOfYear(1)

        val yearData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .mapValues { (_, monthRecords) ->
                monthRecords.minOf { it.temperatureCelsius }.toFloat() to monthRecords.maxOf { it.temperatureCelsius }.toFloat()
            }

        return (0..11).map {
            val date = startOfYear.plusMonths(it.toLong())
            val (min, max) = yearData[date.month] ?: (0f to 0f)
            RangeChartPoint(
                x = date.month.value.toFloat(),
                min = min,
                max = max,
                label = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            )
        }
    }

    private fun updateDateAndButtonStates() {
        updateDateRangeText()
        updateNextButtonState()
    }

    private fun updateNextButtonState() {
        val nextDate = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.plusDays(1)
            TimeRange.WEEK -> _selectedDate.value.plusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.plusMonths(1)
            TimeRange.YEAR -> _selectedDate.value.plusYears(1)
        }
        _isNextEnabled.value = !nextDate.isAfter(LocalDate.now())
    }

    private fun updateDateRangeText() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val weekFields = java.time.temporal.WeekFields.of(Locale.getDefault())

        _dateRangeText.value = when (_timeRange.value) {
            TimeRange.DAY -> when (_selectedDate.value) {
                today -> DateRangeText.StringResource(R.string.today)
                yesterday -> DateRangeText.StringResource(R.string.yesterday)
                else -> DateRangeText.FormattedString(_selectedDate.value.format(DateTimeFormatter.ofPattern("d MMMM yyyy")))
            }
            TimeRange.WEEK -> {
                if (_selectedDate.value.with(weekFields.dayOfWeek(), 1) == today.with(weekFields.dayOfWeek(), 1)) {
                    DateRangeText.StringResource(R.string.this_week)
                } else {
                    val startOfWeek = _selectedDate.value.with(java.time.DayOfWeek.MONDAY)
                    val endOfWeek = _selectedDate.value.with(java.time.DayOfWeek.SUNDAY)
                    val formattedString = "${startOfWeek.format(DateTimeFormatter.ofPattern("d MMM"))} - ${endOfWeek.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
                    DateRangeText.FormattedString(formattedString)
                }
            }
            TimeRange.MONTH -> {
                if (_selectedDate.value.month == today.month && _selectedDate.value.year == today.year) {
                    DateRangeText.StringResource(R.string.this_month)
                } else {
                    DateRangeText.FormattedString(_selectedDate.value.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
                }
            }
            TimeRange.YEAR -> DateRangeText.FormattedString(_selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy")))
        }
    }
}
