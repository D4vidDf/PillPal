package com.d4viddf.medicationreminder.viewmodel

import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.repository.MedicationReminderRepository
import com.d4viddf.medicationreminder.repository.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull // For error checking
import org.junit.Assert.assertTrue // For error message checking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MedicationGraphViewModelTest {

    @Mock
    lateinit var mockReminderRepository: MedicationReminderRepository

    @Mock
    lateinit var mockMedicationRepository: MedicationRepository

    private lateinit var viewModel: MedicationGraphViewModel

    private val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher() // For runTest if needed for Dispatchers.Main

    // Reference date for consistent week generation: Monday, January 15, 2024
    private val referenceDate: LocalDate = LocalDate.of(2024, 1, 15)
    private lateinit var testWeekDays: List<LocalDate>
    private lateinit var dayNameFormatter: DateTimeFormatter


    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Set Main dispatcher for viewModelScope
        dayNameFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        testWeekDays = getTestWeekDays(referenceDate)
        viewModel = MedicationGraphViewModel(mockReminderRepository, mockMedicationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset Main dispatcher after the test
    }

    private fun getTestWeekDays(referenceDate: LocalDate): List<LocalDate> {
        val monday = referenceDate.with(DayOfWeek.MONDAY)
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    private fun getExpectedDayLabels(weekDays: List<LocalDate>): Map<String, Int> {
        val map = LinkedHashMap<String, Int>()
        weekDays.forEach { day ->
            map[day.format(dayNameFormatter)] = 0
        }
        return map
    }

    @Test
    fun `loadWeeklyGraphData processes taken reminders correctly`() = runTest(testDispatcher) {
        val medicationId = 1
        val medName = "TestMed"
        val medication = Medication(id = medicationId, name = medName, typeId = 1, color = "Blue", dosage = "1", packageSize = 30, remainingDoses = 30, startDate = null, endDate = null, reminderTime = null)

        val reminders = listOf(
            // Monday (testWeekDays[0])
            MedicationReminder(id = "1", medicationId = medicationId, reminderTime = LocalDateTime.of(testWeekDays[0], LocalTime.of(8, 0)).toString(), isTaken = true, takenAt = LocalDateTime.of(testWeekDays[0], LocalTime.of(8, 5)).toString()),
            MedicationReminder(id = "2", medicationId = medicationId, reminderTime = LocalDateTime.of(testWeekDays[0], LocalTime.of(12, 0)).toString(), isTaken = true, takenAt = LocalDateTime.of(testWeekDays[0], LocalTime.of(12, 5)).toString()),
            // Wednesday (testWeekDays[2])
            MedicationReminder(id = "3", medicationId = medicationId, reminderTime = LocalDateTime.of(testWeekDays[2], LocalTime.of(9, 0)).toString(), isTaken = true, takenAt = LocalDateTime.of(testWeekDays[2], LocalTime.of(9, 5)).toString()),
            // Outside this week
            MedicationReminder(id = "4", medicationId = medicationId, reminderTime = LocalDateTime.of(testWeekDays[0].minusWeeks(1), LocalTime.of(8, 0)).toString(), isTaken = true, takenAt = LocalDateTime.of(testWeekDays[0].minusWeeks(1), LocalTime.of(8, 5)).toString()),
            // Not taken
            MedicationReminder(id = "5", medicationId = medicationId, reminderTime = LocalDateTime.of(testWeekDays[1], LocalTime.of(8, 0)).toString(), isTaken = false, takenAt = null)
        )

        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(medication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))

        viewModel.loadWeeklyGraphData(medicationId, testWeekDays)

        assertEquals(medName, viewModel.medicationName.value)
        val expectedGraphData = getExpectedDayLabels(testWeekDays).toMutableMap()
        expectedGraphData[testWeekDays[0].format(dayNameFormatter)] = 2
        expectedGraphData[testWeekDays[2].format(dayNameFormatter)] = 1
        assertEquals(expectedGraphData, viewModel.graphData.value)
    }

    @Test
    fun `loadWeeklyGraphData no taken reminders returns zero counts`() = runTest(testDispatcher) {
        val medicationId = 2
        val medName = "NoTakeMed"
        val medication = Medication(id = medicationId, name = medName, typeId = 1, color = "Red", dosage = "1", packageSize = 30, remainingDoses = 30, startDate = null, endDate = null, reminderTime = null)

        val reminders = listOf(
            MedicationReminder(id = "1", medicationId = medicationId, reminderTime = LocalDateTime.of(testWeekDays[0], LocalTime.of(8, 0)).toString(), isTaken = false, takenAt = null),
            MedicationReminder(id = "2", medicationId = medicationId, reminderTime = LocalDateTime.of(testWeekDays[1], LocalTime.of(12, 0)).toString(), isTaken = false, takenAt = null)
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(medication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))

        viewModel.loadWeeklyGraphData(medicationId, testWeekDays)

        assertEquals(medName, viewModel.medicationName.value)
        assertEquals(getExpectedDayLabels(testWeekDays), viewModel.graphData.value)
    }

    @Test
    fun `loadWeeklyGraphData empty reminders list returns zero counts`() = runTest(testDispatcher) {
        val medicationId = 3
        val medName = "EmptyRemMed"
         val medication = Medication(id = medicationId, name = medName, typeId = 1, color = "Green", dosage = "1", packageSize = 30, remainingDoses = 30, startDate = null, endDate = null, reminderTime = null)

        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(medication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(emptyList()))

        viewModel.loadWeeklyGraphData(medicationId, testWeekDays)

        assertEquals(medName, viewModel.medicationName.value)
        assertEquals(getExpectedDayLabels(testWeekDays), viewModel.graphData.value)
    }

    @Test
    fun `loadWeeklyGraphData medication not found clears data and sets default name`() = runTest(testDispatcher) {
        val medicationId = 4
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(null)
        // No need to mock reminderRepository as it won't be called if medication is null by current VM logic,
        // but good practice to define behavior if it were.
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(emptyList()))


        viewModel.loadWeeklyGraphData(medicationId, testWeekDays)

        assertEquals("Medication $medicationId", viewModel.medicationName.value) // ViewModel's default name
        assertEquals(emptyMap<String, Int>(), viewModel.graphData.value) // Graph data should be empty
    }

    @Test
    fun `loadWeeklyGraphData reminder with invalid takenAt format is not counted`() = runTest(testDispatcher) {
        val medicationId = 5
        val medName = "InvalidDateMed"
        val medication = Medication(id = medicationId, name = medName, typeId = 1, color = "Yellow", dosage = "1", packageSize = 30, remainingDoses = 30, startDate = null, endDate = null, reminderTime = null)

        val reminders = listOf(
            MedicationReminder(id = "1", medicationId = medicationId, reminderTime = LocalDateTime.of(testWeekDays[0], LocalTime.of(8,0)).toString(), isTaken = true, takenAt = "INVALID_DATE_FORMAT"),
            MedicationReminder(id = "2", medicationId = medicationId, reminderTime = LocalDateTime.of(testWeekDays[1], LocalTime.of(9,0)).toString(), isTaken = true, takenAt = LocalDateTime.of(testWeekDays[1], LocalTime.of(9,5)).toString())
        )

        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(medication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))

        viewModel.loadWeeklyGraphData(medicationId, testWeekDays)

        assertEquals(medName, viewModel.medicationName.value)
        val expectedGraphData = getExpectedDayLabels(testWeekDays).toMutableMap()
        expectedGraphData[testWeekDays[1].format(dayNameFormatter)] = 1 // Only the valid one
        assertEquals(expectedGraphData, viewModel.graphData.value)
    }

     @Test
    fun `loadWeeklyGraphData with empty currentWeekDays list resets graphData`() = runTest(testDispatcher) {
        val medicationId = 6
        // No need to mock repository calls as the function should return early

        viewModel.loadWeeklyGraphData(medicationId, emptyList())

        assertEquals(emptyMap<String, Int>(), viewModel.graphData.value)
        // medicationName might remain from a previous call or be default, not the focus of this test
    }

    // --- Monthly Graph Data Tests ---

    private fun getExpectedMonthLabels(targetMonth: YearMonth): Map<String, Int> {
        val map = LinkedHashMap<String, Int>()
        for (day in 1..targetMonth.lengthOfMonth()) {
            map[day.toString()] = 0
        }
        return map
    }

    @Test
    fun `loadMonthlyGraphData processes taken reminders correctly`() = runTest(testDispatcher) {
        val medicationId = 10
        val medName = "MonthlyMed"
        val targetMonth = YearMonth.of(2024, 2) // February 2024
        val medication = Medication(id = medicationId, name = medName, typeId = 1, color = "Blue", dosage = "1", packageSize = 30, remainingDoses = 30)

        val reminders = listOf(
            // Feb 1st, 2024
            MedicationReminder(id = "m1", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 2, 1, 8, 0).toString(), isTaken = true, takenAt = LocalDateTime.of(2024, 2, 1, 8, 5).toString()),
            MedicationReminder(id = "m2", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 2, 1, 12, 0).toString(), isTaken = true, takenAt = LocalDateTime.of(2024, 2, 1, 12, 5).toString()),
            // Feb 15th, 2024
            MedicationReminder(id = "m3", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 2, 15, 9, 0).toString(), isTaken = true, takenAt = LocalDateTime.of(2024, 2, 15, 9, 5).toString()),
            // Jan 31st, 2024 (Outside target month)
            MedicationReminder(id = "m4", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 1, 31, 8, 0).toString(), isTaken = true, takenAt = LocalDateTime.of(2024, 1, 31, 8, 5).toString()),
            // Feb 5th, 2024 (Not taken)
            MedicationReminder(id = "m5", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 2, 5, 8, 0).toString(), isTaken = false, takenAt = null)
        )

        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(medication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))

        viewModel.loadMonthlyGraphData(medicationId, targetMonth)
        advanceUntilIdle()

        assertEquals(medName, viewModel.medicationName.value)
        val expectedGraphData = getExpectedMonthLabels(targetMonth).toMutableMap()
        expectedGraphData["1"] = 2 // Day 1 of Feb
        expectedGraphData["15"] = 1 // Day 15 of Feb
        assertEquals(expectedGraphData, viewModel.graphData.value)
        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `loadMonthlyGraphData no taken reminders in month returns zero counts`() = runTest(testDispatcher) {
        val medicationId = 11
        val medName = "NoTakeMonthMed"
        val targetMonth = YearMonth.of(2024, 3)
        val medication = Medication(id = medicationId, name = medName, typeId = 1, color = "Green", dosage = "1", packageSize = 30, remainingDoses = 30)
        val reminders = listOf(
            MedicationReminder(id = "m1", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 3, 1, 8,0).toString(), isTaken = false, takenAt = null)
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(medication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))

        viewModel.loadMonthlyGraphData(medicationId, targetMonth)
        advanceUntilIdle()

        assertEquals(medName, viewModel.medicationName.value)
        assertEquals(getExpectedMonthLabels(targetMonth), viewModel.graphData.value)
    }

    // --- Yearly Graph Data Tests ---

    private fun getExpectedYearLabels(): Map<String, Int> {
        val map = LinkedHashMap<String, Int>()
        (1..12).forEach { monthNum ->
            map[java.time.Month.of(monthNum).getDisplayName(TextStyle.SHORT, Locale.getDefault())] = 0
        }
        return map
    }

    @Test
    fun `loadYearlyGraphData processes taken reminders correctly`() = runTest(testDispatcher) {
        val medicationId = 20
        val medName = "YearlyMed"
        val targetYear = 2024
        val medication = Medication(id = medicationId, name = medName, typeId = 1, color = "Purple", dosage = "1", packageSize = 30, remainingDoses = 30)

        val reminders = listOf(
            MedicationReminder(id = "y1", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 1, 10, 8,0).toString(), isTaken = true, takenAt = LocalDateTime.of(2024, 1, 10, 8,5).toString()), // Jan
            MedicationReminder(id = "y2", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 1, 15, 8,0).toString(), isTaken = true, takenAt = LocalDateTime.of(2024, 1, 15, 8,5).toString()), // Jan
            MedicationReminder(id = "y3", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 3, 5, 8,0).toString(), isTaken = true, takenAt = LocalDateTime.of(2024, 3, 5, 8,5).toString()),  // Mar
            MedicationReminder(id = "y4", medicationId = medicationId, reminderTime = LocalDateTime.of(2023, 12, 20, 8,0).toString(), isTaken = true, takenAt = LocalDateTime.of(2023, 12, 20, 8,5).toString()), // Other year
            MedicationReminder(id = "y5", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 4, 10, 8,0).toString(), isTaken = false, takenAt = null) // Not taken
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(medication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))

        viewModel.loadYearlyGraphData(medicationId, targetYear)
        advanceUntilIdle()

        assertEquals(medName, viewModel.medicationName.value)
        val expectedGraphData = getExpectedYearLabels().toMutableMap()
        expectedGraphData[java.time.Month.JANUARY.getDisplayName(TextStyle.SHORT, Locale.getDefault())] = 2
        expectedGraphData[java.time.Month.MARCH.getDisplayName(TextStyle.SHORT, Locale.getDefault())] = 1
        assertEquals(expectedGraphData, viewModel.graphData.value)
        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `loadYearlyGraphData no taken reminders in year returns zero counts`() = runTest(testDispatcher) {
        val medicationId = 21
        val medName = "NoTakeYearMed"
        val targetYear = 2024
        val medication = Medication(id = medicationId, name = medName, typeId = 1, color = "Orange", dosage = "1", packageSize = 30, remainingDoses = 30)
        val reminders = listOf(
            MedicationReminder(id = "y1", medicationId = medicationId, reminderTime = LocalDateTime.of(2024, 1, 10, 8,0).toString(), isTaken = false, takenAt = null)
        )
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(medication)
        `when`(mockReminderRepository.getRemindersForMedication(medicationId)).thenReturn(flowOf(reminders))

        viewModel.loadYearlyGraphData(medicationId, targetYear)
        advanceUntilIdle()

        assertEquals(medName, viewModel.medicationName.value)
        assertEquals(getExpectedYearLabels(), viewModel.graphData.value)
    }
}
