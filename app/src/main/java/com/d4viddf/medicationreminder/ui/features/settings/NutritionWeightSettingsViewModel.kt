package com.d4viddf.medicationreminder.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.repository.HealthDataRepository
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NutritionWeightSettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {
    private val _waterIntakeGoal = MutableStateFlow("")
    val waterIntakeGoal = _waterIntakeGoal.asStateFlow()

    private val _weightGoalValue = MutableStateFlow("")
    val weightGoalValue = _weightGoalValue.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.waterIntakeGoalFlow.collect {
                _waterIntakeGoal.value = it.toString()
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.weightGoalValueFlow.collect {
                _weightGoalValue.value = if (it == 0f) "" else it.toInt().toString()
            }
        }
    }

    fun onWaterIntakeGoalChange(goal: String) {
        _waterIntakeGoal.value = goal
    }

    fun saveWaterIntakeGoal() {
        viewModelScope.launch {
            _waterIntakeGoal.value.toIntOrNull()?.let {
                userPreferencesRepository.setWaterIntakeGoal(it)
            }
        }
    }

    fun onWeightGoalValueChange(value: String) {
        _weightGoalValue.value = value
    }

    fun saveWeightGoal() {
        viewModelScope.launch {
            _weightGoalValue.value.toIntOrNull()?.let {
                userPreferencesRepository.setWeightGoalValue(it.toFloat())
            }
        }
    }

    fun deleteNutritionData() {
        viewModelScope.launch {
            healthDataRepository.deleteAllWaterIntake()
        }
    }

    fun deleteWeightData() {
        viewModelScope.launch {
            healthDataRepository.deleteAllWeight()
            userPreferencesRepository.deleteWeightGoal()
        }
    }
}