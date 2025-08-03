package com.d4viddf.medicationreminder.ui.features.onboarding

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
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
    val permissionStatus: Map<PermissionType, Boolean> = emptyMap(),
    val isNextEnabled: Boolean = true
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        val steps = getOnboardingSteps()
        _uiState.value = OnboardingUiState(steps = steps)
        checkAllPermissions()
        updateNextButtonState(0) // Initial check for the first page
    }

    private fun getOnboardingSteps(): List<OnboardingStepContent> {
        return listOf(
            OnboardingStepContent(R.string.onboarding_step1_pager_title, R.string.onboarding_step1_pager_desc, R.drawable.rounded_thumb_up_24),
            OnboardingStepContent(R.string.onboarding_step2_notifications_title, R.string.onboarding_step2_notifications_desc, R.drawable.rounded_notifications_24, PermissionType.NOTIFICATION),
            OnboardingStepContent(R.string.onboarding_step3_exact_alarm_title, R.string.onboarding_step3_exact_alarm_desc, R.drawable.rounded_alarm_24, PermissionType.EXACT_ALARM),
            OnboardingStepContent(R.string.onboarding_step_fullscreen_title, R.string.onboarding_step_fullscreen_desc, R.drawable.rounded_notification_sound_24, PermissionType.FULL_SCREEN_INTENT),
            OnboardingStepContent(R.string.onboarding_step4_finish_title, R.string.onboarding_step4_finish_desc, R.drawable.ic_check)
        )
    }

    private fun checkAllPermissions() {
        val newStatus = mutableMapOf<PermissionType, Boolean>()
        _uiState.value.steps.forEach { step ->
            step.permissionType?.let {
                newStatus[it] = isPermissionGranted(it)
            }
        }
        _uiState.update { it.copy(permissionStatus = newStatus) }
    }

    fun checkAllPermissionsAndUpdateState(){
        checkAllPermissions()
    }
    fun updatePermissionStatus(type: PermissionType, isGranted: Boolean) {
        val newStatus = _uiState.value.permissionStatus.toMutableMap()
        newStatus[type] = isGranted
        _uiState.update { it.copy(permissionStatus = newStatus) }
    }


    fun updateNextButtonState(currentPageIndex: Int) {
        val currentStep = _uiState.value.steps.getOrNull(currentPageIndex)
        val permissionType = currentStep?.permissionType
        val isEnabled = if (permissionType != null) {
            _uiState.value.permissionStatus[permissionType] ?: false
        } else {
            true // No permission required for this step
        }
        _uiState.update { it.copy(isNextEnabled = isEnabled) }
    }

    private fun isPermissionGranted(permissionType: PermissionType): Boolean {
        return when (permissionType) {
            PermissionType.NOTIFICATION ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true
            PermissionType.EXACT_ALARM ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
                } else true
            PermissionType.FULL_SCREEN_INTENT ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.canUseFullScreenIntent()
                } else true
        }
    }


    fun finishOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.updateOnboardingCompleted(true)
        }
    }
}