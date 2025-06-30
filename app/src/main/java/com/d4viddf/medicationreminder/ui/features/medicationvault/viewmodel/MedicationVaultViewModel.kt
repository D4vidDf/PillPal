package com.d4viddf.medicationreminder.ui.features.medicationvault.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
import com.d4viddf.medicationreminder.ui.features.medication_details.components.ProgressDetails // This might be needed if vault items show progress
import com.d4viddf.medicationreminder.workers.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class MedicationVaultViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val reminderRepository: MedicationReminderRepository, // Kept for potential future use (e.g. showing last taken)
    private val scheduleRepository: MedicationScheduleRepository, // Kept for potential future use
    @ApplicationContext private val appContext: Context // Kept for WorkerScheduler if actions are added from vault
) : ViewModel() {

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Search functionality
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Medication>>(emptyList())
    val searchResults: StateFlow<List<Medication>> = _searchResults.asStateFlow()

    // ProgressDetails might not be directly used in the vault list, but kept if detail view is launched from here
    private val _medicationProgressDetails = MutableStateFlow<ProgressDetails?>(null)
    val medicationProgressDetails: StateFlow<ProgressDetails?> = _medicationProgressDetails.asStateFlow()


    init {
        observeMedications()
        observeSearchQueryAndMedications()
    }

    private fun observeMedications() {
        viewModelScope.launch {
            _isLoading.value = true
            medicationRepository.getAllMedications().collect { medicationsList ->
                _medications.value = medicationsList
                _isLoading.value = false
            }
        }
    }

    private fun observeSearchQueryAndMedications() {
        viewModelScope.launch {
            combine(_searchQuery, _medications) { query, meds ->
                if (query.isBlank()) {
                    // When query is blank, searchResults could be empty or all medications
                    // depending on how you want the UI to behave before active search.
                    // For a "vault" that shows all by default, and filters on search,
                    // this should probably return `meds` if you want the main list to also be the search list.
                    // However, the original HomeScreen had a separate list for search results.
                    // Let's keep it as filtering only when query is not blank.
                    emptyList()
                } else {
                    meds.filter { medication ->
                        medication.name.contains(query, ignoreCase = true)
                    }
                }
            }.collect { filteredMedications ->
                _searchResults.value = filteredMedications
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshMedications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // This re-triggers collection in observeMedications if the underlying Flow emits anew
                 medicationRepository.getAllMedications().firstOrNull()
            } catch (e: Exception) {
                Log.e("MedicationVaultVM", "Error refreshing medications", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Example function that might be used if navigating to a detail view that needs this.
    // This is similar to what was in MedicationViewModel.
    fun observeMedicationAndRemindersForDailyProgress(medicationId: Int) {
        viewModelScope.launch {
            reminderRepository.getRemindersForMedication(medicationId).collect { remindersList ->
                val currentMedication = medicationRepository.getMedicationById(medicationId)
                val currentSchedule = scheduleRepository.getSchedulesForMedication(medicationId).firstOrNull()?.firstOrNull()
                calculateAndSetDailyProgressDetails(currentMedication, currentSchedule, remindersList)
            }
        }
    }

    suspend fun getMedicationById(medicationId: Int): Medication? {
        return withContext(Dispatchers.IO) {
            medicationRepository.getMedicationById(medicationId)
        }
    }

    private suspend fun calculateAndSetDailyProgressDetails(
        medication: Medication?,
        schedule: MedicationSchedule?,
        allRemindersForMedication: List<MedicationReminder>
    ) {
        if (medication == null || schedule == null) {
            _medicationProgressDetails.value = ProgressDetails(0,0,0,0f, "N/A")
            return
        }

        val today = LocalDate.now()
        val remindersMapForToday = ReminderCalculator.generateRemindersForPeriod(
            medication = medication,
            schedule = schedule,
            periodStartDate = today,
            periodEndDate = today
        )
        val scheduledTimesTodayList = remindersMapForToday[today] ?: emptyList()
        val totalDosesScheduledToday = scheduledTimesTodayList.size

        val dosesTakenToday = allRemindersForMedication.count { reminder ->
            try {
                val reminderDateTime = LocalDateTime.parse(reminder.reminderTime, ReminderCalculator.storableDateTimeFormatter)
                reminderDateTime.toLocalDate().isEqual(today) && reminder.isTaken
            } catch (e: DateTimeParseException) {
                false
            }
        }

        val progressFraction = if (totalDosesScheduledToday > 0) {
            dosesTakenToday.toFloat() / totalDosesScheduledToday.toFloat()
        } else {
            0f
        }
        val displayText = "$dosesTakenToday / $totalDosesScheduledToday"

        _medicationProgressDetails.value = ProgressDetails(
            taken = dosesTakenToday,
            remaining = (totalDosesScheduledToday - dosesTakenToday).coerceAtLeast(0),
            totalFromPackage = totalDosesScheduledToday,
            progressFraction = progressFraction.coerceIn(0f, 1f),
            displayText = displayText
        )
    }

    // These functions might be called if actions (like update/delete) are available from the vault's detail view.
    // For now, they are similar to MedicationViewModel.
    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.updateMedication(medication)
            WorkerScheduler.scheduleRemindersForMedication(appContext, medication.id)
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.deleteMedication(medication)
            WorkerScheduler.scheduleRemindersForMedication(appContext, medication.id)
        }
    }
}
