package com.d4viddf.medicationreminder.viewmodel

import app.cash.turbine.test
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var medicationRepository: MedicationRepository

    @Mock
    private lateinit var medicationScheduleRepository: MedicationScheduleRepository

    private lateinit var viewModel: CalendarViewModel

    // Test Data - Re-evaluate for a 3-week (21-day) visible window.
    // Let's assume a selectedDate of Feb 15, 2023 (Wednesday).
    // Middle week: Mon, Feb 13 to Sun, Feb 19.
    // Visible window: Mon, Feb 6 to Sun, Feb 26.

    private val med1 = Medication(id = 1, name = "Med1 (Spans all 3 weeks)", startDate = "2023-02-01", endDate = "2023-03-01", color = "Red", packageSize = 30, remainingDoses = 30)
    private val schedule1 = MedicationSchedule(id = 1, medicationId = 1, scheduleType = ScheduleType.DAILY)

    private val med2 = Medication(id = 2, name = "Med2 (Only middle week)", startDate = "2023-02-13", endDate = "2023-02-19", color = "Green", packageSize = 7, remainingDoses = 7)
    private val schedule2 = MedicationSchedule(id = 2, medicationId = 2, scheduleType = ScheduleType.DAILY)

    private val med3 = Medication(id = 3, name = "Med3 (First week only)", startDate = "2023-02-06", endDate = "2023-02-12", color = "Blue", packageSize = 7, remainingDoses = 7)
    private val schedule3 = MedicationSchedule(id = 3, medicationId = 3, scheduleType = ScheduleType.DAILY)

    private val med4 = Medication(id = 4, name = "Med4 (Last week, ongoing)", startDate = "2023-02-20", endDate = null, color = "Yellow", packageSize = 30, remainingDoses = 30)
    private val schedule4 = MedicationSchedule(id = 4, medicationId = 4, scheduleType = ScheduleType.DAILY)

    private val med5 = Medication(id = 5, name = "Med5 (Starts mid-first, ends mid-middle)", startDate = "2023-02-08", endDate = "2023-02-15", color = "Purple", packageSize = 10, remainingDoses = 10)
    private val schedule5 = MedicationSchedule(id = 5, medicationId = 5, scheduleType = ScheduleType.DAILY)

    // Outside the Feb 6 - Feb 26 range
    private val med6_Outside = Medication(id = 6, name = "Med6 (Outside)", startDate = "2023-01-01", endDate = "2023-01-05", color = "Orange", packageSize = 5, remainingDoses = 5)
    private val schedule6 = MedicationSchedule(id = 6, medicationId = 6, scheduleType = ScheduleType.DAILY)


    private val allTestMedications = listOf(med1, med2, med3, med4, med5, med6_Outside)
    private val allTestSchedules = listOf(schedule1, schedule2, schedule3, schedule4, schedule5, schedule6)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(allTestMedications))
        `when`(medicationScheduleRepository.getAllSchedules()).thenReturn(flowOf(allTestSchedules))

        viewModel = CalendarViewModel(medicationRepository, medicationScheduleRepository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun getMondayOfNthWeekFromStart(startDate: LocalDate, weekOffset: Int): LocalDate {
        var monday = startDate
        while (monday.dayOfWeek != DayOfWeek.MONDAY) {
            monday = monday.minusDays(1)
        }
        return monday.plusWeeks(weekOffset.toLong())
    }

    @Test
    fun `initial state is correct with 21 visibleDays`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            val today = LocalDate.now()

            assertEquals(today, state.selectedDate)
            assertEquals(YearMonth.from(today), state.currentMonth)
            assertFalse(state.isLoading)
            assertNull(state.error)

            assertEquals(21, state.visibleDays.size)
            val expectedFirstVisibleDay = getMondayOfNthWeekFromStart(today, -1) // Monday of week before selectedDate's week
            assertEquals(expectedFirstVisibleDay, state.visibleDays.first().date)
            assertEquals(DayOfWeek.MONDAY, state.visibleDays.first().date.dayOfWeek)
            assertTrue(state.visibleDays.any { it.date.isEqual(today) && it.isSelected })
            assertTrue(state.visibleDays.any { it.date.isEqual(today) && it.isToday })
        }
    }

    @Test
    fun `setSelectedDate updates date, 21 visibleDays, and schedules correctly`() = runTest {
        val testSelectedDate = LocalDate.of(2023, 2, 15) // Wednesday

        viewModel.uiState.test {
            awaitItem()

            viewModel.setSelectedDate(testSelectedDate)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertEquals(testSelectedDate, loadingState.selectedDate)
            assertEquals(YearMonth.from(testSelectedDate), loadingState.currentMonth)

            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertEquals(testSelectedDate, finalState.selectedDate)
            assertEquals(YearMonth.from(testSelectedDate), finalState.currentMonth)

            assertEquals(21, finalState.visibleDays.size)
            val expectedFirstVisibleDay = getMondayOfNthWeekFromStart(testSelectedDate, -1) // Monday of week before Feb 15's week
            assertEquals(expectedFirstVisibleDay, finalState.visibleDays.first().date) // Should be Feb 6
            assertEquals(DayOfWeek.MONDAY, finalState.visibleDays.first().date.dayOfWeek)
            assertTrue(finalState.visibleDays.any { it.date == testSelectedDate && it.isSelected })

            // Based on test data for Feb 6 - Feb 26 window: meds 1,2,3,4,5 should be present
            assertEquals(5, finalState.medicationSchedules.size)
        }
    }

    @Test
    fun `onNextWeek updates selectedDate and 21 visibleDays correctly`() = runTest {
        val initialSelectedDate = LocalDate.of(2023, 2, 15) // Wednesday
        viewModel.setSelectedDate(initialSelectedDate)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onNextWeek()

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertEquals(initialSelectedDate.plusWeeks(1), loadingState.selectedDate)

            val finalState = awaitItem()
            assertFalse(finalState.isLoading)

            val expectedNewSelectedDate = initialSelectedDate.plusWeeks(1) // Feb 22, 2023
            assertEquals(expectedNewSelectedDate, finalState.selectedDate)
            assertEquals(YearMonth.from(expectedNewSelectedDate), finalState.currentMonth)

            assertEquals(21, finalState.visibleDays.size)
            val expectedFirstVisibleDay = getMondayOfNthWeekFromStart(expectedNewSelectedDate, -1)
            assertEquals(expectedFirstVisibleDay, finalState.visibleDays.first().date) // Mon, Feb 13
            assertEquals(DayOfWeek.MONDAY, finalState.visibleDays.first().date.dayOfWeek)
            assertTrue(finalState.visibleDays.any { it.date == expectedNewSelectedDate && it.isSelected })
        }
    }

    @Test
    fun `onPreviousWeek updates selectedDate and 21 visibleDays correctly`() = runTest {
        val initialSelectedDate = LocalDate.of(2023, 2, 15) // Wednesday
        viewModel.setSelectedDate(initialSelectedDate)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onPreviousWeek()

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertEquals(initialSelectedDate.minusWeeks(1), loadingState.selectedDate)

            val finalState = awaitItem()
            assertFalse(finalState.isLoading)

            val expectedNewSelectedDate = initialSelectedDate.minusWeeks(1) // Feb 8, 2023
            assertEquals(expectedNewSelectedDate, finalState.selectedDate)
            assertEquals(YearMonth.from(expectedNewSelectedDate), finalState.currentMonth)

            assertEquals(21, finalState.visibleDays.size)
            val expectedFirstVisibleDay = getMondayOfNthWeekFromStart(expectedNewSelectedDate, -1)
            assertEquals(expectedFirstVisibleDay, finalState.visibleDays.first().date) // Mon, Jan 30
            assertEquals(DayOfWeek.MONDAY, finalState.visibleDays.first().date.dayOfWeek)
            assertTrue(finalState.visibleDays.any { it.date == expectedNewSelectedDate && it.isSelected })
        }
    }

    @Test
    fun `fetchMedicationSchedulesForVisibleDays filters and formats offsets correctly for 21-day window`() = runTest {
        val selectedDateForTest = LocalDate.of(2023, 2, 15) // Wednesday
        // Visible window will be Mon, Feb 6 to Sun, Feb 26.
        // Middle week: Feb 13-19. Indices 7-13.

        viewModel.uiState.test {
            awaitItem()

            viewModel.setSelectedDate(selectedDateForTest) // Triggers fetch

            awaitItem() // Loading state
            val finalState = awaitItem() // Final state after fetch

            assertFalse("Should not be loading after fetch", finalState.isLoading)
            assertNull("Error should be null on success", finalState.error)
            assertEquals(21, finalState.visibleDays.size)
            assertEquals(LocalDate.of(2023,2,6), finalState.visibleDays.first().date) // Mon Feb 6
            assertEquals(LocalDate.of(2023,2,26), finalState.visibleDays.last().date)  // Sun Feb 26

            val schedules = finalState.medicationSchedules
            assertEquals("Expected 5 medications in the Feb 6 - Feb 26 window", 5, schedules.size)

            val item1 = schedules.find { it.medication.id == med1.id } // Spans all 3 weeks (Feb 1 - Mar 1)
            assertNotNull(item1); item1!!
            assertEquals(0, item1.startOffsetInVisibleDays) // Starts before visible window
            assertEquals(20, item1.endOffsetInVisibleDays) // Ends after visible window
            assertTrue(item1.isOngoingOverall)

            val item2 = schedules.find { it.medication.id == med2.id } // Only middle week (Feb 13-19)
            assertNotNull(item2); item2!!
            assertEquals(7, item2.startOffsetInVisibleDays)  // Feb 13 is index 7
            assertEquals(13, item2.endOffsetInVisibleDays) // Feb 19 is index 13
            assertFalse(item2.isOngoingOverall)

            val item3 = schedules.find { it.medication.id == med3.id } // First week only (Feb 6-12)
            assertNotNull(item3); item3!!
            assertEquals(0, item3.startOffsetInVisibleDays)  // Feb 6 is index 0
            assertEquals(6, item3.endOffsetInVisibleDays)  // Feb 12 is index 6
            assertFalse(item3.isOngoingOverall)

            val item4 = schedules.find { it.medication.id == med4.id } // Last week, ongoing (Starts Feb 20)
            assertNotNull(item4); item4!!
            assertEquals(14, item4.startOffsetInVisibleDays) // Feb 20 is index 14
            assertEquals(20, item4.endOffsetInVisibleDays) // Ongoing, so fills to end of visible range
            assertTrue(item4.isOngoingOverall)

            val item5 = schedules.find { it.medication.id == med5.id } // Starts mid-first (Feb 8), ends mid-middle (Feb 15)
            assertNotNull(item5); item5!!
            assertEquals(2, item5.startOffsetInVisibleDays)  // Feb 8 is index 2
            assertEquals(9, item5.endOffsetInVisibleDays)  // Feb 15 is index 9
            assertFalse(item5.isOngoingOverall)
        }
    }

    @Test
    fun `fetchMedicationSchedulesForVisibleDays handles no medications`() = runTest {
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(emptyList()))
        `when`(medicationScheduleRepository.getAllSchedules()).thenReturn(flowOf(emptyList()))

        val localViewModel = CalendarViewModel(medicationRepository, medicationScheduleRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        localViewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertTrue(finalState.medicationSchedules.isEmpty())
        }
    }

    // Error handling tests remain largely the same, as the fetch mechanism is similar
    @Test
    fun `fetchMedicationSchedulesForVisibleDays handles repository error for medications`() = runTest {
        val errorMessage = "Medication Database error"
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf { throw RuntimeException(errorMessage) })

        val localViewModel = CalendarViewModel(medicationRepository, medicationScheduleRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        localViewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertTrue("Error message should contain: $errorMessage", finalState.error?.contains(errorMessage) ?: false)
        }
    }

     @Test
    fun `fetchMedicationSchedulesForVisibleDays handles repository error for schedules`() = runTest {
        val errorMessage = "Schedule Database error"
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(listOf(med1))) // Provide some valid meds
        `when`(medicationScheduleRepository.getAllSchedules()).thenReturn(flowOf { throw RuntimeException(errorMessage) })

        val localViewModel = CalendarViewModel(medicationRepository, medicationScheduleRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        localViewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertTrue("Error message should contain: $errorMessage", finalState.error?.contains(errorMessage) ?: false)
        }
    }
}
