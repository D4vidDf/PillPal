package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
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
    val labels: List<String> = emptyList()
)

data class WeightUiState(
    val chartData: WeightChartData = WeightChartData(),
    val weightLogs: List<WeightLogItem> = emptyList(),
    val yAxisRange: ClosedFloatingPointRange<Float> = 0f..100f,
    val currentWeight: Float = 0f,
    val weightProgress: Float = 0f
)

data class WeightLogItem(
    val weight: Double,
    val date: ZonedDateTime
)

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _weightGoal = MutableStateFlow(0f)
    val weightGoal: StateFlow<Float> = _weightGoal.asStateFlow()

    private val _averageWeight = MutableStateFlow(0f)
    val averageWeight: StateFlow<Float> = _averageWeight.asStateFlow()

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
        viewModelScope.launch {
            userPreferencesRepository.weightGoalValueFlow.collect { goal ->
                _weightGoal.value = goal
            }
        }
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
                    val chartData = aggregateDataForChart(records)

                    val weightLogs = records.map {
                        WeightLogItem(
                            weight = it.weightKilograms,
                            date = it.time.atZone(ZoneId.systemDefault())
                        )
                    }.sortedByDescending { it.date }

                    val maxWeight = records.maxOfOrNull { it.weightKilograms }?.toFloat() ?: 0f
                    val yMax = if (maxWeight > 100f) (maxWeight + 10) else 100f

                    val currentWeight = records.maxByOrNull { it.time }?.weightKilograms?.toFloat() ?: 0f
                    val progress = if (weightGoal.value > 0) currentWeight / weightGoal.value else 0f

                    _averageWeight.value = if (records.isNotEmpty()) records.map { it.weightKilograms }.average().toFloat() else 0f

                    _weightUiState.value = WeightUiState(
                        chartData = chartData,
                        weightLogs = weightLogs,
                        yAxisRange = 0f..yMax,
                        currentWeight = currentWeight,
                        weightProgress = progress
                    )
                }
        }
    }

    private fun aggregateDataForChart(records: List<Weight>): WeightChartData {
        return when (_timeRange.value) {
            TimeRange.DAY -> aggregateByHour(records)
            TimeRange.WEEK -> aggregateByDayOfWeek(records)
            TimeRange.MONTH -> aggregateByDayOfMonth(records)
            TimeRange.YEAR -> aggregateByMonth(records)
        }
    }

    private fun aggregateByHour(records: List<Weight>): WeightChartData {
        val data = records
            .sortedBy { it.time }
            .map {
                val zonedDateTime = it.time.atZone(ZoneId.systemDefault())
                LineChartPoint(
                    x = zonedDateTime.hour.toFloat(),
                    y = it.weightKilograms.toFloat(),
                    label = "" // Labels are now separate
                )
            }
        val labels = (0..23).map { it.toString() }
        return WeightChartData(lineChartData = data, labels = labels)
    }

    private fun aggregateByDayOfWeek(records: List<Weight>): WeightChartData {
        val weekFields = java.time.temporal.WeekFields.of(Locale.getDefault())
        val startOfWeek = _selectedDate.value.with(weekFields.dayOfWeek(), 1)

        val lastRecordDate = records.maxOfOrNull { it.time }?.atZone(ZoneId.systemDefault())?.toLocalDate()

        val weekData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.map { it.weightKilograms }.average().toFloat()
            }

        val labels = (0..6).map {
            startOfWeek.plusDays(it.toLong()).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).first().toString()
        }

        val data = (0..6).mapNotNull {
            val date = startOfWeek.plusDays(it.toLong())
            if (lastRecordDate != null && date.isAfter(lastRecordDate)) {
                null
            } else {
                val avg = weekData[date] ?: 0f
                LineChartPoint(
                    x = date.dayOfWeek.value.toFloat(),
                    y = avg,
                    label = ""
                )
            }
        }
        return WeightChartData(lineChartData = data, labels = labels)
    }

    private fun aggregateByDayOfMonth(records: List<Weight>): WeightChartData {
        val startOfMonth = _selectedDate.value.withDayOfMonth(1)
        val daysInMonth = _selectedDate.value.lengthOfMonth()

        val lastRecordDate = records.maxOfOrNull { it.time }?.atZone(ZoneId.systemDefault())?.toLocalDate()

        val monthData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.map { it.weightKilograms }.average().toFloat()
            }

        val labelsToShow = listOf(1, 5, 10, 15, 20, 25, daysInMonth).toSet()

        val labels = (0 until daysInMonth).map {
            val day = it + 1
            if (day in labelsToShow) day.toString() else ""
        }

        val data = (0 until daysInMonth).mapNotNull {
            val date = startOfMonth.plusDays(it.toLong())
            if (lastRecordDate != null && date.isAfter(lastRecordDate)) {
                null
            } else {
                val avg = monthData[date] ?: 0f
                LineChartPoint(
                    x = date.dayOfMonth.toFloat(),
                    y = avg,
                    label = ""
                )
            }
        }
        return WeightChartData(lineChartData = data, labels = labels)
    }

    private fun aggregateByMonth(records: List<Weight>): WeightChartData {
        val startOfYear = _selectedDate.value.withDayOfYear(1)

        val lastRecordDate = records.maxOfOrNull { it.time }?.atZone(ZoneId.systemDefault())?.toLocalDate()

        val yearData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .mapValues { (_, monthRecords) ->
                monthRecords.map { it.weightKilograms }.average().toFloat()
            }

        val labels = (0..11).map {
            startOfYear.plusMonths(it.toLong()).month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).first().toString()
        }

        val data = (0..11).mapNotNull {
            val date = startOfYear.plusMonths(it.toLong())
            if (lastRecordDate != null && date.withDayOfMonth(1).isAfter(lastRecordDate.withDayOfMonth(1))) {
                null
            } else {
                val avg = yearData[date.month] ?: 0f
                LineChartPoint(
                    x = date.month.value.toFloat(),
                    y = avg,
                    label = ""
                )
            }
        }
        return WeightChartData(lineChartData = data, labels = labels)
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
