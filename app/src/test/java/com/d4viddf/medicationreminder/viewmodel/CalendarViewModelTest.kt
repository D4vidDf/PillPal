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

    private val medication1 = Medication(id = 1, name = "Med A", color = "ColorA", startDate = "2023-01-15", endDate = "2023-03-10", packageSize = 1, remainingDoses = 1)
    private val schedule1 = MedicationSchedule(id = 1, medicationId = 1, scheduleType = ScheduleType.DAILY, intervalHours = null, intervalMinutes = null, daysOfWeek = null, specificTimes = null, intervalStartTime = null, intervalEndTime = null)

    private val medication2 = Medication(id = 2, name = "Med B", color = "ColorB", startDate = "2023-02-05", endDate = null, packageSize = 1, remainingDoses = 1) // Ongoing
    private val schedule2 = MedicationSchedule(id = 2, medicationId = 2, scheduleType = ScheduleType.WEEKLY, intervalHours = null, intervalMinutes = null, daysOfWeek = null, specificTimes = null, intervalStartTime = null, intervalEndTime = null)

    private val medication3 = Medication(id = 3, name = "Med C", color = "ColorC", startDate = "2023-02-20", endDate = "2023-02-25", packageSize = 1, remainingDoses = 1) // Short, within Feb
    private val schedule3 = MedicationSchedule(id = 3, medicationId = 3, scheduleType = ScheduleType.DAILY, intervalHours = null, intervalMinutes = null, daysOfWeek = null, specificTimes = null, intervalStartTime = null, intervalEndTime = null)


    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(listOf(medication1, medication2, medication3)))
        `when`(medicationScheduleRepository.getAllSchedules()).thenReturn(flowOf(listOf(schedule1, schedule2, schedule3)))

        viewModel = CalendarViewModel(medicationRepository, medicationScheduleRepository)
        // Advance past the initial loading triggered by init block
        testDispatcher.scheduler.advanceUntilIdle() // Ensures init{}'s setSelectedDate and fetch completes
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct after init`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem() // State after init has finished loading
            val today = LocalDate.now()
            assertEquals(today, initialState.selectedDate)
            assertEquals(YearMonth.from(today), initialState.currentMonth)
            assertFalse(initialState.isLoading)
            assertNull(initialState.error)
            assertEquals(YearMonth.from(today).lengthOfMonth(), initialState.daysInMonth.size)
        }
    }

    @Test
    fun `setSelectedDate updates date, month, and schedules correctly`() = runTest {
        val testDate = LocalDate.of(2023, 2, 15)

        viewModel.uiState.test {
            awaitItem() // Current state after init

            viewModel.setSelectedDate(testDate)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertEquals(YearMonth.of(2023, 2), loadingState.currentMonth)

            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertEquals(testDate, finalState.selectedDate)
            assertEquals(YearMonth.of(2023, 2), finalState.currentMonth)
            assertEquals(28, finalState.daysInMonth.size) // Feb 2023 has 28 days
            assertTrue(finalState.daysInMonth.any { it.date == testDate && it.isSelected })
            assertEquals(3, finalState.medicationSchedules.size) // Check schedules for Feb 2023
        }
    }

    @Test
    fun `onNextMonth updates month and schedules correctly`() = runTest {
        val initialDate = LocalDate.of(2023, 1, 10)
        viewModel.setSelectedDate(initialDate)
        testDispatcher.scheduler.advanceUntilIdle() // Ensure this setSelectedDate completes before proceeding

        viewModel.uiState.test {
            awaitItem() // State after setSelectedDate(initialDate)

            viewModel.onNextMonth()

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertEquals(YearMonth.of(2023, 2), loadingState.currentMonth)
            assertEquals(LocalDate.of(2023,2,1), loadingState.selectedDate)

            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertEquals(YearMonth.of(2023, 2), finalState.currentMonth)
            assertEquals(LocalDate.of(2023,2,1), finalState.selectedDate)
            assertEquals(3, finalState.medicationSchedules.size)
        }
    }

    @Test
    fun `onPreviousMonth updates month and schedules correctly`() = runTest {
        val initialDate = LocalDate.of(2023, 3, 10)
        viewModel.setSelectedDate(initialDate)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // State after setSelectedDate(initialDate)

            viewModel.onPreviousMonth()

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertEquals(YearMonth.of(2023, 2), loadingState.currentMonth)
            assertEquals(LocalDate.of(2023,2,1), loadingState.selectedDate)

            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertEquals(YearMonth.of(2023, 2), finalState.currentMonth)
            assertEquals(LocalDate.of(2023,2,1), finalState.selectedDate)
            assertEquals(3, finalState.medicationSchedules.size)
        }
    }

    @Test
    fun `fetchMedicationSchedulesForMonth filters and formats correctly for February 2023`() = runTest {
        val targetMonth = YearMonth.of(2023, 2)
        // The viewModel is already initialized by @Before, and its initial fetch is done.
        // We are testing the effect of a new setSelectedDate call.

        viewModel.uiState.test {
            val initialStateFromInit = awaitItem() // Consume the state from @Before's init

            viewModel.setSelectedDate(targetMonth.atDay(1))

            val loadingState = awaitItem()
            assertTrue("Should be loading after date set", loadingState.isLoading)
            assertEquals("Month should update in loading state", targetMonth, loadingState.currentMonth)

            val finalState = awaitItem()
            assertFalse("Should not be loading after fetch", finalState.isLoading)
            assertNull("Error should be null on success", finalState.error)
            assertEquals("Should have 3 schedules for Feb 2023", 3, finalState.medicationSchedules.size)

            val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

            val item1 = finalState.medicationSchedules.find { it.medication.id == 1 } // MedA (Jan 15 - Mar 10)
            assertNotNull("MedA should be present", item1)
            assertNull("MedA startDateText should be null for Feb", item1!!.startDateText)
            assertNull("MedA endDateText should be null for Feb", item1.endDateText)
            assertTrue("MedA should be ongoing in Feb", item1.isOngoing)

            val item2 = finalState.medicationSchedules.find { it.medication.id == 2 } // MedB (Feb 5 - Ongoing)
            assertNotNull("MedB should be present", item2)
            assertEquals("Starts ${LocalDate.of(2023,2,5).format(formatter)}", item2!!.startDateText)
            assertNull("MedB endDateText should be null (ongoing)", item2.endDateText)
            assertFalse("MedB starts in Feb, so not 'isOngoing' from prior month", item2.isOngoing)

            val item3 = finalState.medicationSchedules.find { it.medication.id == 3 } // MedC (Feb 20 - Feb 25)
            assertNotNull("MedC should be present", item3)
            assertEquals("Starts ${LocalDate.of(2023,2,20).format(formatter)}", item3!!.startDateText)
            assertEquals("Ends ${LocalDate.of(2023,2,25).format(formatter)}", item3.endDateText)
            assertFalse("MedC starts in Feb, so not 'isOngoing' from prior month", item3.isOngoing)
        }
    }

    @Test
    fun `fetchMedicationSchedulesForMonth handles no medications`() = runTest {
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(emptyList()))
        `when`(medicationScheduleRepository.getAllSchedules()).thenReturn(flowOf(emptyList()))

        val localViewModel = CalendarViewModel(medicationRepository, medicationScheduleRepository)
        // init calls setSelectedDate(today), advance scheduler to let it finish
        testDispatcher.scheduler.advanceUntilIdle()

        localViewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertTrue(finalState.medicationSchedules.isEmpty())
        }
    }

    @Test
    fun `fetchMedicationSchedulesForMonth handles repository error for medications`() = runTest {
        val errorMessage = "Medication Database error"
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf { throw RuntimeException(errorMessage) })
        // medicationScheduleRepository mock is still the default one from @Before

        val localViewModel = CalendarViewModel(medicationRepository, medicationScheduleRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        localViewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertTrue("Error message should contain: $errorMessage", finalState.error?.contains(errorMessage) ?: false)
        }
    }

    @Test
    fun `fetchMedicationSchedulesForMonth handles repository error for schedules`() = runTest {
        val errorMessage = "Schedule Database error"
        // Use default medicationRepository mock from @Before
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(listOf(medication1))) // Ensure combine has something to work with
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
