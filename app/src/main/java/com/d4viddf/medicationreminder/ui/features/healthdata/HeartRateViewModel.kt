package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.HeartRate
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.ui.features.healthdata.component.LineChartPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HeartRateViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _heartRateRecords = MutableStateFlow<List<HeartRate>>(emptyList())
    val heartRateRecords: StateFlow<List<HeartRate>> = _heartRateRecords.asStateFlow()

    private val _chartData = MutableStateFlow(HeartRateChartData())
    val chartData: StateFlow<HeartRateChartData> = _chartData.asStateFlow()

    init {
        fetchHeartRateRecords()
    }

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private fun fetchHeartRateRecords() {
        healthDataRepository.getHeartRateBetween(
            LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            Instant.now(),
            hasPermissions.value
        ).onEach { records ->
            _heartRateRecords.value = records
            _chartData.value = aggregateDataForChart(records)
        }.launchIn(viewModelScope)
    }

    private fun aggregateDataForChart(records: List<HeartRate>): HeartRateChartData {
        if (records.isEmpty()) {
            return HeartRateChartData()
        }
        val data = records.map {
            LineChartPoint(
                x = it.time.epochSecond.toFloat(),
                y = it.beatsPerMinute.toFloat(),
                label = ""
            )
        }
        val labels = records.map {
            it.time.atZone(ZoneId.systemDefault()).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
        return HeartRateChartData(lineChartData = data, labels = labels)
    }
}
