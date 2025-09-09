package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.R
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

data class WeightChartData(
    val lineChartData: List<LineChartPoint> = emptyList(),
    val rangeChartData: List<RangeChartPoint> = emptyList()
)

data class WeightUiState(
    val chartData: WeightChartData = WeightChartData(),
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

    private val _selectedBar = MutableStateFlow<RangeChartPoint?>(null)
    val selectedBar: StateFlow<RangeChartPoint?> = _selectedBar.asStateFlow()

    fun onBarSelected(bar: RangeChartPoint?) {
        _selectedBar.value = bar
    }

    private val _weightUiState = MutableStateFlow(WeightUiState())
    val weightUiState: StateFlow<WeightUiState> = _weightUiState.asStateFlow()

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
        fetchWeightRecords()
    }

    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
        _selectedDate.value = LocalDate.now()
        updateDateAndButtonStates()
        fetchWeightRecords()
    }

    fun onPreviousClick() {
        _selectedDate.value = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.minusDays(1)
            TimeRange.WEEK -> _selectedDate.value.minusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.minusMonths(1)
            TimeRange.YEAR -> _selectedDate.value.minusYears(1)
        }
        updateDateAndButtonStates()
        fetchWeightRecords()
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
            fetchWeightRecords()
        }
    }

    private fun fetchWeightRecords() {
        viewModelScope.launch {
            val (start, end) = _timeRange.value.getStartAndEndTimes(_selectedDate.value)
            healthDataRepository.getWeightBetween(start, end)
                .collect { records ->
                    val chartData = if (_timeRange.value == TimeRange.DAY) {
                        WeightChartData(lineChartData = aggregateForLineChart(records))
                    } else {
                        WeightChartData(rangeChartData = aggregateForRangeChart(records))
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

    private fun aggregateForLineChart(records: List<Weight>): List<LineChartPoint> {
        return records.map {
            val zonedDateTime = it.time.atZone(ZoneId.systemDefault())
            LineChartPoint(
                x = zonedDateTime.hour.toFloat(),
                y = it.weightKilograms.toFloat(),
                label = zonedDateTime.hour.toString()
            )
        }
    }

    private fun aggregateForRangeChart(records: List<Weight>): List<RangeChartPoint> {
        return when (_timeRange.value) {
            TimeRange.WEEK -> aggregateByDayOfWeek(records)
            TimeRange.MONTH -> aggregateByDayOfMonth(records)
            TimeRange.YEAR -> aggregateByMonth(records)
            else -> emptyList()
        }
    }

    private fun aggregateByDayOfWeek(records: List<Weight>): List<RangeChartPoint> {
        val today = LocalDate.now()
        val weekFields = java.time.temporal.WeekFields.of(Locale.getDefault())
        val startOfWeek = _selectedDate.value.with(weekFields.dayOfWeek(), 1)

        val weekData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.minOf { it.weightKilograms }.toFloat() to dayRecords.maxOf { it.weightKilograms }.toFloat()
            }

        return (0..6).mapNotNull {
            val date = startOfWeek.plusDays(it.toLong())
            if (date.isAfter(today)) {
                null
            } else {
                val (min, max) = weekData[date] ?: (0f to 0f)
                RangeChartPoint(
                    x = date.dayOfWeek.value.toFloat(),
                    min = min,
                    max = max,
                    label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                )
            }
        }
    }

    private fun aggregateByDayOfMonth(records: List<Weight>): List<RangeChartPoint> {
        val today = LocalDate.now()
        val startOfMonth = _selectedDate.value.withDayOfMonth(1)
        val daysInMonth = _selectedDate.value.lengthOfMonth()

        val monthData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.minOf { it.weightKilograms }.toFloat() to dayRecords.maxOf { it.weightKilograms }.toFloat()
            }

        val labelsToShow = listOf(1, 5, 10, 15, 20, 25, daysInMonth).toSet()

        return (0 until daysInMonth).mapNotNull {
            val date = startOfMonth.plusDays(it.toLong())
            if (date.isAfter(today)) {
                null
            } else {
                val (min, max) = monthData[date] ?: (0f to 0f)
                RangeChartPoint(
                    x = date.dayOfMonth.toFloat(),
                    min = min,
                    max = max,
                    label = if (date.dayOfMonth in labelsToShow) date.dayOfMonth.toString() else ""
                )
            }
        }
    }

    private fun aggregateByMonth(records: List<Weight>): List<RangeChartPoint> {
        val today = LocalDate.now()
        val startOfYear = _selectedDate.value.withDayOfYear(1)

        val yearData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .mapValues { (_, monthRecords) ->
                monthRecords.minOf { it.weightKilograms }.toFloat() to monthRecords.maxOf { it.weightKilograms }.toFloat()
            }

        return (0..11).mapNotNull {
            val date = startOfYear.plusMonths(it.toLong())
            if (date.isAfter(today)) {
                null
            } else {
                val (min, max) = yearData[date.month] ?: (0f to 0f)
                RangeChartPoint(
                    x = date.month.value.toFloat(),
                    min = min,
                    max = max,
                    label = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                )
            }
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
