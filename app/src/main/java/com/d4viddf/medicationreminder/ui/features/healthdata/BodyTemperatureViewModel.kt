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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import java.time.DayOfWeek

data class TemperatureChartData(
    val lineChartData: List<LineChartPoint> = emptyList(),
    val rangeChartData: List<RangeChartPoint> = emptyList(),
    val labels: List<String> = emptyList()
)

data class TemperatureUiState(
    val chartData: TemperatureChartData = TemperatureChartData(),
    val temperatureLogs: List<TemperatureLogItem> = emptyList(),
    val yAxisRange: ClosedFloatingPointRange<Float> = 36f..40f,
    val dayViewTemperature: Float? = null,
    val periodTemperatureRange: Pair<Float, Float>? = null
)

data class TemperatureLogItem(
    val temperature: Double,
    val date: ZonedDateTime,
    val sourceApp: String?
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

    private val _timeRange = MutableStateFlow(TimeRange.DAY)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _dateRangeText = MutableStateFlow<DateRangeText?>(null)
    val dateRangeText: StateFlow<DateRangeText?> = _dateRangeText.asStateFlow()

    private val _isNextEnabled = MutableStateFlow(false)
    val isNextEnabled: StateFlow<Boolean> = _isNextEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            combine(timeRange, selectedDate) { timeRange, selectedDate ->
                fetchBodyTemperatureRecords(timeRange, selectedDate)
            }.collect{}
        }
    }

    private fun fetchBodyTemperatureRecords(timeRange: TimeRange, selectedDate: LocalDate) {
        viewModelScope.launch {
            val (start, end) = timeRange.getStartAndEndTimes(selectedDate)
            healthDataRepository.getBodyTemperatureBetween(start, end)
                .combine(healthDataRepository.getLatestBodyTemperature()) { records, latestTemperature ->
                    processBodyTemperatureData(records, latestTemperature, timeRange, selectedDate)
                }.collect {
                    _temperatureUiState.value = it
                }
        }
    }

    private fun processBodyTemperatureData(records: List<BodyTemperature>, latestTemperature: BodyTemperature?, timeRange: TimeRange, selectedDate: LocalDate): TemperatureUiState {
        val temperatureLogs = records.map {
            TemperatureLogItem(
                temperature = it.temperatureCelsius,
                date = it.time.atZone(ZoneId.systemDefault()),
                sourceApp = it.sourceApp
            )
        }.sortedByDescending { it.date }

        val chartData = aggregateDataForChart(records, latestTemperature, timeRange, selectedDate)

        var dayViewTemperature: Float? = null
        var periodTemperatureRange: Pair<Float, Float>? = null

        if (records.isNotEmpty()) {
            when (timeRange) {
                TimeRange.DAY -> {
                    dayViewTemperature = records.maxByOrNull { it.time }?.temperatureCelsius?.toFloat()
                }
                else -> {
                    val minTemp = records.minOf { it.temperatureCelsius }.toFloat()
                    val maxTemp = records.maxOf { it.temperatureCelsius }.toFloat()
                    periodTemperatureRange = Pair(minTemp, maxTemp)
                }
            }
        }

        val maxTemp = records.maxOfOrNull { it.temperatureCelsius }?.toFloat() ?: (latestTemperature?.temperatureCelsius?.toFloat() ?: 0f)
        val yMax = if (maxTemp > 40f) {
            if (maxTemp % 2 == 0f) maxTemp else maxTemp.toInt() + 1f
        } else {
            40f
        }
        updateDateAndButtonStates()
        return TemperatureUiState(
            chartData = chartData,
            temperatureLogs = temperatureLogs,
            yAxisRange = 34f..yMax,
            dayViewTemperature = dayViewTemperature,
            periodTemperatureRange = periodTemperatureRange
        )
    }

    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
        _selectedDate.value = LocalDate.now()
    }

    fun onPreviousClick() {
        _selectedDate.value = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.minusDays(1)
            TimeRange.WEEK -> _selectedDate.value.minusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.minusMonths(1)
            TimeRange.YEAR -> _selectedDate.value.minusYears(1)
        }
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
        }
    }

    private fun aggregateDataForChart(records: List<BodyTemperature>, latestTemperature: BodyTemperature?, timeRange: TimeRange, selectedDate: LocalDate): TemperatureChartData {
        return when (timeRange) {
            TimeRange.DAY -> aggregateByHour(records, latestTemperature, selectedDate)
            TimeRange.WEEK -> aggregateByDayOfWeek(records, selectedDate)
            TimeRange.MONTH -> aggregateByDayOfMonth(records, selectedDate)
            TimeRange.YEAR -> aggregateByMonth(records, selectedDate)
        }
    }

    private fun aggregateByHour(records: List<BodyTemperature>, latestTemperature: BodyTemperature?, selectedDate: LocalDate): TemperatureChartData {
        val data = records
            .filter { it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate }
            .sortedBy { it.time }
            .map {
                val zonedDateTime = it.time.atZone(ZoneId.systemDefault())
                LineChartPoint(
                    x = zonedDateTime.hour.toFloat(),
                    y = it.temperatureCelsius.toFloat(),
                    label = ""
                )
            }
        val labels = (0..23).map { if (it % 4 == 0) it.toString() else "" }
        return TemperatureChartData(lineChartData = data, labels = labels)
    }

    private fun aggregateByDayOfWeek(records: List<BodyTemperature>, selectedDate: LocalDate): TemperatureChartData {
        val weekFields = java.time.temporal.WeekFields.of(Locale.getDefault())
        val startOfWeek = selectedDate.with(weekFields.dayOfWeek(), 1)

        val weekData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.minOf { it.temperatureCelsius }.toFloat() to dayRecords.maxOf { it.temperatureCelsius }.toFloat()
            }

        val dayOfWeekInitials = mapOf(
            DayOfWeek.MONDAY to "l",
            DayOfWeek.TUESDAY to "m",
            DayOfWeek.WEDNESDAY to "x",
            DayOfWeek.THURSDAY to "j",
            DayOfWeek.FRIDAY to "v",
            DayOfWeek.SATURDAY to "s",
            DayOfWeek.SUNDAY to "d"
        )
        val labels = (0..6).map {
            val day = startOfWeek.plusDays(it.toLong()).dayOfWeek
            dayOfWeekInitials[day] ?: ""
        }

        val data = (0..6).map {
            val date = startOfWeek.plusDays(it.toLong())
            val (min, max) = weekData[date] ?: (0f to 0f)
            RangeChartPoint(
                x = it.toFloat(),
                min = min,
                max = max,
                label = ""
            )
        }
        return TemperatureChartData(rangeChartData = data, labels = labels)
    }

    private fun aggregateByDayOfMonth(records: List<BodyTemperature>, selectedDate: LocalDate): TemperatureChartData {
        val startOfMonth = selectedDate.withDayOfMonth(1)
        val daysInMonth = selectedDate.lengthOfMonth()

        val monthData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.minOf { it.temperatureCelsius }.toFloat() to dayRecords.maxOf { it.temperatureCelsius }.toFloat()
            }

        val labelsToShow = listOf(1, 5, 10, 15, 20, 25, daysInMonth).toSet()

        val labels = (0 until daysInMonth).map {
            val day = it + 1
            if (day in labelsToShow) day.toString() else ""
        }

        val data = (0 until daysInMonth).map {
            val date = startOfMonth.plusDays(it.toLong())
            val (min, max) = monthData[date] ?: (0f to 0f)
            RangeChartPoint(
                x = it.toFloat(),
                min = min,
                max = max,
                label = ""
            )
        }
        return TemperatureChartData(rangeChartData = data, labels = labels)
    }

    private fun aggregateByMonth(records: List<BodyTemperature>, selectedDate: LocalDate): TemperatureChartData {
        val startOfYear = selectedDate.withDayOfYear(1)

        val yearData = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .mapValues { (_, monthRecords) ->
                monthRecords.minOf { it.temperatureCelsius }.toFloat() to monthRecords.maxOf { it.temperatureCelsius }.toFloat()
            }

        val labels = (0..11).map {
            startOfYear.plusMonths(it.toLong()).month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).first().toString()
        }

        val data = (0..11).map {
            val date = startOfYear.plusMonths(it.toLong())
            val (min, max) = yearData[date.month] ?: (0f to 0f)
            RangeChartPoint(
                x = it.toFloat(),
                min = min,
                max = max,
                label = ""
            )
        }
        return TemperatureChartData(rangeChartData = data, labels = labels)
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
