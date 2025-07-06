package com.d4viddf.medicationreminder.viewmodel

import app.cash.turbine.test
import com.d4viddf.medicationreminder.data.*
import com.d4viddf.medicationreminder.logic.ReminderCalculator // Actual object, not mocked
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Context
import app.cash.turbine.test
import com.d4viddf.medicationreminder.data.*
import com.d4viddf.medicationreminder.logic.ReminderCalculator // Actual object, not mocked
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse // Added for boolean checks
import org.junit.Assert.assertNull // Added for nullable checks
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyInt // For anyInt()
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MedicationViewModelTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    @Mock
    private lateinit var medicationRepository: MedicationRepository

    @Mock
    private lateinit var reminderRepository: MedicationReminderRepository

    @Mock
    private lateinit var scheduleRepository: MedicationScheduleRepository

    @Mock // Mock for Context
    private lateinit var mockContext: Context

    private lateinit var viewModel: MedicationViewModel

    private val dateStorableFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeStorableFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    private val storableDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME


    private val testMedId = 1

    private val sampleMedications = listOf(
        Medication(id = 1, name = "Amoxicillin", dosage = "250mg", startDate = "01/01/2023"),
        Medication(id = 2, name = "Ibuprofen", dosage = "200mg", startDate = "01/01/2023"),
        Medication(id = 3, name = "Metformin", dosage = "500mg", startDate = "01/01/2023"),
        Medication(id = 4, name = "Amlodipine", dosage = "5mg", startDate = "01/01/2023")
    )

    @Before
    fun setUp() {
        // Default mock for getAllMedications, can be overridden in specific tests
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(emptyList()))
        `when`(reminderRepository.getRemindersForMedication(anyInt())).thenReturn(flowOf(emptyList()))
        `when`(scheduleRepository.getSchedulesForMedication(anyInt())).thenReturn(flowOf(emptyList()))
        `when`(medicationRepository.getMedicationById(anyInt())).thenReturn(null)
        // mockContext is already declared with @Mock, it will be initialized by MockitoJUnitRunner
        // No need for: mockContext = org.mockito.Mockito.mock(Context::class.java)

        viewModel = MedicationViewModel(medicationRepository, reminderRepository, scheduleRepository, mockContext)
    }

    // Updated createMedication to align with current Medication data class structure
    // and provide defaults for new fields for existing tests.
    private fun createMedication(
        id: Int = testMedId,
        name: String = "TestMed $id",
        startDate: String,
        endDate: String? = null,
        reminderTime: String? = null, // Added with default
        color: String = "DEFAULT_COLOR", // Added with default
        typeId: Int? = 1, // Added with default
        dosage: String? = "1 pill", // Ensured it's present
        packageSize: Int = 0, // Added with default
        remainingDoses: Int = 0, // Added with default
        isFutureDose: Boolean = false // Added with default
    ): Medication {
        return Medication(
            id = id,
            name = name,
            dosage = dosage,
            startDate = startDate,
            endDate = endDate,
            reminderTime = reminderTime,
            color = color,
            typeId = typeId,
            packageSize = packageSize,
            remainingDoses = remainingDoses,
            isFutureDose = isFutureDose
            // Assuming 'notes' is no longer part of Medication data class
            // registrationDate and nregistro can use their defaults from data class
        )
    }

    // Specific helper for future dose tests, more explicit about required fields for clarity
    private fun createMedicationForFutureTest(
        id: Int,
        name: String,
        reminderTime: String?,
        color: String = "Blue",
        typeId: Int? = 1,
        dosage: String? = "1 pill",
        packageSize: Int = 30,
        remainingDoses: Int = 30,
        startDate: String? = "01/01/2023",
        endDate: String? = null
    ): Medication {
        return Medication(
            id = id, name = name, reminderTime = reminderTime, color = color, typeId = typeId,
            dosage = dosage, packageSize = packageSize, remainingDoses = remainingDoses,
            startDate = startDate, endDate = endDate
            // isFutureDose is calculated by ViewModel
        )
    }

    private fun createSchedule(
        medId: Int = testMedId,
        scheduleType: ScheduleType,
        intervalHours: Int? = null,
        intervalMinutes: Int? = null,
        intervalStartTime: String? = null, // Using String to match data class
        specificTimes: List<LocalTime>? = null // e.g., listOf(LocalTime.of(8,0), LocalTime.of(12,0))
    ): MedicationSchedule {
        return MedicationSchedule(
            id = medId * 10, // ensure unique schedule id
            medicationId = medId,
            scheduleType = scheduleType,
            intervalHours = intervalHours,
            intervalMinutes = intervalMinutes,
            intervalStartTime = intervalStartTime,
            specificTimes = specificTimes,
            daysOfWeek = null, // Not testing this aspect here
            intervalEndTime = null // Assuming full day for daily repeating intervals for simplicity
        )
    }

    private fun createReminder(
        id: Int,
        medId: Int = testMedId,
        reminderTime: LocalDateTime,
        isTaken: Boolean
    ): MedicationReminder {
        return MedicationReminder(
            id = id,
            medicationId = medId,
            medicationScheduleId = medId * 10,
            reminderTime = reminderTime.format(storableDateTimeFormatter),
            isTaken = isTaken,
            takenAt = if (isTaken) reminderTime.plusMinutes(1).format(storableDateTimeFormatter) else null,
            notificationId = id
        )
    }

    @Test
    fun `calculateAndSetDailyProgressDetails - Continuous Interval (Type B) - Correct TotalDoses`() = runTest {
        val today = LocalDate.now()
        val medicationStartDate = today.minusDays(1) // Started yesterday
        val medication = createMedication(startDate = medicationStartDate.format(dateStorableFormatter))
        // Continuous 7-hour interval, intervalStartTime = null
        val schedule = createSchedule(
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = 7,
            intervalStartTime = null
        )

        // Mock repository responses
        `when`(medicationRepository.getMedicationById(testMedId)).thenReturn(medication)
        `when`(scheduleRepository.getSchedulesForMedication(testMedId)).thenReturn(flowOf(listOf(schedule)))

        // Simulate taken reminders for today
        // D1 (yesterday): 00:00, 07:00, 14:00, 21:00
        // D2 (today):    04:00, 11:00, 18:00
        val remindersForToday = listOf(
            createReminder(1, testMedId, today.atTime(4,0), isTaken = true),
            createReminder(2, testMedId, today.atTime(11,0), isTaken = false)
            // 18:00 reminder not yet occurred or not taken
        )
        `when`(reminderRepository.getRemindersForMedication(testMedId)).thenReturn(flowOf(remindersForToday))

        viewModel.medicationProgressDetails.test {
            viewModel.observeMedicationAndRemindersForDailyProgress(testMedId)

            val progressItem = awaitItem() // Initial null
            val updatedProgress = awaitItem() // Progress after calculation

            assertNotNull(updatedProgress)
            assertEquals("Total doses for continuous interval Type B", 3, updatedProgress!!.totalFromPackage) // totalFromPackage is used for totalDosesScheduledToday
            assertEquals("Taken doses", 1, updatedProgress.taken)
            assertEquals("Remaining doses", 2, updatedProgress.remaining)
            assertEquals(1f/3f, updatedProgress.progressFraction, 0.001f)
            assertEquals("1 / 3", updatedProgress.displayText)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `calculateAndSetDailyProgressDetails - Daily Repeating Interval (Type A) - Correct TotalDoses`() = runTest {
        val today = LocalDate.now()
        val medicationStartDate = today.minusDays(1)
        val medication = createMedication(startDate = medicationStartDate.format(dateStorableFormatter))
        // Daily repeating 4-hour interval, starting 08:00
        val schedule = createSchedule(
            scheduleType = ScheduleType.INTERVAL,
            intervalHours = 4,
            intervalStartTime = LocalTime.of(8,0).format(timeStorableFormatter) // "08:00"
        )

        `when`(medicationRepository.getMedicationById(testMedId)).thenReturn(medication)
        `when`(scheduleRepository.getSchedulesForMedication(testMedId)).thenReturn(flowOf(listOf(schedule)))

        // Simulate taken reminders for today
        // Expected for today: 08:00, 12:00, 16:00, 20:00
        val remindersForToday = listOf(
            createReminder(1, testMedId, today.atTime(8,0), isTaken = true),
            createReminder(2, testMedId, today.atTime(12,0), isTaken = true),
            createReminder(3, testMedId, today.atTime(16,0), isTaken = false)
            // 20:00 reminder not yet occurred or not taken
        )
        `when`(reminderRepository.getRemindersForMedication(testMedId)).thenReturn(flowOf(remindersForToday))


        viewModel.medicationProgressDetails.test {
            viewModel.observeMedicationAndRemindersForDailyProgress(testMedId)

            val progressItem = awaitItem() // Initial null
            val updatedProgress = awaitItem() // Progress after calculation

            assertNotNull(updatedProgress)
            assertEquals("Total doses for daily repeating interval Type A", 4, updatedProgress!!.totalFromPackage)
            assertEquals("Taken doses", 2, updatedProgress.taken)
            assertEquals("Remaining doses", 2, updatedProgress.remaining)
            assertEquals(2f/4f, updatedProgress.progressFraction, 0.001f)
            assertEquals("2 / 4", updatedProgress.displayText)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `calculateAndSetDailyProgressDetails - DAILY type - Correct TotalDoses`() = runTest {
        val today = LocalDate.now()
        val medicationStartDate = today.minusDays(1)
        val medication = createMedication(startDate = medicationStartDate.format(dateStorableFormatter))
        // DAILY schedule, one time at 09:00
        val schedule = createSchedule(
            scheduleType = ScheduleType.DAILY,
            specificTimes = listOf(LocalTime.of(9,0)) // "09:00"
        )

        `when`(medicationRepository.getMedicationById(testMedId)).thenReturn(medication)
        `when`(scheduleRepository.getSchedulesForMedication(testMedId)).thenReturn(flowOf(listOf(schedule)))
        `when`(reminderRepository.getRemindersForMedication(testMedId)).thenReturn(flowOf(emptyList())) // No doses taken yet

        viewModel.medicationProgressDetails.test {
            viewModel.observeMedicationAndRemindersForDailyProgress(testMedId)

            val progressItem = awaitItem()
            val updatedProgress = awaitItem()

            assertNotNull(updatedProgress)
            assertEquals("Total doses for DAILY schedule", 1, updatedProgress!!.totalFromPackage)
            assertEquals("Taken doses", 0, updatedProgress.taken)
            assertEquals("0 / 1", updatedProgress.displayText)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `calculateAndSetDailyProgressDetails - CUSTOM_ALARMS type - Correct TotalDoses`() = runTest {
        val today = LocalDate.now()
        val medicationStartDate = today.minusDays(1)
        val medication = createMedication(name = "TestMed $testMedId", startDate = medicationStartDate.format(dateStorableFormatter))
        // CUSTOM_ALARMS schedule, times at 10:00, 14:00, 18:00
        val specificTimesList = listOf(LocalTime.of(10,0), LocalTime.of(14,0), LocalTime.of(18,0))
        val schedule = createSchedule(
            scheduleType = ScheduleType.CUSTOM_ALARMS,
            specificTimes = specificTimesList
        )

        `when`(medicationRepository.getMedicationById(testMedId)).thenReturn(medication)
        `when`(scheduleRepository.getSchedulesForMedication(testMedId)).thenReturn(flowOf(listOf(schedule)))
        `when`(reminderRepository.getRemindersForMedication(testMedId)).thenReturn(flowOf(emptyList()))

        viewModel.medicationProgressDetails.test {
            viewModel.observeMedicationAndRemindersForDailyProgress(testMedId)

            val progressItem = awaitItem()
            val updatedProgress = awaitItem()

            assertNotNull(updatedProgress)
            assertEquals("Total doses for CUSTOM_ALARMS schedule", 3, updatedProgress!!.totalFromPackage)
            assertEquals("0 / 3", updatedProgress.displayText)
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Search Functionality Tests ---

    @Test
    fun `searchQuery initial state is empty`() = runTest {
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `searchResults initial state is empty`() = runTest {
        assertTrue(viewModel.searchResults.value.isEmpty())
    }

    @Test
    fun `updateSearchQuery updates searchQuery StateFlow`() = runTest {
        val testQuery = "test query"
        viewModel.updateSearchQuery(testQuery)
        assertEquals(testQuery, viewModel.searchQuery.value)
    }

    @Test
    fun `searchResults updates correctly based on searchQuery`() = runTest {
        // Override default mock for this test
        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(sampleMedications))
        // Re-initialize viewModel or ensure the flow is collected if init was already run by @Before
        // Since observeMedications and observeSearchQueryAndMedications are in init,
        // we need to re-initialize the viewModel after mocking getAllMedications for this specific test.
        viewModel = MedicationViewModel(medicationRepository, reminderRepository, scheduleRepository)


        viewModel.searchResults.test {
            // Initial state (empty because query is blank and medications might not have been processed yet by combine)
            // Depending on timing, it might be empty or full list if query is blank initially.
            // ViewModel's combine logic for blank query returns emptyList.
            assertEquals(emptyList<Medication>(), awaitItem())

            viewModel.updateSearchQuery("Amoxi")
            var results = awaitItem()
            assertEquals(1, results.size)
            assertEquals("Amoxicillin", results[0].name)

            viewModel.updateSearchQuery("iBuPrOfEn") // Case-insensitive
            results = awaitItem()
            assertEquals(1, results.size)
            assertEquals("Ibuprofen", results[0].name)

            viewModel.updateSearchQuery("NonExistent")
            results = awaitItem()
            assertTrue(results.isEmpty())

            viewModel.updateSearchQuery("A") // Matches Amoxicillin and Amlodipine
            results = awaitItem()
            assertEquals(2, results.size)
            assertTrue(results.any { it.name == "Amoxicillin" })
            assertTrue(results.any { it.name == "Amlodipine" })


            viewModel.updateSearchQuery("") // Blank query
            results = awaitItem()
            assertTrue("Search results should be empty for a blank query", results.isEmpty())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `searchResults is empty if medications list is empty`() = runTest {
        // Default setup in @Before already mocks getAllMedications to flowOf(emptyList())
        // viewModel is already initialized with this mock.

        viewModel.searchResults.test {
            assertEquals(emptyList<Medication>(), awaitItem()) // Initial state

            viewModel.updateSearchQuery("AnyQuery")
            // Since medications list is empty, results should remain empty.
            // awaitItem() might be needed if there's a re-emission, but if not, check current value.
            // If the combine operator emits an empty list immediately due to empty _medications,
            // and then emits again due to query change but still results in empty,
            // awaitItem() might catch the same empty list or a subsequent identical one.
            // To be safe, we can just assert the current value after action.
            // However, turbine expects explicit consumptions.
             assertEquals("Results should be empty when source is empty", emptyList<Medication>(), awaitItem())


            cancelAndConsumeRemainingEvents()
        }
    }

     @Test
    fun `search results are correct when medications flow updates after initial query`() = runTest {
        val dynamicMedicationsFlow = MutableStateFlow<List<Medication>>(emptyList())
        `when`(medicationRepository.getAllMedications()).thenReturn(dynamicMedicationsFlow)

        // Re-initialize viewModel to use the new dynamic flow
        viewModel = MedicationViewModel(medicationRepository, reminderRepository, scheduleRepository)

        viewModel.searchResults.test {
            assertEquals(emptyList<Medication>(), awaitItem()) // Initial: empty query, empty meds

            viewModel.updateSearchQuery("Metro")
            assertEquals(emptyList<Medication>(), awaitItem()) // Query "Metro", still empty meds

            dynamicMedicationsFlow.value = sampleMedications // Medications are emitted
            // Now searchResults should update: "Metro" should find "Metformin"
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals("Metformin", results[0].name)

            dynamicMedicationsFlow.value = emptyList() // Medications become empty again
             assertEquals(emptyList<Medication>(), awaitItem()) // Query "Metro", meds now empty


            viewModel.updateSearchQuery("") // Clear query
            // current meds are empty, query is empty
            assertEquals(emptyList<Medication>(), awaitItem())


            dynamicMedicationsFlow.value = sampleMedications // Meds repopulated
            // query is blank, so results should be empty as per current logic
            assertEquals(emptyList<Medication>(), awaitItem())


            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Tests for isFutureDose and Dose Actions (NEW TESTS) ---

    @Test
    fun `isFutureDose calculation is correct for various times`() = runTest {
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        val now = LocalDateTime.now()

        // Define times relative to 'now'
        val pastTime = now.minusHours(1).format(timeFormatter)
        val futureTime = now.plusHours(1).format(timeFormatter)
        // For current time, use a time that will parse correctly but be slightly in past after parsing to avoid flakiness
        val currentTime = now.minusMinutes(1).format(timeFormatter)


        val testMedications = listOf(
            createMedicationForFutureTest(id = 1, name = "Past Med", reminderTime = pastTime),
            createMedicationForFutureTest(id = 2, name = "Future Med", reminderTime = futureTime),
            createMedicationForFutureTest(id = 3, name = "Current Med", reminderTime = currentTime),
            createMedicationForFutureTest(id = 4, name = "Null Time Med", reminderTime = null),
            createMedicationForFutureTest(id = 5, name = "Invalid Time Med", reminderTime = "INVALID-TIME")
        )

        `when`(medicationRepository.getAllMedications()).thenReturn(flowOf(testMedications))
        // Re-initialize viewModel to ensure it picks up the new mock data via its init block
        // This is important because observeMedications() is called in init{}
        viewModel = MedicationViewModel(medicationRepository, reminderRepository, scheduleRepository, mockContext)

        viewModel.medications.test {
            val processedMedications = awaitItem() // First emission after processing

            assertEquals("Processed medications list size", 5, processedMedications.size)
            assertEquals("Past Med should not be future", false, processedMedications.find { it.id == 1 }?.isFutureDose)
            assertEquals("Future Med should be future", true, processedMedications.find { it.id == 2 }?.isFutureDose)
            assertEquals("Current Med should not be future", false, processedMedications.find { it.id == 3 }?.isFutureDose)
            assertEquals("Null Time Med should not be future", false, processedMedications.find { it.id == 4 }?.isFutureDose)
            assertEquals("Invalid Time Med should not be future", false, processedMedications.find { it.id == 5 }?.isFutureDose)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onMarkAsTakenRequested updates dialog visibility and medicationId`() = runTest {
        val testMedId = 123
        viewModel.onMarkAsTakenRequested(testMedId)

        assertTrue("Dialog should be visible after requesting mark as taken", viewModel.showMarkAsTakenConfirmationDialog.value)
        assertEquals("Medication ID for confirmation should be set", testMedId, viewModel.medicationIdForConfirmation.value)
    }

    @Test
    fun `confirmMarkAsTaken resets dialog visibility and medicationId`() = runTest {
        val testMedId = 123
        viewModel.onMarkAsTakenRequested(testMedId) // Set initial state

        // Pre-conditions
        assertTrue(viewModel.showMarkAsTakenConfirmationDialog.value)
        assertEquals(testMedId, viewModel.medicationIdForConfirmation.value)

        viewModel.confirmMarkAsTaken()

        assertFalse("Dialog should be hidden after confirming mark as taken", viewModel.showMarkAsTakenConfirmationDialog.value)
        assertNull("Medication ID for confirmation should be reset", viewModel.medicationIdForConfirmation.value)
    }

    @Test
    fun `confirmMarkAsTaken does nothing if medicationIdForConfirmation is null`() = runTest {
        // Pre-conditions
        assertNull(viewModel.medicationIdForConfirmation.value)
        assertFalse(viewModel.showMarkAsTakenConfirmationDialog.value)

        viewModel.confirmMarkAsTaken() // Action

        assertFalse("Dialog visibility should remain false", viewModel.showMarkAsTakenConfirmationDialog.value)
        assertNull("Medication ID should remain null", viewModel.medicationIdForConfirmation.value)
    }

    @Test
    fun `onSkipRequested updates dialog visibility and medicationId`() = runTest {
        val testMedId = 456
        viewModel.onSkipRequested(testMedId)

        assertTrue("Dialog should be visible after requesting skip", viewModel.showSkipConfirmationDialog.value)
        assertEquals("Medication ID for confirmation should be set", testMedId, viewModel.medicationIdForConfirmation.value)
    }

    @Test
    fun `confirmSkip resets dialog visibility and medicationId`() = runTest {
        val testMedId = 456
        viewModel.onSkipRequested(testMedId) // Set initial state
        // Pre-conditions
        assertTrue(viewModel.showSkipConfirmationDialog.value)
        assertEquals(testMedId, viewModel.medicationIdForConfirmation.value)

        viewModel.confirmSkip()

        assertFalse("Dialog should be hidden after confirming skip", viewModel.showSkipConfirmationDialog.value)
        assertNull("Medication ID for confirmation should be reset", viewModel.medicationIdForConfirmation.value)
    }

    @Test
    fun `confirmSkip does nothing if medicationIdForConfirmation is null`() = runTest {
        // Pre-conditions
        assertNull(viewModel.medicationIdForConfirmation.value)
        assertFalse(viewModel.showSkipConfirmationDialog.value)

        viewModel.confirmSkip() // Action

        assertFalse("Dialog visibility should remain false", viewModel.showSkipConfirmationDialog.value)
        assertNull("Medication ID should remain null", viewModel.medicationIdForConfirmation.value)
    }

    @Test
    fun `cancelDoseAction resets all dialog states`() = runTest {
        // Setup: make MarkAsTaken dialog active
        viewModel.onMarkAsTakenRequested(1)
        assertTrue(viewModel.showMarkAsTakenConfirmationDialog.value)
        assertEquals(1, viewModel.medicationIdForConfirmation.value)

        viewModel.cancelDoseAction()

        assertFalse("MarkAsTaken dialog should be hidden", viewModel.showMarkAsTakenConfirmationDialog.value)
        assertFalse("Skip dialog should be hidden", viewModel.showSkipConfirmationDialog.value)
        assertNull("Medication ID for confirmation should be reset", viewModel.medicationIdForConfirmation.value)

        // Setup: make Skip dialog active
        viewModel.onSkipRequested(2)
        assertTrue(viewModel.showSkipConfirmationDialog.value)
        assertEquals(2, viewModel.medicationIdForConfirmation.value)

        viewModel.cancelDoseAction()
        assertFalse("MarkAsTaken dialog should be hidden after 2nd cancel", viewModel.showMarkAsTakenConfirmationDialog.value)
        assertFalse("Skip dialog should be hidden after 2nd cancel", viewModel.showSkipConfirmationDialog.value)
        assertNull("Medication ID should be reset after 2nd cancel", viewModel.medicationIdForConfirmation.value)
    }
}
