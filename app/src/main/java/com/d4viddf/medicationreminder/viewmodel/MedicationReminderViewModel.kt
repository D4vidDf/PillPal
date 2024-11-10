package com.d4viddf.medicationreminder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationReminderViewModel @Inject constructor(
    private val reminderRepository: MedicationReminderRepository
) : ViewModel() {

    private val _reminders = MutableStateFlow<List<MedicationReminder>>(emptyList())
    val reminders: StateFlow<List<MedicationReminder>> = _reminders

    fun getRemindersForMedication(medicationId: Int) {
        viewModelScope.launch {
            reminderRepository.getRemindersForMedication(medicationId).collect {
                _reminders.value = it
            }
        }
    }

    fun insertReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            reminderRepository.insertReminder(reminder)
        }
    }

    fun markReminderAsTaken(reminderId: Int, takenAt: String) {
        viewModelScope.launch {
            reminderRepository.markReminderAsTaken(reminderId, takenAt)
        }
    }
}
