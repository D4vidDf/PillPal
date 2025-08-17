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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

    init {
        updateDateAndButtonStates()
        fetchWaterIntakeRecords()
    }

    fun onBarSelected(bar: Pair<Instant, Double>?) {
        _selectedBar.value = bar
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
                            val aggregated = aggregateByWeek(allRecordsInRange)
                            aggregated to (aggregated.maxOfOrNull { it.second } ?: _dailyGoal.value)
                        }

                        TimeRange.YEAR -> {
                            val aggregated = aggregateByMonth(allRecordsInRange)
                            aggregated to (aggregated.maxOfOrNull { it.second } ?: _dailyGoal.value)
                        }
                    }
                    _aggregatedWaterIntakeRecords.value = aggregatedRecords
                    _yAxisMax.value = yMax * 1.2

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
        val today = LocalDate.now()
        val yearMap = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .mapValues { (_, monthRecords) ->
                monthRecords.sumOf { it.volumeMilliliters } / monthRecords.map {
                    it.time.atZone(
                        ZoneId.systemDefault()
                    ).toLocalDate()
                }.distinct().size.coerceAtLeast(1)
            }

        return (1..12).mapNotNull {
            val month = java.time.Month.of(it)
            val monthDate = _selectedDate.value.withMonth(it)
            if (monthDate.isAfter(today.withDayOfMonth(1))) {
                null
            } else {
                val value = yearMap[month] ?: 0.0
                monthDate.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault())
                    .toInstant() to value
            }
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
                today -> "Today"
                yesterday -> "Yesterday"
                else -> _selectedDate.value.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
            }

            TimeRange.WEEK -> {
                if (_selectedDate.value.with(weekFields.dayOfWeek(), 1) == today.with(weekFields.dayOfWeek(), 1)) {
                    "This Week"
                } else {
                    "${startOfWeek.format(DateTimeFormatter.ofPattern("d MMM"))} - ${endOfWeek.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
                }
            }

            TimeRange.MONTH -> {
                if (_selectedDate.value.month == today.month && _selectedDate.value.year == today.year) {
                    "This Month"
                } else {
                    _selectedDate.value.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                }
            }
            TimeRange.YEAR -> _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    }
}