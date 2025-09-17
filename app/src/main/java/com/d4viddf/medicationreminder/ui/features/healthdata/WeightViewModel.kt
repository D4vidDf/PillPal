package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.healthdata.component.LineChartPoint
import com.d4viddf.medicationreminder.ui.features.healthdata.util.DateRangeText
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val weightProgress: Float = 0f,
    val lastWeightLog: WeightLogItem? = null
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
        fetchWeightRecords()
    }

    private fun fetchWeightRecords() {
        viewModelScope.launch {
            val (start, end) = _timeRange.value.getStartAndEndTimes(_selectedDate.value)
            healthDataRepository.getWeightBetween(start, end)
                .combine(healthDataRepository.getLatestWeight()) { records, latestWeight ->
                    processWeightData(records, latestWeight, _timeRange.value, _selectedDate.value)
                }.collect {
                    _weightUiState.value = it
                }
        }
    }

    private fun processWeightData(records: List<Weight>, latestWeight: Weight?, timeRange: TimeRange, selectedDate: LocalDate): WeightUiState {
        val weightLogs = records.map {
            WeightLogItem(
                weight = it.weightKilograms,
                date = it.time.atZone(ZoneId.systemDefault())
            )
        }.sortedByDescending { it.date }

        val chartData = aggregateDataForChart(records, latestWeight, timeRange, selectedDate)
        val maxWeight = records.maxOfOrNull { it.weightKilograms }?.toFloat() ?: (latestWeight?.weightKilograms?.toFloat() ?: 0f)
        val yMax = if (maxWeight > 100f) (maxWeight + 10) else 100f

        val currentWeight = records.maxByOrNull { it.time }?.weightKilograms?.toFloat() ?: 0f
        val progress = if (weightGoal.value > 0) currentWeight / weightGoal.value else 0f
        _averageWeight.value = if (records.isNotEmpty()) records.map { it.weightKilograms }.average().toFloat() else (latestWeight?.weightKilograms?.toFloat() ?: 0f)

        val lastWeightLog = latestWeight?.let {
            WeightLogItem(
                weight = it.weightKilograms,
                date = it.time.atZone(ZoneId.systemDefault())
            )
        }
        updateDateAndButtonStates()
        return WeightUiState(
            chartData = chartData,
            weightLogs = weightLogs,
            yAxisRange = 0f..yMax,
            currentWeight = currentWeight,
            weightProgress = progress,
            lastWeightLog = lastWeightLog
        )
    }

    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
        _selectedDate.value = LocalDate.now()
        fetchWeightRecords()
    }

    fun onPreviousClick() {
        _selectedDate.value = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.minusDays(1)
            TimeRange.WEEK -> _selectedDate.value.minusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.minusMonths(1)
            TimeRange.YEAR -> _selectedDate.value.minusYears(1)
        }
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
            fetchWeightRecords()
        }
    }

    private fun aggregateDataForChart(records: List<Weight>, latestWeight: Weight?, timeRange: TimeRange, selectedDate: LocalDate): WeightChartData {
        if (records.isEmpty() && latestWeight != null) {
            val lastWeight = latestWeight.weightKilograms.toFloat()
            return when (timeRange) {
                TimeRange.WEEK -> {
                    val weekFields = java.time.temporal.WeekFields.of(Locale.getDefault())
                    val startOfWeek = selectedDate.with(weekFields.dayOfWeek(), 1)
                    val labels = (0..6).map {
                        startOfWeek.plusDays(it.toLong()).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).first().toString()
                    }
                    val data = (0..6).map {
                        val date = startOfWeek.plusDays(it.toLong())
                        if (date.isAfter(LocalDate.now())) {
                            LineChartPoint(x = it.toFloat(), y = -1f, label = "", showPoint = false)
                        } else {
                            LineChartPoint(x = it.toFloat(), y = lastWeight, label = "", showPoint = false)
                        }
                    }
                    WeightChartData(lineChartData = data, labels = labels)
                }
                TimeRange.MONTH -> {
                    val startOfMonth = selectedDate.withDayOfMonth(1)
                    val daysInMonth = selectedDate.lengthOfMonth()
                    val labelsToShow = listOf(1, 5, 10, 15, 20, 25, daysInMonth).toSet()
                    val labels = (0 until daysInMonth).map {
                        val day = it + 1
                        if (day in labelsToShow) day.toString() else ""
                    }
                    val data = (0 until daysInMonth).map {
                        val date = startOfMonth.plusDays(it.toLong())
                        if (date.isAfter(LocalDate.now())) {
                            LineChartPoint(x = it.toFloat(), y = -1f, label = "", showPoint = false)
                        } else {
                            LineChartPoint(x = it.toFloat(), y = lastWeight, label = "", showPoint = false)
                        }
                    }
                    WeightChartData(lineChartData = data, labels = labels)
                }
                else -> WeightChartData()
            }
        }
        return when (timeRange) {
            TimeRange.DAY -> aggregateByHour(records, selectedDate)
            TimeRange.WEEK -> aggregateByDayOfWeek(records, selectedDate, latestWeight)
            TimeRange.MONTH -> aggregateByDayOfMonth(records, selectedDate, latestWeight)
            TimeRange.YEAR -> aggregateByMonth(records, selectedDate, latestWeight)
        }
    }

    private fun aggregateByHour(records: List<Weight>, selectedDate: LocalDate): WeightChartData {
        val data = records
            .filter { it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate }
            .sortedBy { it.time }
            .map {
                val zonedDateTime = it.time.atZone(ZoneId.systemDefault())
                LineChartPoint(
                    x = zonedDateTime.hour.toFloat(),
                    y = it.weightKilograms.toFloat(),
                    label = ""
                )
            }
        val labels = (0..23).map { it.toString() }
        return WeightChartData(lineChartData = data, labels = labels)
    }

    private fun aggregateByDayOfWeek(records: List<Weight>, selectedDate: LocalDate, latestWeight: Weight?): WeightChartData {
        val weekFields = java.time.temporal.WeekFields.of(Locale.getDefault())
        val startOfWeek = selectedDate.with(weekFields.dayOfWeek(), 1)

        val weekData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.map { it.weightKilograms }.average().toFloat()
            }

        val labels = (0..6).map {
            startOfWeek.plusDays(it.toLong()).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).first().toString()
        }

        var lastKnownWeight = latestWeight?.weightKilograms?.toFloat() ?: 0f
        val data = (0..6).map {
            val date = startOfWeek.plusDays(it.toLong())
            val avg = weekData[date]
            if (avg != null) {
                lastKnownWeight = avg
                LineChartPoint(x = it.toFloat(), y = avg, label = "")
            } else {
                LineChartPoint(x = it.toFloat(), y = lastKnownWeight, label = "", showPoint = false)
            }
        }
        return WeightChartData(lineChartData = data, labels = labels)
    }

    private fun aggregateByDayOfMonth(records: List<Weight>, selectedDate: LocalDate, latestWeight: Weight?): WeightChartData {
        val startOfMonth = selectedDate.withDayOfMonth(1)
        val daysInMonth = selectedDate.lengthOfMonth()

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

        var lastKnownWeight = latestWeight?.weightKilograms?.toFloat() ?: 0f
        val data = (0 until daysInMonth).map {
            val date = startOfMonth.plusDays(it.toLong())
            val avg = monthData[date]
            if (avg != null) {
                lastKnownWeight = avg
                LineChartPoint(x = it.toFloat(), y = avg, label = "")
            } else {
                LineChartPoint(x = it.toFloat(), y = lastKnownWeight, label = "", showPoint = false)
            }
        }
        return WeightChartData(lineChartData = data, labels = labels)
    }

    private fun aggregateByMonth(records: List<Weight>, selectedDate: LocalDate, latestWeight: Weight?): WeightChartData {
        val startOfYear = selectedDate.withDayOfYear(1)

        val yearData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .mapValues { (_, monthRecords) ->
                monthRecords.map { it.weightKilograms }.average().toFloat()
            }

        val labels = (0..11).map {
            startOfYear.plusMonths(it.toLong()).month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).first().toString()
        }

        var lastKnownWeight = latestWeight?.weightKilograms?.toFloat() ?: 0f
        val data = (0..11).map {
            val date = startOfYear.plusMonths(it.toLong())
            val avg = yearData[date.month]
            if (avg != null) {
                lastKnownWeight = avg
                LineChartPoint(x = it.toFloat(), y = avg, label = "")
            } else {
                LineChartPoint(x = it.toFloat(), y = lastKnownWeight, label = "", showPoint = false)
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
