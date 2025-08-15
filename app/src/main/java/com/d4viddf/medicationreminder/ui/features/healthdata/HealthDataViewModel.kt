package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.model.healthdata.BodyTemperature
import com.d4viddf.medicationreminder.data.model.healthdata.WaterIntake
import com.d4viddf.medicationreminder.data.model.healthdata.Weight
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HealthDataViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

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