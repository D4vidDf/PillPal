package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
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
class WeightViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _weightRecords = MutableStateFlow<List<Weight>>(emptyList())
    val weightRecords: StateFlow<List<Weight>> = _weightRecords.asStateFlow()

    private val _aggregatedWeightRecords = MutableStateFlow<List<Pair<Instant, Double>>>(emptyList())
    val aggregatedWeightRecords: StateFlow<List<Pair<Instant, Double>>> = _aggregatedWeightRecords.asStateFlow()

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

    init {
        fetchWeightRecords()
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
            TimeRange.THREE_MONTHS -> _selectedDate.value.minusMonths(3)
            TimeRange.YEAR -> _selectedDate.value.minusYears(1)
        }
        fetchWeightRecords()
    }

    fun onNextClick() {
        val nextDate = when (_timeRange.value) {
            TimeRange.DAY -> _selectedDate.value.plusDays(1)
            TimeRange.WEEK -> _selectedDate.value.plusWeeks(1)
            TimeRange.MONTH -> _selectedDate.value.plusMonths(1)
            TimeRange.THREE_MONTHS -> _selectedDate.value.plusMonths(3)
            TimeRange.YEAR -> _selectedDate.value.plusYears(1)
        }
        if (nextDate.isAfter(LocalDate.now())) return
        _selectedDate.value = nextDate
        fetchWeightRecords()
    }

    fun onHistoryItemClick(newTimeRange: TimeRange, newDate: LocalDate) {
        _timeRange.value = newTimeRange
        _selectedDate.value = newDate
        fetchWeightRecords()
    }

    private fun fetchWeightRecords() {
        viewModelScope.launch {
            val (start, end) = _timeRange.value.getStartAndEndTimes(_selectedDate.value)
            _startTime.value = start
            _endTime.value = end
            healthDataRepository.getWeightBetween(start, end)
                .collect { records ->
                    _weightRecords.value = records
                    aggregateRecords(records)
                }
            updateDateRangeText()
        }
    }

    private fun aggregateRecords(records: List<Weight>) {
        _aggregatedWeightRecords.value = when (_timeRange.value) {
            TimeRange.DAY -> records.map { it.time to it.weightKilograms }
            TimeRange.WEEK -> aggregateByDay(records)
            TimeRange.MONTH -> aggregateByWeek(records)
            TimeRange.THREE_MONTHS -> aggregateByMonth(records)
            TimeRange.YEAR -> aggregateByMonth(records)
        }
    }

    private fun aggregateByDay(records: List<Weight>): List<Pair<Instant, Double>> {
        return records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .map { (date, records) ->
                val average = records.map { it.weightKilograms }.average()
                date.atStartOfDay(ZoneId.systemDefault()).toInstant() to average
            }
    }

    private fun aggregateByWeek(records: List<Weight>): List<Pair<Instant, Double>> {
        val weekFields = WeekFields.of(Locale.getDefault())
        return records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).get(weekFields.weekOfWeekBasedYear()) }
            .map { (_, records) ->
                val average = records.map { it.weightKilograms }.average()
                records.first().time to average
            }
    }

    private fun aggregateByMonth(records: List<Weight>): List<Pair<Instant, Double>> {
        return records
            .groupBy { it.time.atZone(ZoneId.systemDefault()).month }
            .map { (_, records) ->
                val average = records.map { it.weightKilograms }.average()
                records.first().time to average
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
            TimeRange.THREE_MONTHS -> {
                val startMonth = _selectedDate.value.minusMonths(2)
                "${startMonth.format(DateTimeFormatter.ofPattern("MMM"))} - ${_selectedDate.value.format(DateTimeFormatter.ofPattern("MMM yyyy"))}"
            }
            TimeRange.YEAR -> _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy"))
        }
    }
}
