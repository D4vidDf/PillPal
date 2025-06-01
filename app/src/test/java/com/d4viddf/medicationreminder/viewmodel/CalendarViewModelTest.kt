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

    // Test Data - Adjusted for week view testing
    // Scenario: Visible week is Mon, Feb 13, 2023 to Sun, Feb 19, 2023
    // Selected date for this scenario will be Feb 15, 2023 (Wednesday)

    // Spans before, during, and after the visible week
    private val med1_FullOverlap = Medication(id = 1, name = "Med Full Overlap", startDate = "2023-02-01", endDate = "2023-02-28", color = "Red", packageSize = 30, remainingDoses = 30)
    private val schedule1 = MedicationSchedule(id = 1, medicationId = 1, scheduleType = ScheduleType.DAILY)

    // Starts before, ends within the visible week
    private val med2_StartBeforeEndIn = Medication(id = 2, name = "Med Start Before End In", startDate = "2023-02-10", endDate = "2023-02-15", color = "Green", packageSize = 30, remainingDoses = 30) // Ends on Wednesday
    private val schedule2 = MedicationSchedule(id = 2, medicationId = 2, scheduleType = ScheduleType.DAILY)

    // Starts within, ends within the visible week
    private val med3_StartInEndIn = Medication(id = 3, name = "Med Start In End In", startDate = "2023-02-14", endDate = "2023-02-16", color = "Blue", packageSize = 30, remainingDoses = 30) // Tue-Thu
    private val schedule3 = MedicationSchedule(id = 3, medicationId = 3, scheduleType = ScheduleType.DAILY)

    // Starts within, ends after the visible week
    private val med4_StartInEndAfter = Medication(id = 4, name = "Med Start In End After", startDate = "2023-02-17", endDate = "2023-03-05", color = "Yellow", packageSize = 30, remainingDoses = 30) // Fri - Future
    private val schedule4 = MedicationSchedule(id = 4, medicationId = 4, scheduleType = ScheduleType.DAILY)

    // Starts and ends before the visible week (should not appear)
    private val med5_Before = Medication(id = 5, name = "Med Before", startDate = "2023-02-01", endDate = "2023-02-05", color = "Purple", packageSize = 30, remainingDoses = 30)
    private val schedule5 = MedicationSchedule(id = 5, medicationId = 5, scheduleType = ScheduleType.DAILY)

    // Starts and ends after the visible week (should not appear)
    private val med6_After = Medication(id = 6, name = "Med After", startDate = "2023-02-25", endDate = "2023-02-28", color = "Orange", packageSize = 30, remainingDoses = 30)
    private val schedule6 = MedicationSchedule(id = 6, medicationId = 6, scheduleType = ScheduleType.DAILY)

    // Ongoing medication, started long ago
    private val med7_OngoingOld = Medication(id = 7, name = "Med Ongoing Old", startDate = "2022-01-01", endDate = null, color = "Cyan", packageSize = 30, remainingDoses = 30)
    private val schedule7 = MedicationSchedule(id = 7, medicationId = 7, scheduleType = ScheduleType.DAILY)

    // Starts within week, ongoing
     private val med8_StartInOngoing = Medication(id = 8, name = "Med Start In Ongoing", startDate = "2023-02-16", endDate = null, color = "Magenta", packageSize = 30, remainingDoses = 30) // Starts Thu, ongoing
    private val schedule8 = MedicationSchedule(id = 8, medicationId = 8, scheduleType = ScheduleType.DAILY)


    private val allMedications = listOf(med1_FullOverlap, med2_StartBeforeEndIn, med3_StartInEndIn, med4_StartInEndAfter, med5_Before, med6_After, med7_OngoingOld, med8_StartInOngoing)
    private val allSchedules = listOf(schedule1, schedule2, schedule3, schedule4, schedule5, schedule6, schedule7, schedule8)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Default mock responses, can be overridden in specific tests
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(allMedications))
        `when`(medicationScheduleRepository.getAllSchedules()).thenReturn(flowOf(allSchedules))

        viewModel = CalendarViewModel(medicationRepository, medicationScheduleRepository)
        testDispatcher.scheduler.advanceUntilIdle() // For init block's setSelectedDate and fetch
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct with week view`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem() // State after init
            val today = LocalDate.now()

            assertEquals(today, state.selectedDate)
            assertEquals(YearMonth.from(today), state.currentMonth) // Title month
            assertFalse(state.isLoading)
            assertNull(state.error)

            assertEquals(7, state.visibleDays.size)
            assertEquals(DayOfWeek.MONDAY, state.visibleDays.first().date.dayOfWeek)
            assertTrue(state.visibleDays.any { it.date.isEqual(today) && it.isSelected })
            assertTrue(state.visibleDays.any { it.date.isEqual(today) && it.isToday })
        }
    }

    @Test
    fun `setSelectedDate updates date, visibleDays, and schedules correctly`() = runTest {
        val testSelectedDate = LocalDate.of(2023, 2, 15) // Wednesday
        val expectedMonday = LocalDate.of(2023, 2, 13)

        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.setSelectedDate(testSelectedDate)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertEquals(testSelectedDate, loadingState.selectedDate)
            assertEquals(YearMonth.from(testSelectedDate), loadingState.currentMonth)

            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertEquals(testSelectedDate, finalState.selectedDate)
            assertEquals(YearMonth.from(testSelectedDate), finalState.currentMonth)

            assertEquals(7, finalState.visibleDays.size)
            assertEquals(expectedMonday, finalState.visibleDays.first().date)
            assertEquals(DayOfWeek.MONDAY, finalState.visibleDays.first().date.dayOfWeek)
            assertTrue(finalState.visibleDays.any { it.date == testSelectedDate && it.isSelected })

            // Check that schedules are filtered for the week of Feb 13-19
            // Meds expected: 1, 2, 3, 4, 7, 8 (6 meds)
            assertEquals(6, finalState.medicationSchedules.size)
        }
    }

    @Test
    fun `onNextWeek updates selectedDate and visibleDays correctly`() = runTest {
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
            val expectedNewMonday = LocalDate.of(2023, 2, 20)

            assertEquals(expectedNewSelectedDate, finalState.selectedDate)
            assertEquals(YearMonth.from(expectedNewSelectedDate), finalState.currentMonth)

            assertEquals(7, finalState.visibleDays.size)
            assertEquals(expectedNewMonday, finalState.visibleDays.first().date)
            assertEquals(DayOfWeek.MONDAY, finalState.visibleDays.first().date.dayOfWeek)
            assertTrue(finalState.visibleDays.any { it.date == expectedNewSelectedDate && it.isSelected })
        }
    }

    @Test
    fun `onPreviousWeek updates selectedDate and visibleDays correctly`() = runTest {
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
            val expectedNewMonday = LocalDate.of(2023, 2, 6)

            assertEquals(expectedNewSelectedDate, finalState.selectedDate)
            assertEquals(YearMonth.from(expectedNewSelectedDate), finalState.currentMonth)

            assertEquals(7, finalState.visibleDays.size)
            assertEquals(expectedNewMonday, finalState.visibleDays.first().date)
            assertEquals(DayOfWeek.MONDAY, finalState.visibleDays.first().date.dayOfWeek)
            assertTrue(finalState.visibleDays.any { it.date == expectedNewSelectedDate && it.isSelected })
        }
    }

    @Test
    fun `fetchMedicationSchedulesForVisibleDays filters and formats offsets correctly`() = runTest {
        // Visible week: Mon, Feb 13, 2023 to Sun, Feb 19, 2023
        val selectedDateForTest = LocalDate.of(2023, 2, 15) // Wednesday
        val expectedVisibleMonday = LocalDate.of(2023, 2, 13)
        val expectedVisibleSunday = LocalDate.of(2023, 2, 19)

        viewModel.uiState.test {
            awaitItem() // Initial state from setup or previous test

            viewModel.setSelectedDate(selectedDateForTest)

            awaitItem() // Loading state
            val finalState = awaitItem() // Final state after fetch

            assertFalse("Should not be loading after fetch", finalState.isLoading)
            assertNull("Error should be null on success", finalState.error)

            assertEquals(7, finalState.visibleDays.size)
            assertEquals(expectedVisibleMonday, finalState.visibleDays.first().date)
            assertEquals(expectedVisibleSunday, finalState.visibleDays.last().date)

            val schedules = finalState.medicationSchedules
            assertEquals("Expected 6 medications in the Feb 13-19 week", 6, schedules.size)

            val item1 = schedules.find { it.medication.id == med1_FullOverlap.id }
            assertNotNull(item1); item1!!
            assertEquals(0, item1.startOffsetInVisibleDays)
            assertEquals(6, item1.endOffsetInVisibleDays)
            assertTrue(item1.isOngoingOverall) // Ends Feb 28, after visible week

            val item2 = schedules.find { it.medication.id == med2_StartBeforeEndIn.id }
            assertNotNull(item2); item2!!
            assertEquals(0, item2.startOffsetInVisibleDays) // Starts Feb 10 (before Mon Feb 13)
            assertEquals(2, item2.endOffsetInVisibleDays) // Ends Feb 15 (Wed, index 2)
            assertFalse(item2.isOngoingOverall)

            val item3 = schedules.find { it.medication.id == med3_StartInEndIn.id }
            assertNotNull(item3); item3!!
            assertEquals(1, item3.startOffsetInVisibleDays) // Starts Feb 14 (Tue, index 1)
            assertEquals(3, item3.endOffsetInVisibleDays) // Ends Feb 16 (Thu, index 3)
            assertFalse(item3.isOngoingOverall)

            val item4 = schedules.find { it.medication.id == med4_StartInEndAfter.id }
            assertNotNull(item4); item4!!
            assertEquals(4, item4.startOffsetInVisibleDays) // Starts Feb 17 (Fri, index 4)
            assertEquals(6, item4.endOffsetInVisibleDays) // Ends Mar 05 (after Sun Feb 19)
            assertTrue(item4.isOngoingOverall)

            val item7 = schedules.find { it.medication.id == med7_OngoingOld.id }
            assertNotNull(item7); item7!!
            assertEquals(0, item7.startOffsetInVisibleDays)
            assertEquals(6, item7.endOffsetInVisibleDays)
            assertTrue(item7.isOngoingOverall)

            val item8 = schedules.find { it.medication.id == med8_StartInOngoing.id }
            assertNotNull(item8); item8!!
            assertEquals(3, item8.startOffsetInVisibleDays) // Starts Feb 16 (Thu, index 3)
            assertEquals(6, item8.endOffsetInVisibleDays) // Ongoing
            assertTrue(item8.isOngoingOverall)


            // Check startDateText and endDateText for one item for sanity (logic is simpler now)
            // Med3: Starts Feb 14, Ends Feb 16. Both within Feb 13-19 week.
            val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
            assertEquals("Starts ${LocalDate.of(2023,2,14).format(formatter)}", item3.startDateText)
            assertEquals("Ends ${LocalDate.of(2023,2,16).format(formatter)}", item3.endDateText)

             // Med2: Starts Feb 10 (before week), Ends Feb 15 (in week)
            assertNull("Starts text should be null if start date is outside visible week but not way before", item2.startDateText) // Or specific text like "Ongoing in week"
            assertEquals("Ends ${LocalDate.of(2023,2,15).format(formatter)}", item2.endDateText)

        }
    }

    @Test
    fun `fetchMedicationSchedulesForVisibleDays handles no medications`() = runTest {
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(emptyList()))
        `when`(medicationScheduleRepository.getAllSchedules()).thenReturn(flowOf(emptyList()))

        // Need a new ViewModel instance for this specific mock setup to take effect cleanly from init
        val localViewModel = CalendarViewModel(medicationRepository, medicationScheduleRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        localViewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertTrue(finalState.medicationSchedules.isEmpty())
        }
    }

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
        // Use a valid flow for medications, but error for schedules
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(allMedications.subList(0,1)))
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
