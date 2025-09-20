package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.healthconnect.HealthConnectManager
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.WaterPreset
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HealthDataViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository,
    val healthConnectManager: HealthConnectManager
) : ViewModel() {

    val waterPresets = healthDataRepository.getWaterPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWaterPreset(name: String, amount: Double) {
        viewModelScope.launch {
            healthDataRepository.insertWaterPreset(WaterPreset(name = name, amount = amount))
        }
    }

    fun deleteWaterPreset(id: Int) {
        viewModelScope.launch {
            healthDataRepository.deleteWaterPreset(id)
        }
    }

    fun logWater(volumeMl: Double, time: Instant, type: String?) {
        viewModelScope.launch {
            val record = WaterIntake(
                time = time,
                volumeMilliliters = volumeMl,
                type = type,
                sourceApp = "com.d4viddf.medicationreminder"
            )
            healthDataRepository.insertWaterIntake(record)
        }
    }

    fun logWeight(weightKg: Double, time: Instant) {
        viewModelScope.launch {
            val record = Weight(
                time = time,
                weightKilograms = weightKg,
                sourceApp = "com.d4viddf.medicationreminder"
            )
            healthDataRepository.insertWeight(record)
        }
    }

    fun logTemperature(tempCelsius: Double, time: Instant, location: Int) {
        viewModelScope.launch {
            val record = BodyTemperature(
                time = time,
                temperatureCelsius = tempCelsius,
                measurementLocation = location,
                sourceApp = "com.d4viddf.medicationreminder"
            )
            healthDataRepository.insertBodyTemperature(record)
        }
    }
}