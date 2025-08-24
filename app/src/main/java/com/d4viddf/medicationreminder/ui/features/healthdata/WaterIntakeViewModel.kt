package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.ui.features.common.charts.ChartDataPoint
import com.d4viddf.medicationreminder.ui.features.healthdata.util.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WaterIntakeViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _waterIntakeRecords = MutableStateFlow<List<WaterIntake>>(emptyList())
    val waterIntakeRecords: StateFlow<List<WaterIntake>> = _waterIntakeRecords.asStateFlow()

    private val _aggregatedWaterIntakeRecords =
        MutableStateFlow<List<Pair<Instant, Double>>>(emptyList())
    val aggregatedWaterIntakeRecords: StateFlow<List<Pair<Instant, Double>>> =
        _aggregatedWaterIntakeRecords.asStateFlow()

    private val _chartData = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val chartData: StateFlow<List<ChartDataPoint>> = _chartData.asStateFlow()

    private val _chartDateRangeLabel = MutableStateFlow("")
    val chartDateRangeLabel: StateFlow<String> = _chartDateRangeLabel.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _timeRange = MutableStateFlow(TimeRange.DAY)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    private val _dateRangeText = MutableStateFlow("")
    val dateRangeText: StateFlow<String> = _dateRangeText.asStateFlow()

    private val _startTime = MutableStateFlow(Instant.now())
    val startTime: StateFlow<Instant> = _startTime.asStateFlow()

    private val _endTime = MutableStateFlow(Instant.now())
    val endTime: StateFlow<Instant> = _endTime.asStateFlow()

    private val _totalWaterIntake = MutableStateFlow(0.0)
    val totalWaterIntake: StateFlow<Double> = _totalWaterIntake.asStateFlow()

    private val _daysGoalReached = MutableStateFlow(0)
    val daysGoalReached: StateFlow<Int> = _daysGoalReached.asStateFlow()

    private val _dailyGoal = MutableStateFlow(4000.0) // Daily goal in ml
    val dailyGoal: StateFlow<Double> = _dailyGoal.asStateFlow()

    private val _waterIntakeProgress = MutableStateFlow(0f)
    val waterIntakeProgress: StateFlow<Float> = _waterIntakeProgress.asStateFlow()

    private val _isNextEnabled = MutableStateFlow(false)
    val isNextEnabled: StateFlow<Boolean> = _isNextEnabled.asStateFlow()

    private val _numberOfDaysInRange = MutableStateFlow(1)
    val numberOfDaysInRange: StateFlow<Int> = _numberOfDaysInRange.asStateFlow()

    private val _waterIntakeByType = MutableStateFlow<Map<String?, List<WaterIntake>>>(emptyMap())
    val waterIntakeByType: StateFlow<Map<String?, List<WaterIntake>>> =
        _waterIntakeByType.asStateFlow()

    private val _yAxisMax = MutableStateFlow(5000.0)
    val yAxisMax: StateFlow<Double> = _yAxisMax.asStateFlow()

    private val _selectedBar = MutableStateFlow<Pair<Instant, Double>?>(null)
    val selectedBar: StateFlow<Pair<Instant, Double>?> = _selectedBar.asStateFlow()

    private val _selectedChartBar = MutableStateFlow<ChartDataPoint?>(null)
    val selectedChartBar: StateFlow<ChartDataPoint?> = _selectedChartBar.asStateFlow()

    private val _headerAverage = MutableStateFlow(0.0)
    val headerAverage: StateFlow<Double> = _headerAverage.asStateFlow()

    private val _headerDaysGoalReached = MutableStateFlow(0)
    val headerDaysGoalReached: StateFlow<Int> = _headerDaysGoalReached.asStateFlow()

    private val _headerTotalIntake = MutableStateFlow(0.0)
    val headerTotalIntake: StateFlow<Double> = _headerTotalIntake.asStateFlow()

    init {
        updateDateAndButtonStates()
        fetchWaterIntakeRecords()
    }

    fun onBarSelected(bar: Pair<Instant, Double>?) {
        _selectedBar.value = bar
    }

    fun onChartBarSelected(dataPoint: ChartDataPoint?) {
        _selectedChartBar.value = dataPoint
    }

    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
        _selectedDate.value = LocalDate.now()
        updateDateAndButtonStates()
        fetchWaterIntakeRecords()
    }

    fun onPreviousClick() {
        _selectedDate.value = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.minusDays(1)
            TimeRange.WEEK -> _selectedDate.value.minusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.minusMonths(1)
            TimeRange.YEAR -> _selectedDate.value.minusYears(1)
        }
        updateDateAndButtonStates()
        fetchWaterIntakeRecords()
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
            fetchWaterIntakeRecords()
        }
    }

    fun onHistoryItemClick(newTimeRange: TimeRange, newDate: LocalDate) {
        _timeRange.value = newTimeRange
        _selectedDate.value = newDate
        updateDateAndButtonStates()
        fetchWaterIntakeRecords()
    }

    private fun updateDateAndButtonStates() {
        updateDateRangeText()
        updateNextButtonState()
    }

    fun fetchWaterIntakeRecords() {
        viewModelScope.launch {
            val (start, end) = _timeRange.value.getStartAndEndTimes(_selectedDate.value)
            _startTime.value = start
            _endTime.value = end

            healthDataRepository.getWaterIntakeBetween(start, end)
                .collect { allRecordsInRange ->
                    when (_timeRange.value) {
                        TimeRange.DAY -> loadChartDataForDay(_selectedDate.value, allRecordsInRange)
                        TimeRange.WEEK -> loadChartDataForWeek(_selectedDate.value, allRecordsInRange)
                        TimeRange.MONTH -> loadChartDataForMonth(_selectedDate.value, allRecordsInRange)
                        TimeRange.YEAR -> loadChartDataForYear(_selectedDate.value, allRecordsInRange)
                        else -> {
                            _chartData.value = emptyList()
                        }
                    }

                    val (aggregatedRecords, yMax) = when (_timeRange.value) {
                        TimeRange.DAY -> {
                            val dayRecords = allRecordsInRange.filter {
                                it.time.atZone(ZoneId.systemDefault())
                                    .toLocalDate() == _selectedDate.value
                            }
                            _waterIntakeRecords.value = dayRecords
                            _totalWaterIntake.value = dayRecords.sumOf { it.volumeMilliliters }
                            _waterIntakeByType.value = dayRecords.groupBy { it.type }
                            _waterIntakeProgress.value =
                                (_totalWaterIntake.value / _dailyGoal.value).toFloat()
                                    .coerceIn(0f, 1f)
                            val aggregated = dayRecords.map { it.time to it.volumeMilliliters }
                            aggregated to (dayRecords.maxOfOrNull { it.volumeMilliliters }
                                ?: _dailyGoal.value)
                        }

                        TimeRange.WEEK -> {
                            val aggregated = aggregateByDay(allRecordsInRange)
                            aggregated to (aggregated.maxOfOrNull { it.second } ?: _dailyGoal.value)
                        }

                        TimeRange.MONTH -> {
                            val dailyIntakes = allRecordsInRange
                                .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
                                .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.volumeMilliliters } }

                            val aggregated = aggregateByWeek(allRecordsInRange)

                            val yMax = dailyIntakes.values.maxOrNull()

                            aggregated to (yMax ?: _dailyGoal.value)
                        }

                        TimeRange.YEAR -> {
                            val aggregated = aggregateByMonth(allRecordsInRange)
                            aggregated to (aggregated.maxOfOrNull { it.second } ?: _dailyGoal.value)
                        }
                    }
                    _aggregatedWaterIntakeRecords.value = aggregatedRecords
                    _yAxisMax.value = (yMax * 1.2).coerceAtLeast(_dailyGoal.value)

                    val today = LocalDate.now()
                    val recordsInPast = allRecordsInRange.filter { it.time.atZone(ZoneId.systemDefault()).toLocalDate().isBefore(today.plusDays(1)) }

                    _headerTotalIntake.value = recordsInPast.sumOf { it.volumeMilliliters }

                    val dailyIntakes = recordsInPast.groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
                        .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.volumeMilliliters } }

                    _headerDaysGoalReached.value = dailyIntakes.filter { it.value >= _dailyGoal.value }.count()

                    val startOfRange = _startTime.value.atZone(ZoneId.systemDefault()).toLocalDate()
                    val endOfRange = minOf(today, _endTime.value.atZone(ZoneId.systemDefault()).toLocalDate())
                    val daysInRange = ChronoUnit.DAYS.between(startOfRange, endOfRange).toInt() + 1

                    _headerAverage.value = if (daysInRange > 0) _headerTotalIntake.value / daysInRange else 0.0

                    _daysGoalReached.value =
                        allRecordsInRange.groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
                            .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.volumeMilliliters } }
                            .filter { it.value >= _dailyGoal.value }
                            .count()
                }
        }
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

    private fun aggregateByDay(records: List<WaterIntake>): List<Pair<Instant, Double>> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val startOfWeek = _selectedDate.value.with(weekFields.dayOfWeek(), 1)
        val today = LocalDate.now()
        val weekMap = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.volumeMilliliters } }

        return (0..6).mapNotNull {
            val date = startOfWeek.plusDays(it.toLong())
            if (date.isAfter(today)) {
                null
            } else {
                val value = weekMap[date] ?: 0.0
                date.atStartOfDay(ZoneId.systemDefault()).toInstant() to value
            }
        }.sortedBy { it.first }
    }

    private fun aggregateByWeek(records: List<WaterIntake>): List<Pair<Instant, Double>> {
        val today = LocalDate.now()
        val monthMap = records
            .groupBy {
                val date = it.time.atZone(ZoneId.systemDefault()).toLocalDate()
                val weekFields = WeekFields.of(Locale.getDefault())
                date.with(weekFields.dayOfWeek(), 1)
            }
            .mapValues { (_, weekRecords) -> weekRecords.sumOf { it.volumeMilliliters } / 7 }

        val startOfMonth = _selectedDate.value.withDayOfMonth(1)
        val endOfMonth = _selectedDate.value.withDayOfMonth(_selectedDate.value.lengthOfMonth())
        val weeksInMonth = mutableListOf<LocalDate>()
        var current = startOfMonth
        while (current.isBefore(endOfMonth) || current.isEqual(endOfMonth)) {
            weeksInMonth.add(current.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1))
            current = current.plusWeeks(1)
        }

        return weeksInMonth.distinct().mapNotNull { weekStart ->
            if (weekStart.isAfter(today)) {
                null
            } else {
                val value = monthMap[weekStart] ?: 0.0
                weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant() to value
            }
        }.sortedBy { it.first }
    }

    private fun aggregateByMonth(records: List<WaterIntake>): List<Pair<Instant, Double>> {
        val yearMap = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .mapValues { (month, monthRecords) ->
                val totalIntake = monthRecords.sumOf { it.volumeMilliliters }
                val daysInMonth = month.length(_selectedDate.value.isLeapYear)
                totalIntake.toDouble() / daysInMonth
            }

        return (1..12).map {
            val month = java.time.Month.of(it)
            val monthDate = _selectedDate.value.withMonth(it)
            val value = yearMap[month] ?: 0.0
            monthDate.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant() to value
        }.sortedBy { it.first }
    }

    private fun updateDateRangeText() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val weekFields = WeekFields.of(Locale.getDefault())
        val startOfWeek = _selectedDate.value.with(weekFields.dayOfWeek(), 1)
        val endOfWeek = startOfWeek.plusDays(6)

        _dateRangeText.value = when (_timeRange.value) {
            TimeRange.DAY -> when (_selectedDate.value) {
                today -> "today"
                yesterday -> "yesterday"
                else -> _selectedDate.value.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
            }

            TimeRange.WEEK -> {
                if (_selectedDate.value.with(weekFields.dayOfWeek(), 1) == today.with(weekFields.dayOfWeek(), 1)) {
                    "this_week"
                } else {
                    "${startOfWeek.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))} - ${endOfWeek.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}"
                }
            }

            TimeRange.MONTH -> {
                if (_selectedDate.value.month == today.month && _selectedDate.value.year == today.year) {
                    "this_month"
                } else {
                    _selectedDate.value.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
                }
            }
            TimeRange.YEAR -> _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy", Locale.getDefault()))
        }
    }

    private fun loadChartDataForWeek(selectedDate: LocalDate, allRecordsInRange: List<WaterIntake>) {
        val rawData = allRecordsInRange.groupBy {
            it.time.atZone(ZoneId.systemDefault()).toLocalDate()
        }.mapValues { entry ->
            entry.value.sumOf { it.volumeMilliliters }.toFloat()
        }

        val weekStart = selectedDate.with(DayOfWeek.MONDAY)
        val daysOfWeek = (0..6).map { weekStart.plusDays(it.toLong()) }

        val chartDataWithDate = daysOfWeek.map { date ->
            date to ChartDataPoint(
                value = rawData[date] ?: 0f, // Default to 0 if no data for that day
                label = date.dayOfWeek.getDisplayName(
                    TextStyle.NARROW,
                    Locale.getDefault()
                ).first().toString(), // L, M, X, J, V, S, D
                fullLabel = date.format(
                    DateTimeFormatter.ofPattern(
                        "EEEE, d MMM",
                        Locale.getDefault()
                    ).withLocale(Locale.getDefault())
                ), // lunes, 18 ago
                date = date
            )
        }
        _chartData.value = chartDataWithDate.map { it.second }

        _chartDateRangeLabel.value = formatWeekRange(selectedDate)
    }

    private fun formatWeekRange(dateInWeek: LocalDate): String {
        val locale = Locale.getDefault()
        val startOfWeek = dateInWeek.with(DayOfWeek.MONDAY)
        val endOfWeek = dateInWeek.with(DayOfWeek.SUNDAY)

        val startMonth = startOfWeek.format(DateTimeFormatter.ofPattern("MMM", locale))
        val endMonth = endOfWeek.format(DateTimeFormatter.ofPattern("MMM", locale))

        return if (startMonth == endMonth) {
            "${startOfWeek.dayOfMonth} - ${endOfWeek.dayOfMonth} ${endMonth.replace(".", "")}" // e.g., "11 - 17 ago"
        } else {
            "${startOfWeek.dayOfMonth} ${startMonth.replace(".", "")} - ${endOfWeek.dayOfMonth} ${endMonth.replace(".", "")}" // e.g., "28 jul - 3 ago"
        }
    }

    private fun loadChartDataForMonth(selectedDate: LocalDate, allRecordsInRange: List<WaterIntake>) {
        val monthMap = allRecordsInRange
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.volumeMilliliters }.toFloat() }

        val startOfMonth = selectedDate.withDayOfMonth(1)
        val endOfMonth = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth())
        val daysInMonth = (startOfMonth.dayOfMonth..endOfMonth.dayOfMonth).map { startOfMonth.withDayOfMonth(it) }

        val labelsToShow = listOf(1, 5, 10, 15, 20, 25, endOfMonth.dayOfMonth).toSet()

        _chartData.value = daysInMonth.map { date ->
            ChartDataPoint(
                value = monthMap[date] ?: 0f,
                label = if (date.dayOfMonth in labelsToShow) date.dayOfMonth.toString() else "",
                fullLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())),
                date = date
            )
        }
        _chartDateRangeLabel.value = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }

    private fun loadChartDataForYear(selectedDate: LocalDate, allRecordsInRange: List<WaterIntake>) {
        val yearMap = allRecordsInRange
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .mapValues { (month, monthRecords) ->
                val totalIntake = monthRecords.sumOf { it.volumeMilliliters }.toFloat()
                val daysInMonth = month.length(selectedDate.isLeapYear)
                totalIntake / daysInMonth
            }

        _chartData.value = (1..12).map {
            val month = java.time.Month.of(it)
            val monthDate = selectedDate.withMonth(it)
            ChartDataPoint(
                value = yearMap[month] ?: 0f,
                label = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                fullLabel = month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                date = monthDate
            )
        }
        _chartDateRangeLabel.value = selectedDate.format(DateTimeFormatter.ofPattern("yyyy", Locale.getDefault()))
    }

    private fun loadChartDataForDay(selectedDate: LocalDate, allRecordsInRange: List<WaterIntake>) {
        val dayRecords = allRecordsInRange.filter {
            it.time.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
        }

        val hourlyData = dayRecords
            .groupBy { it.time.atZone(ZoneId.systemDefault()).hour }
            .mapValues { (_, hourRecords) -> hourRecords.sumOf { it.volumeMilliliters }.toFloat() }

        _chartData.value = (0..23).map { hour ->
            val hourDate = selectedDate.atTime(hour, 0)
            ChartDataPoint(
                value = hourlyData[hour] ?: 0f,
                label = if (hour % 2 == 0) "$hour" else "",
                fullLabel = "$hour:00",
                date = selectedDate
            )
        }
    }
}