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
import kotlinx.coroutines.launch
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
    val isOngoing: Boolean = false // To help UI decide how to render schedule bar
)

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val daysInMonth: List<CalendarDay> = emptyList(),
    val medicationSchedules: List<MedicationScheduleItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val medicationScheduleRepository: MedicationScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    // Formatter for medication start/end dates if they are in "yyyy-MM-dd" format
    private val dateParser = DateTimeFormatter.ISO_LOCAL_DATE // Assumes "yyyy-MM-dd"

    init {
        setSelectedDate(LocalDate.now())
    }

    fun setSelectedDate(date: LocalDate) {
        val newMonth = YearMonth.from(date)
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            currentMonth = newMonth,
            isLoading = true // Set loading true when selection changes
        )
        generateDaysInMonth(newMonth, date)
        fetchMedicationSchedulesForMonth(newMonth)
    }

    private fun generateDaysInMonth(month: YearMonth, selectedDate: LocalDate) {
        val today = LocalDate.now()
        val days = (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            CalendarDay(
                date = date,
                isSelected = date.isEqual(selectedDate),
                isToday = date.isEqual(today)
            )
        }
        _uiState.value = _uiState.value.copy(daysInMonth = days)
    }

    private fun parseDate(dateString: String?): LocalDate? {
        return try {
            dateString?.let { LocalDate.parse(it, dateParser) }
        } catch (e: DateTimeParseException) {
            Log.e("CalendarViewModel", "Error parsing date: $dateString", e)
            null // Or handle error appropriately
        }
    }

    private fun fetchMedicationSchedulesForMonth(currentDisplayMonth: YearMonth) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                medicationRepository.getAllMedications()
                    .combine(medicationScheduleRepository.getAllSchedules()) { medications, schedules ->
                        val medicationMap = medications.associateBy { it.id }
                        schedules.mapNotNull { schedule ->
                            medicationMap[schedule.medicationId]?.let { medication ->
                                val medStartDate = parseDate(medication.startDate)
                                val medEndDate = parseDate(medication.endDate)

                                // Determine if medication is active in the currentDisplayMonth
                                val monthStart = currentDisplayMonth.atDay(1)
                                val monthEnd = currentDisplayMonth.atEndOfMonth()

                                val isActiveInMonth = (medStartDate == null || medStartDate.isBefore(monthEnd) || medStartDate.isEqual(monthEnd)) &&
                                                      (medEndDate == null || medEndDate.isAfter(monthStart) || medEndDate.isEqual(monthStart))

                                if (!isActiveInMonth) {
                                    return@mapNotNull null // Skip this schedule if not active in month
                                }

                                var startDateText: String? = null
                                var endDateText: String? = null
                                val isOngoing: Boolean

                                val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

                                if (medStartDate != null && YearMonth.from(medStartDate) == currentDisplayMonth) {
                                    startDateText = "Starts ${medStartDate.format(monthDayFormatter)}"
                                }

                                if (medEndDate != null && YearMonth.from(medEndDate) == currentDisplayMonth) {
                                    endDateText = "Ends ${medEndDate.format(monthDayFormatter)}"
                                }

                                isOngoing = medStartDate != null && medStartDate.isBefore(monthStart) && (medEndDate == null || medEndDate.isAfter(monthEnd))


                                MedicationScheduleItem(
                                    medication = medication,
                                    schedule = schedule,
                                    startDateText = startDateText,
                                    endDateText = endDateText,
                                    isOngoing = isOngoing
                                )
                            }
                        }
                    }.collect { scheduleItems ->
                        _uiState.value = _uiState.value.copy(
                            medicationSchedules = scheduleItems.filterNotNull(), // Ensure no nulls if mapNotNull had issues
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error fetching schedules: ${e.localizedMessage}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load schedules: ${e.localizedMessage}"
                )
            }
        }
    }

    fun onNextMonth() {
        val currentMonthInState = _uiState.value.currentMonth
        val newSelectedDate = _uiState.value.selectedDate.with(currentMonthInState.plusMonths(1).atDay(1))
        setSelectedDate(newSelectedDate)
    }

    fun onPreviousMonth() {
        val currentMonthInState = _uiState.value.currentMonth
        val newSelectedDate = _uiState.value.selectedDate.with(currentMonthInState.minusMonths(1).atDay(1))
        setSelectedDate(newSelectedDate)
    }
}
