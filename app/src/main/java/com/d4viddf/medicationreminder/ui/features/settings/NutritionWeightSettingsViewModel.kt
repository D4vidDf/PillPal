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

    init {
        viewModelScope.launch {
            userPreferencesRepository.waterIntakeGoalFlow.collect {
                _waterIntakeGoal.value = it.toString()
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

    private val _weightGoalType = MutableStateFlow("lose")
    val weightGoalType = _weightGoalType.asStateFlow()

    private val _weightGoalValue = MutableStateFlow("")
    val weightGoalValue = _weightGoalValue.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.waterIntakeGoalFlow.collect {
                _waterIntakeGoal.value = it.toString()
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.weightGoalTypeFlow.collect {
                _weightGoalType.value = it
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.weightGoalValueFlow.collect {
                _weightGoalValue.value = it.toString()
            }
        }
    }

    fun onWeightGoalTypeChange(type: String) {
        _weightGoalType.value = type
    }

    fun onWeightGoalValueChange(value: String) {
        _weightGoalValue.value = value
    }

    fun saveWeightGoal() {
        viewModelScope.launch {
            userPreferencesRepository.setWeightGoalType(_weightGoalType.value)
            _weightGoalValue.value.toFloatOrNull()?.let {
                userPreferencesRepository.setWeightGoalValue(it)
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
        }
    }
}
