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
    val startDateText: String? = null, // May become less relevant
    val endDateText: String? = null,   // May become less relevant
    val isOngoingOverall: Boolean = false, // If the medication itself is ongoing beyond the visible range
    val startOffsetInVisibleDays: Int?, // Index in visibleDays where bar starts
    val endOffsetInVisibleDays: Int?     // Index in visibleDays where bar ends
)

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(), // This might still be useful for the title
    val visibleDays: List<CalendarDay> = emptyList(),
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
        // Generate new visible days first
        val newVisibleDays = generateVisibleDays(date)

        _uiState.value = _uiState.value.copy( // Update state in one go
            selectedDate = date,
            currentMonth = newMonth, // Keep updating this for the title
            isLoading = true,
            visibleDays = newVisibleDays // Set the newly generated visibleDays
        )
        // Pass the just generated visibleDays to the fetch function
        fetchMedicationSchedulesForVisibleDays(newVisibleDays)
    }

    // Now returns the list and doesn't update state directly
    private fun generateVisibleDays(selectedDate: LocalDate): List<CalendarDay> {
        val today = LocalDate.now()
        // Calculate Monday of the week containing selectedDate (this is the center of our 3-week view)
        var middleWeekMonday = selectedDate
        while (middleWeekMonday.dayOfWeek != DayOfWeek.MONDAY) {
            middleWeekMonday = middleWeekMonday.minusDays(1)
        }

        // Start from one week before the middle week's Monday
        val firstDayToShow = middleWeekMonday.minusWeeks(1)

        val daysToShow = mutableListOf<CalendarDay>()
        for (i in 0 until 21) { // 3 weeks = 21 days
            val dayInRange = firstDayToShow.plusDays(i.toLong())
            daysToShow.add(
                CalendarDay(
                    date = dayInRange,
                    isSelected = dayInRange.isEqual(selectedDate), // Highlight the originally selected date
                    isToday = dayInRange.isEqual(today)
                )
            )
        }
        return daysToShow
    }

    private fun parseDate(dateString: String?): LocalDate? {
        return try {
            dateString?.let { LocalDate.parse(it, dateParser) }
        } catch (e: DateTimeParseException) {
            Log.e("CalendarViewModel", "Error parsing date: $dateString", e)
            null // Or handle error appropriately
        }
    }

    // Signature changed to accept the list of CalendarDay objects
    private fun fetchMedicationSchedulesForVisibleDays(currentVisibleDays: List<CalendarDay>) {
        viewModelScope.launch {
            // isLoading is already set true by setSelectedDate
            try {
                val visibleDaysList = currentVisibleDays.map { it.date } // Use passed parameter
                if (visibleDaysList.isEmpty()) {
                    _uiState.value = _uiState.value.copy(medicationSchedules = emptyList(), isLoading = false)
                    return@launch
                }

                medicationRepository.getAllMedications()
                    .combine(medicationScheduleRepository.getAllSchedules()) { medications, schedules ->
                        val medicationMap = medications.associateBy { it.id }
                        schedules.mapNotNull { schedule ->
                            medicationMap[schedule.medicationId]?.let { medication ->
                                val medStartDate = parseDate(medication.startDate)
                                val medEndDate = parseDate(medication.endDate)

                                val firstVisibleDate = visibleDaysList.first()
                                val lastVisibleDate = visibleDaysList.last()

                                // Check overlap with the actual visibleDays range
                                val actualStartDate = medStartDate ?: firstVisibleDate // Treat null start as very old
                                val actualEndDate = medEndDate ?: lastVisibleDate     // Treat null end as very far in future

                                if (actualStartDate.isAfter(lastVisibleDate) || actualEndDate.isBefore(firstVisibleDate)) {
                                    return@mapNotNull null // Not within the visible day range at all
                                }

                                var startDateText: String? = null
                                var endDateText: String? = null
                                var currentStartOffset: Int? = null
                                var currentEndOffset: Int? = null

                                val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

                                // Calculate offsets
                                if (actualStartDate.isBefore(firstVisibleDate) || actualStartDate.isEqual(firstVisibleDate)) {
                                    currentStartOffset = 0
                                } else {
                                    currentStartOffset = visibleDaysList.indexOf(actualStartDate).takeIf { it != -1 }
                                }

                                if (actualEndDate.isAfter(lastVisibleDate) || actualEndDate.isEqual(lastVisibleDate)) {
                                    currentEndOffset = visibleDaysList.size - 1
                                } else {
                                    currentEndOffset = visibleDaysList.indexOf(actualEndDate).takeIf { it != -1 }
                                }

                                // If either offset is null but the medication is active in range (e.g. starts before, ends after), it should span the whole view.
                                // However, the previous check (actualStartDate.isAfter(lastVisibleDate) || actualEndDate.isBefore(firstVisibleDate))
                                // should mean that if we reach here, at least part of it is in view.
                                // If currentStartOffset or currentEndOffset is still null, it means the start/end date itself is not one of the visibleDays,
                                // but the range overlaps. Example: med starts/ends between two visible days.
                                // For simplicity now, we require start/end to align with a visible day or be outside the range.
                                // More precise drawing can be done on canvas later.

                                if (medStartDate != null && medStartDate.isEqual(firstVisibleDate) || (medStartDate?.isAfter(firstVisibleDate) == true && medStartDate.isBefore(lastVisibleDate.plusDays(1)))) {
                                    startDateText = "Starts ${medStartDate?.format(monthDayFormatter)}"
                                }
                                if (medEndDate != null && medEndDate.isEqual(lastVisibleDate) || (medEndDate?.isBefore(lastVisibleDate) == true && medEndDate.isAfter(firstVisibleDate.minusDays(1)))) {
                                    endDateText = "Ends ${medEndDate?.format(monthDayFormatter)}"
                                }
                                val isOngoingOverall = medEndDate == null || medEndDate.isAfter(lastVisibleDate)

                                MedicationScheduleItem(
                                    medication = medication,
                                    schedule = schedule,
                                    startDateText = startDateText,
                                    endDateText = endDateText,
                                    isOngoingOverall = isOngoingOverall,
                                    startOffsetInVisibleDays = currentStartOffset,
                                    endOffsetInVisibleDays = currentEndOffset
                                ).also { item ->
                                    Log.d("CalendarViewModel", "Created Item: ${item.medication.name}, startOff: ${item.startOffsetInVisibleDays}, endOff: ${item.endOffsetInVisibleDays}, ongoing: ${item.isOngoingOverall}")
                                }
                            }
                        }
                    }.collect { scheduleItems ->
                        Log.d("CalendarViewModel", "Final scheduleItems size: ${scheduleItems.size}")
                        _uiState.value = _uiState.value.copy(
                            medicationSchedules = scheduleItems.filterNotNull(),
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

    fun onPreviousWeek() {
        val newSelectedDate = _uiState.value.selectedDate.minusWeeks(1)
        setSelectedDate(newSelectedDate)
    }

    fun onNextWeek() {
        val newSelectedDate = _uiState.value.selectedDate.plusWeeks(1)
        setSelectedDate(newSelectedDate)
    }
    // Removed onNextMonth and onPreviousMonth
}
