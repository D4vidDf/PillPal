package com.d4viddf.medicationreminder.viewmodel

import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.data.model.ScheduleType
import com.d4viddf.medicationreminder.data.repository.MedicationScheduleRepository
import com.d4viddf.medicationreminder.ui.features.medication.schedules.AllSchedulesViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class AllSchedulesViewModelTest {

    @Mock
    lateinit var mockScheduleRepository: MedicationScheduleRepository

    private lateinit var viewModel: AllSchedulesViewModel

    // @Before is not strictly necessary here as ViewModel is instantiated per test,
    // but good practice if there were shared setup.
    // For this specific case, instantiating viewModel directly in the test is also fine.

    @Test
    fun testGetSchedules_returnsRepositoryFlow() = runTest {
        // Arrange
        val medicationId = 1
        val sampleSchedules = listOf(
import java.time.LocalTime

            MedicationSchedule(
                id = 1,
                medicationId = medicationId,
                scheduleType = ScheduleType.DAILY,
                specificTimes = listOf(LocalTime.of(8,0), LocalTime.of(14,0)),
                intervalHours = null,
                intervalMinutes = null,
                daysOfWeek = null,
                intervalStartTime = null,
                intervalEndTime = null
            ),
            MedicationSchedule(
                id = 2,
import java.time.DayOfWeek

                medicationId = medicationId,
                scheduleType = ScheduleType.WEEKLY,
                daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                specificTimes = listOf(LocalTime.of(10,0)),
                intervalHours = null,
                intervalMinutes = null,
                intervalStartTime = null,
                intervalEndTime = null
            )
        )

        // Mock the repository call
        `when`(mockScheduleRepository.getSchedulesForMedication(medicationId)).thenReturn(flowOf(sampleSchedules))

        // Instantiate the ViewModel (can also be in a @Before block if preferred)
        viewModel = AllSchedulesViewModel(mockScheduleRepository)

        // Act
        val resultFlow = viewModel.getSchedules(medicationId)
        val resultList = resultFlow.first() // Collect the first (and only in this case) emission

        // Assert
        // Verify the repository method was called with the correct medicationId
        verify(mockScheduleRepository).getSchedulesForMedication(medicationId)

        // Assert that the collected list is equal to the sampleSchedules
        assertEquals(sampleSchedules, resultList)
        assertEquals(2, resultList.size)
        assertEquals(listOf(LocalTime.of(8,0), LocalTime.of(14,0)), resultList[0].specificTimes)
    }

    @Test
    fun testGetSchedules_emptyListFromRepository() = runTest {
        // Arrange
        val medicationId = 2
        val emptySchedules = emptyList<MedicationSchedule>()

        `when`(mockScheduleRepository.getSchedulesForMedication(medicationId)).thenReturn(flowOf(emptySchedules))
        viewModel = AllSchedulesViewModel(mockScheduleRepository)

        // Act
        val resultFlow = viewModel.getSchedules(medicationId)
        val resultList = resultFlow.first()

        // Assert
        verify(mockScheduleRepository).getSchedulesForMedication(medicationId)
        assertEquals(emptySchedules, resultList)
        assertEquals(0, resultList.size)
    }
}
