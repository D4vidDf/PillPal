package com.d4viddf.medicationreminder.viewmodel

import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.repository.MedicationReminderRepository
import com.d4viddf.medicationreminder.repository.MedicationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        // It's important to set a specific Locale for tests that format dates/days,
        // otherwise, they might behave differently on different systems/CI.
        // Locale.setDefault(Locale.US) // Or any specific locale you want to test against.
        // For this test, we'll rely on the default behavior of TextStyle.SHORT which uses the default locale.
        // If specific locale formatting is critical, it should be passed to getDisplayName.
        // The ViewModel uses Locale.getDefault() for its dayFormatter, so tests should align.
        dayNameFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        testWeekDays = getTestWeekDays(referenceDate)
        viewModel = MedicationGraphViewModel(mockReminderRepository, mockMedicationRepository)
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
}
