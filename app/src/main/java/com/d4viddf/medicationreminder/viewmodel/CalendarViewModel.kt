package com.d4viddf.medicationreminder.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job // Import Job
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject

data class CalendarDay(
    val date: LocalDate,
    val isSelected: Boolean,
    val isToday: Boolean
)

data class MedicationScheduleItem(
    val medication: Medication,
    val schedule: MedicationSchedule,
    val startDateText: String? = null,
    val endDateText: String? = null,
    val actualStartDate: LocalDate, // Added
    val actualEndDate: LocalDate?,  // Added
    val isOngoingOverall: Boolean = false // Definition changed: true if medication.endDate is null
)

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(), // Updated by setSelectedDate
    // val visibleDays: List<CalendarDay> = emptyList(), // Removed
    val medicationSchedules: List<MedicationScheduleItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedMedicationId: Int? = null // Added for selected medication
)

// private fun createCalendarDay(...) // Removed as visibleDays is removed

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val medicationScheduleRepository: MedicationScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    // private val dateParser = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()) // Removed
    private var fetchJob: Job? = null

    init {
        setSelectedDate(LocalDate.now())
    }

    fun setSelectedDate(date: LocalDate) {
        // val newMonth = YearMonth.from(date) // currentMonth is updated directly
        // val newVisibleDays = generateVisibleDays(date) // Removed

        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            currentMonth = YearMonth.from(date), // Update currentMonth based on selectedDate
            isLoading = true
            // visibleDays = newVisibleDays // Removed
        )
        loadMedicationScheduleItems() // Renamed and no longer takes visibleDays
    }

    // private fun generateVisibleDays(selectedDate: LocalDate): List<CalendarDay> { ... } // Removed

    private fun parseDate(dateString: String?): LocalDate? {
        if (dateString.isNullOrBlank()) {
            return null
        }
        return try {
            // Try ISO format first (yyyy-MM-dd)
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            try {
                // If ISO fails, try dd/MM/yyyy format
                LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
            } catch (e2: DateTimeParseException) {
                Log.e("CalendarViewModel", "Error parsing date string '$dateString' with known formats.", e2)
                null
            }
        }
    }

    // Renamed from fetchMedicationSchedulesForVisibleDays, no longer takes currentVisibleDays
    private fun loadMedicationScheduleItems() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            // isLoading is set by setSelectedDate or init
            try {
                medicationRepository.getAllMedications()
                    .combine(medicationScheduleRepository.getAllSchedules()) { medications, schedules ->
                        val medicationMap = medications.associateBy { it.id }
                        schedules.mapNotNull { schedule ->
                            medicationMap[schedule.medicationId]?.let { medication ->
                                val parsedMedStartDate = parseDate(medication.startDate)
                                val parsedRegistrationDate = parseDate(medication.registrationDate)
                                val itemActualStartDate = parsedMedStartDate ?: parsedRegistrationDate

                                // If itemActualStartDate is null, this medication schedule item is invalid or has no start point.
                                // Depending on business logic, you might skip it, log an error, or use a default.
                                // For now, let's skip if no valid start date can be determined.
                                if (itemActualStartDate == null) {
                                    Log.w("CalendarViewModel", "Medication ${medication.name} (ID: ${medication.id}) has no valid start or registration date. Skipping.")
                                    return@mapNotNull null
                                }

                                val medEndDate = parseDate(medication.endDate)
                                val itemActualEndDate = medEndDate

                                val isOngoing = medEndDate == null

                                var startDateText: String? = null
                                var endDateText: String? = null
                                val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

                                // Update startDateText and endDateText logic to not depend on visibleDaysList
                                parsedMedStartDate?.let {
                                    startDateText = "Starts ${it.format(monthDayFormatter)}"
                                }
                                medEndDate?.let {
                                    endDateText = "Ends ${it.format(monthDayFormatter)}"
                                }

                                MedicationScheduleItem(
                                    medication = medication,
                                    schedule = schedule,
                                    startDateText = startDateText,
                                    endDateText = endDateText,
                                    actualStartDate = itemActualStartDate,
                                    actualEndDate = itemActualEndDate,
                                    isOngoingOverall = isOngoing
                                )
                                // Removed logging for offsets as they are no longer calculated here
                            }
                        }
                    }.collect { scheduleItems ->
                        Log.d("CalendarViewModel", "Loaded ${scheduleItems.size} schedule items.")
                        _uiState.value = _uiState.value.copy(
                            medicationSchedules = scheduleItems.filterNotNull(),
                            isLoading = false,
                            error = null // Clear any previous error on successful load
                        )
                    }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.i("CalendarViewModel", "Schedule items loading job was cancelled.")
                    throw e
                }
                Log.e("CalendarViewModel", "Error loading schedule items: ${e.localizedMessage}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load schedules: ${e.localizedMessage}"
                )
            }
        }
    }

    fun onPreviousWeek() {
        val newSelectedDate = _uiState.value.selectedDate.minusWeeks(1)
        setSelectedDate(newSelectedDate) // This will trigger UI update and data reload via loadMedicationScheduleItems
    }

    fun onNextWeek() {
        val newSelectedDate = _uiState.value.selectedDate.plusWeeks(1)
        setSelectedDate(newSelectedDate) // This will trigger UI update and data reload
    }

    fun setSelectedMedicationId(medicationId: Int?) {
        _uiState.value = _uiState.value.copy(selectedMedicationId = medicationId)
    }
    // loadMorePastDays() and loadMoreFutureDays() removed
}
