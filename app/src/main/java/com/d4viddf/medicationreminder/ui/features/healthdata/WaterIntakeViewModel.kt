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

    private val _aggregatedWaterIntakeRecords = MutableStateFlow<List<Pair<Instant, Double>>>(emptyList())
    val aggregatedWaterIntakeRecords: StateFlow<List<Pair<Instant, Double>>> = _aggregatedWaterIntakeRecords.asStateFlow()

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

    private val _waterIntakeByType = MutableStateFlow<Map<String, Int>>(emptyMap())
    val waterIntakeByType: StateFlow<Map<String, Int>> = _waterIntakeByType.asStateFlow()

    private val _isNextEnabled = MutableStateFlow(false)
    val isNextEnabled: StateFlow<Boolean> = _isNextEnabled.asStateFlow()

    private val _numberOfDaysInRange = MutableStateFlow(1)
    val numberOfDaysInRange: StateFlow<Int> = _numberOfDaysInRange.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedDate.collect {
                fetchWaterIntakeRecords()
            }
        }
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
        val newSelectedDate = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.plusDays(1)
            TimeRange.WEEK -> _selectedDate.value.plusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.plusMonths(1)
            TimeRange.YEAR -> _selectedDate.value.plusYears(1)
        }
        if (!newSelectedDate.isAfter(LocalDate.now())) {
            _selectedDate.value = newSelectedDate
        }
    }

    fun onHistoryItemClick(newTimeRange: TimeRange, newDate: LocalDate) {
        _timeRange.value = newTimeRange
        _selectedDate.value = newDate
    }

    fun fetchWaterIntakeRecords() {
        viewModelScope.launch {
            _numberOfDaysInRange.value = when (_timeRange.value) {
                TimeRange.DAY -> 1
                TimeRange.WEEK -> 7
                TimeRange.MONTH -> _selectedDate.value.lengthOfMonth()
                TimeRange.YEAR -> _selectedDate.value.lengthOfYear()
            }
            val (start, end) = _timeRange.value.getStartAndEndTimes(_selectedDate.value)
            _startTime.value = start
            _endTime.value = end
            healthDataRepository.getWaterIntakeBetween(start, end)
                .collect { records ->
                    _waterIntakeRecords.value = records
                    _totalWaterIntake.value = records.sumOf { it.volumeMilliliters }
                    _waterIntakeProgress.value = (_totalWaterIntake.value / _dailyGoal.value).toFloat().coerceIn(0f, 1f)

                    if (_timeRange.value == TimeRange.DAY) {
                        _waterIntakeByType.value = records
                            .mapNotNull { it.type }
                            .groupBy { it }
                            .mapValues { it.value.size }
                    } else {
                        _waterIntakeByType.value = emptyMap()
                    }

                    _daysGoalReached.value = records.groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
                        .mapValues { (_, records) -> records.sumOf { it.volumeMilliliters } }
                        .filter { it.value >= 4000 }
                        .count()
                    aggregateRecords(records)
                }
            updateDateRangeText()

            val nextDate = when (_timeRange.value) {
                TimeRange.DAY -> _selectedDate.value.plusDays(1)
                TimeRange.WEEK -> _selectedDate.value.plusWeeks(1)
                TimeRange.MONTH -> _selectedDate.value.plusMonths(1)
                TimeRange.YEAR -> _selectedDate.value.plusYears(1)
            }
            _isNextEnabled.value = !nextDate.isAfter(LocalDate.now())
        }
    }

    private fun aggregateRecords(records: List<WaterIntake>) {
        _aggregatedWaterIntakeRecords.value = when (_timeRange.value) {
            TimeRange.DAY -> records.map { it.time to it.volumeMilliliters }
            TimeRange.WEEK -> aggregateByDay(records)
            TimeRange.MONTH -> aggregateByWeek(records)
            TimeRange.YEAR -> aggregateByMonth(records)
        }
    }

    private fun aggregateByDay(records: List<WaterIntake>): List<Pair<Instant, Double>> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val startOfWeek = _selectedDate.value.with(weekFields.dayOfWeek(), 1)
        val endOfWeek = startOfWeek.plusDays(6)
        val weekMap = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, records) -> records.sumOf { it.volumeMilliliters } }

        val fullWeek = (0..6).map {
            val date = startOfWeek.plusDays(it.toLong())
            val value = weekMap[date] ?: 0.0
            date.atStartOfDay(ZoneId.systemDefault()).toInstant() to value
        }
        return fullWeek
    }

    private fun aggregateByWeek(records: List<WaterIntake>): List<Pair<Instant, Double>> {
        val monthMap = records
            .groupBy {
                val date = it.time.atZone(ZoneId.systemDefault()).toLocalDate()
                val weekFields = WeekFields.of(Locale.getDefault())
                date.with(weekFields.dayOfWeek(), 1)
            }
            .mapValues { (_, records) -> records.sumOf { it.volumeMilliliters } / 7 }

        val startOfMonth = _selectedDate.value.withDayOfMonth(1)
        val endOfMonth = _selectedDate.value.withDayOfMonth(_selectedDate.value.lengthOfMonth())
        val weeksInMonth = mutableListOf<LocalDate>()
        var current = startOfMonth
        while (current.isBefore(endOfMonth) || current.isEqual(endOfMonth)) {
            weeksInMonth.add(current.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1))
            current = current.plusWeeks(1)
        }

        return weeksInMonth.distinct().map { weekStart ->
            val value = monthMap[weekStart] ?: 0.0
            weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant() to value
        }
    }

    private fun aggregateByMonth(records: List<WaterIntake>): List<Pair<Instant, Double>> {
        val yearMap = records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .mapValues { (_, records) -> records.sumOf { it.volumeMilliliters } / records.map { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }.distinct().size }

        return (1..12).map {
            val month = java.time.Month.of(it)
            val value = yearMap[month] ?: 0.0
            _selectedDate.value.withMonth(it).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant() to value
        }
    }

    private fun updateDateRangeText() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        _dateRangeText.value = when (_timeRange.value) {
            TimeRange.DAY -> when (_selectedDate.value) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> _selectedDate.value.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
            }
            TimeRange.WEEK -> {
                val startOfWeek = _selectedDate.value.with(java.time.DayOfWeek.MONDAY)
                val endOfWeek = _selectedDate.value.with(java.time.DayOfWeek.SUNDAY)
                "${startOfWeek.format(DateTimeFormatter.ofPattern("d MMM"))} - ${endOfWeek.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
            }
            TimeRange.MONTH -> _selectedDate.value.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            TimeRange.YEAR -> _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    }
}
