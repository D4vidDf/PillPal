package com.d4viddf.medicationreminder.ui.features.onboarding.viewmodel

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.ui.features.onboarding.screen.OnboardingStepContent
import com.d4viddf.medicationreminder.ui.features.onboarding.screen.PermissionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val steps: List<OnboardingStepContent> = emptyList(),
    val isCurrentStepPermissionGranted: Boolean = true
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = OnboardingUiState(steps = getOnboardingSteps())
        checkPermissionForCurrentStep(0) // Check for the initial step
    }

    private fun getOnboardingSteps(): List<OnboardingStepContent> {
        return listOf(
            OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc, R.drawable.rounded_thumb_up_24),
            OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, R.drawable.rounded_notifications_24, PermissionType.NOTIFICATION),
            OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, R.drawable.rounded_watch_24, PermissionType.EXACT_ALARM),
            OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, R.drawable.rounded_medication_24, PermissionType.FULL_SCREEN_INTENT),
            OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc, R.drawable.ic_check)
        )
    }

    fun checkPermissionForCurrentStep(pageIndex: Int) {
        val currentStep = uiState.value.steps.getOrNull(pageIndex)
        val isGranted = when (currentStep?.permissionType) {
            PermissionType.NOTIFICATION ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true
            PermissionType.EXACT_ALARM ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
                } else true
            PermissionType.FULL_SCREEN_INTENT -> true // Assumed granted
            null -> true
        }
        _uiState.update { it.copy(isCurrentStepPermissionGranted = isGranted) }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.updateOnboardingCompleted(true)
        }
    }
}
