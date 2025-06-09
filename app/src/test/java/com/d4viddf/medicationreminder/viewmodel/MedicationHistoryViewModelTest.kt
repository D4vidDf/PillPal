package com.d4viddf.medicationreminder.viewmodel

import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationHistoryEntry
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.repository.MedicationReminderRepository
import com.d4viddf.medicationreminder.repository.MedicationRepository
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
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MedicationHistoryViewModelTest {

    @Mock
    private lateinit var mockReminderRepository: MedicationReminderRepository

    @Mock
    private lateinit var mockMedicationRepository: MedicationRepository

    private lateinit var viewModel: MedicationHistoryViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val medicationId = 1
    private val medicationName = "TestMed"
    private val sampleMedication = Medication(id = medicationId, name = medicationName, nregistro = "123", typeId = 1, color = "Blue", dosage = "10mg", packageSize = 30, remainingDoses = 30)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MedicationHistoryViewModel(mockReminderRepository, mockMedicationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createReminder(id: String, dateTime: LocalDateTime, isTaken: Boolean, takenAtSuffix: String? = null): MedicationReminder {
        val takenAtStr = if (takenAtSuffix == "valid") dateTime.plusMinutes(5).toString() else takenAtSuffix
        return MedicationReminder(
            id = id,
            medicationId = medicationId,
            reminderTime = dateTime.toString(),
            isTaken = isTaken,
            takenAt = takenAtStr,
            medicationScheduleId = 1 // Assuming a valid schedule ID
        )
    }

    // --- loadInitialHistory Tests ---
    @Test
    fun `loadInitialHistory success fetches filters sorts and transforms data`() = runTest {
        val reminders = listOf(
            createReminder("r1", LocalDateTime.of(2023, 10, 10, 8, 0), true, "valid"), // Kept
            createReminder("r2", LocalDateTime.of(2023, 10, 9, 10, 0), true, "valid"),  // Kept
            createReminder("r3", LocalDateTime.of(2023, 10, 10, 12, 0), false, null),    // Filtered out (not taken)
            createReminder("r4", LocalDateTime.of(2023, 10, 8, 10, 0), true, null)       // Filtered out (takenAt null)
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))

        viewModel.loadInitialHistory(medicationId)
        advanceUntilIdle()

        assertEquals(medicationName, viewModel.medicationName.value)
        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.error.value)
        assertEquals(2, viewModel.filteredAndSortedHistory.value.size)
        // Default sort is descending
        assertEquals("r1", viewModel.filteredAndSortedHistory.value[0].id) // 2023-10-10 08:05
        assertEquals("r2", viewModel.filteredAndSortedHistory.value[1].id) // 2023-10-09 10:05
    }

    @Test
    fun `loadInitialHistory medicationNotFound setsError`() = runTest {
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(null)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(emptyList()))


        viewModel.loadInitialHistory(medicationId)
        advanceUntilIdle()

        assertEquals("Medication $medicationId", viewModel.medicationName.value) // Default name
        assertEquals(false, viewModel.isLoading.value)
        assertNotNull(viewModel.error.value) // ViewModel doesn't set error for this, but rawHistory will be empty
        assertTrue(viewModel.filteredAndSortedHistory.value.isEmpty())
    }

    @Test
    fun `loadInitialHistory noReminders isEmptyAndNoError`() = runTest {
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(emptyList()))

        viewModel.loadInitialHistory(medicationId)
        advanceUntilIdle()

        assertEquals(medicationName, viewModel.medicationName.value)
        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.error.value)
        assertTrue(viewModel.filteredAndSortedHistory.value.isEmpty())
    }

    @Test
    fun `loadInitialHistory noTakenReminders isEmptyAndNoError`() = runTest {
        val reminders = listOf(
            createReminder("r1", LocalDateTime.of(2023,10,10,8,0), false, null)
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))

        viewModel.loadInitialHistory(medicationId)
        advanceUntilIdle()

        assertTrue(viewModel.filteredAndSortedHistory.value.isEmpty())
    }

    // --- setDateFilter Tests ---
    @Test
    fun `setDateFilter withStartDateOnly filtersCorrectly`() = runTest {
        val startDate = LocalDate.of(2023, 10, 10)
        val reminders = listOf(
            createReminder("r1", LocalDateTime.of(2023, 10, 10, 8, 0), true, "valid"), // Kept
            createReminder("r2", LocalDateTime.of(2023, 10, 9, 10, 0), true, "valid")   // Filtered out
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))
        viewModel.loadInitialHistory(medicationId)
        advanceUntilIdle() // Initial load

        viewModel.setDateFilter(startDate, null)
        advanceUntilIdle() // Process filter

        assertEquals(1, viewModel.filteredAndSortedHistory.value.size)
        assertEquals("r1", viewModel.filteredAndSortedHistory.value[0].id)
    }

    @Test
    fun `setDateFilter withEndDateOnly_filtersCorrectly`() = runTest {
        val endDate = LocalDate.of(2023, 10, 9)
         val reminders = listOf(
            createReminder("r1", LocalDateTime.of(2023, 10, 10, 8, 0), true, "valid"), // Filtered out
            createReminder("r2", LocalDateTime.of(2023, 10, 9, 10, 0), true, "valid")   // Kept
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))
        viewModel.loadInitialHistory(medicationId)
        advanceUntilIdle()

        viewModel.setDateFilter(null, endDate)
        advanceUntilIdle()

        assertEquals(1, viewModel.filteredAndSortedHistory.value.size)
        assertEquals("r2", viewModel.filteredAndSortedHistory.value[0].id)
    }

    @Test
    fun `setDateFilter withStartAndEndDate_filtersCorrectly`() = runTest {
        val startDate = LocalDate.of(2023, 10, 9)
        val endDate = LocalDate.of(2023, 10, 11)
         val reminders = listOf(
            createReminder("r1", LocalDateTime.of(2023, 10, 10, 8, 0), true, "valid"), // Kept
            createReminder("r2", LocalDateTime.of(2023, 10, 9, 10, 0), true, "valid"),  // Kept
            createReminder("r3", LocalDateTime.of(2023, 10, 12, 8, 0), true, "valid"), // Filtered out (after end)
            createReminder("r4", LocalDateTime.of(2023, 10, 8, 8, 0), true, "valid")   // Filtered out (before start)
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))
        viewModel.loadInitialHistory(medicationId)
        advanceUntilIdle()

        viewModel.setDateFilter(startDate, endDate)
        advanceUntilIdle()

        assertEquals(2, viewModel.filteredAndSortedHistory.value.size)
        // Default sort is descending
        assertEquals("r1", viewModel.filteredAndSortedHistory.value[0].id)
        assertEquals("r2", viewModel.filteredAndSortedHistory.value[1].id)
    }

    @Test
    fun `setDateFilter clearFilter showsAllTakenHistory`() = runTest {
         val reminders = listOf(
            createReminder("r1", LocalDateTime.of(2023, 10, 10, 8, 0), true, "valid"),
            createReminder("r2", LocalDateTime.of(2023, 10, 9, 10, 0), true, "valid")
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))
        viewModel.loadInitialHistory(medicationId)
        advanceUntilIdle()

        viewModel.setDateFilter(LocalDate.of(2023,10,10), LocalDate.of(2023,10,10)) // Apply a filter first
        advanceUntilIdle()
        assertEquals(1, viewModel.filteredAndSortedHistory.value.size)

        viewModel.setDateFilter(null, null) // Clear filter
        advanceUntilIdle()
        assertEquals(2, viewModel.filteredAndSortedHistory.value.size)
    }

    // --- setSortOrder Tests ---
    @Test
    fun `setSortOrder ascending sortsCorrectly`() = runTest {
        val reminders = listOf(
            createReminder("r1", LocalDateTime.of(2023, 10, 10, 8, 0), true, "valid"), // Later
            createReminder("r2", LocalDateTime.of(2023, 10, 9, 10, 0), true, "valid")   // Earlier
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))
        viewModel.loadInitialHistory(medicationId) // Loads with default descending
        advanceUntilIdle()

        viewModel.setSortOrder(true) // Set to ascending
        advanceUntilIdle()

        assertEquals(2, viewModel.filteredAndSortedHistory.value.size)
        assertEquals("r2", viewModel.filteredAndSortedHistory.value[0].id) // Earlier first
        assertEquals("r1", viewModel.filteredAndSortedHistory.value[1].id) // Later second
    }

    @Test
    fun `setSortOrder descending sortsCorrectlyAfterAscending`() = runTest {
        val reminders = listOf(
            createReminder("r1", LocalDateTime.of(2023, 10, 10, 8, 0), true, "valid"), // Later
            createReminder("r2", LocalDateTime.of(2023, 10, 9, 10, 0), true, "valid")   // Earlier
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))
        viewModel.loadInitialHistory(medicationId)
        viewModel.setSortOrder(true) // Set to ascending
        advanceUntilIdle()

        viewModel.setSortOrder(false) // Set back to descending
        advanceUntilIdle()

        assertEquals(2, viewModel.filteredAndSortedHistory.value.size)
        assertEquals("r1", viewModel.filteredAndSortedHistory.value[0].id) // Later first
        assertEquals("r2", viewModel.filteredAndSortedHistory.value[1].id) // Earlier second
    }
}
