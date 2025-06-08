package com.d4viddf.medicationreminder.viewmodel

import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
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
            MedicationSchedule(
                id = 1,
                medicationId = medicationId,
                scheduleType = ScheduleType.DAILY,
                specificTimes = "08:00,14:00",
                intervalHours = null,
                intervalMinutes = null,
                daysOfWeek = null,
                intervalStartTime = null,
                intervalEndTime = null
            ),
            MedicationSchedule(
                id = 2,
                medicationId = medicationId,
                scheduleType = ScheduleType.WEEKLY,
                daysOfWeek = "1,3,5",
                specificTimes = "10:00",
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
        assertEquals("08:00,14:00", resultList[0].specificTimes)
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
