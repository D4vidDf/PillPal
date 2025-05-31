package com.d4viddf.medicationreminder.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.TodayScheduleItem
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class MainCoroutineRule(
    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}

@ExperimentalCoroutinesApi
class MedicationReminderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var mockReminderRepository: MedicationReminderRepository

    @MockK
    private lateinit var mockContext: Context // Required by ViewModel constructor

    private lateinit var viewModel: MedicationReminderViewModel

    private val testMedicationId = 1
    private val testMedicationName = "Medication $testMedicationId"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        // Mock WorkManager related calls within context if necessary, for now, keep it simple
        val mockWorkManager = mockk<androidx.work.WorkManager>(relaxed = true)
        coEvery { mockContext.applicationContext } returns mockContext // Return mockContext for applicationContext
        coEvery { mockContext.getSystemService(Context.WORK_SERVICE) } returns mockWorkManager // Mock getSystemService
        coEvery { mockContext.packageName } returns "com.d4viddf.medicationreminder.test"


        viewModel = MedicationReminderViewModel(mockReminderRepository, mockContext)
    }

    @After
    fun tearDown() {
        // Clean up
    }

    // --- Tests for loadTodaySchedule ---

    @Test
    fun `loadTodaySchedule_usesPlaceholderLogic_emitsCorrectlyMappedList`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        // Since loadTodaySchedule currently uses hardcoded placeholder logic,
        // this test will verify that placeholder logic.
        // No repository mocking is needed for this specific test as per current VM implementation.

        viewModel.loadTodaySchedule(testMedicationId)

        val expectedItems = listOf(
            TodayScheduleItem(
                id = "${testMedicationId}_0900",
                medicationName = testMedicationName,
                time = LocalTime.of(9, 0),
                isPast = LocalTime.now().isAfter(LocalTime.of(9, 0)),
                isTaken = false,
                underlyingReminderId = 1L
            ),
            TodayScheduleItem(
                id = "${testMedicationId}_1500",
                medicationName = testMedicationName,
                time = LocalTime.of(15, 0),
                isPast = LocalTime.now().isAfter(LocalTime.of(15, 0)),
                isTaken = true,
                underlyingReminderId = 2L
            ),
            TodayScheduleItem(
                id = "${testMedicationId}_2100",
                medicationName = testMedicationName,
                time = LocalTime.of(21, 0),
                isPast = LocalTime.now().isAfter(LocalTime.of(21, 0)),
                isTaken = false,
                underlyingReminderId = 3L
            )
        ).sortedBy { it.time }

        assertEquals(expectedItems, viewModel.todayScheduleItems.value)
    }

    // --- Tests for addPastMedicationTaken ---

    @Test
    fun `addPastMedicationTaken_validPastDateTimeToday_callsLoadTodaySchedule`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val today = LocalDate.now()
        val pastTime = LocalTime.now().minusHours(1)

        // No actual repository call is made in placeholder, so no coVerify for repo.
        // We check if loadTodaySchedule was called by observing its effect on todayScheduleItems.

        viewModel.addPastMedicationTaken(testMedicationId, "TestMed", today, pastTime)

        // Verify that loadTodaySchedule's placeholder logic was executed again
        val expectedItems = listOf(
            TodayScheduleItem("${testMedicationId}_0900", testMedicationName, LocalTime.of(9,0), LocalTime.now().isAfter(LocalTime.of(9,0)), false, 1L),
            TodayScheduleItem("${testMedicationId}_1500", testMedicationName, LocalTime.of(15,0), LocalTime.now().isAfter(LocalTime.of(15,0)), true, 2L),
            TodayScheduleItem("${testMedicationId}_2100", testMedicationName, LocalTime.of(21,0), LocalTime.now().isAfter(LocalTime.of(21,0)), false, 3L)
        ).sortedBy { it.time }
        assertEquals(expectedItems, viewModel.todayScheduleItems.value)
    }

    @Test
    fun `addPastMedicationTaken_futureDateTime_doesNotCallLoadTodaySchedule`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val today = LocalDate.now()
        val futureTime = LocalTime.now().plusHours(1) // Future time today
        val futureDate = LocalDate.now().plusDays(1) // Future date

        val initialSchedule = viewModel.todayScheduleItems.value // Capture initial state (empty)

        viewModel.addPastMedicationTaken(testMedicationId, "TestMed", today, futureTime)
        assertEquals(initialSchedule, viewModel.todayScheduleItems.value) // Should not change

        viewModel.addPastMedicationTaken(testMedicationId, "TestMed", futureDate, LocalTime.NOON)
        assertEquals(initialSchedule, viewModel.todayScheduleItems.value) // Should not change
        // Also, no repository calls should be made (coVerify would be used if repo calls were expected)
    }

    @Test
    fun `addPastMedicationTaken_validPastDateNotToday_doesNotRefreshTodaySchedule`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val pastDate = LocalDate.now().minusDays(1)
        val pastTime = LocalTime.NOON

        val initialSchedule = viewModel.todayScheduleItems.value // Capture initial state (empty)
        viewModel.addPastMedicationTaken(testMedicationId, "TestMed", pastDate, pastTime)

        // todayScheduleItems should remain unchanged (empty) because the date was not today
        assertEquals(initialSchedule, viewModel.todayScheduleItems.value)
        // Repository call for insert would be verified here in a non-placeholder scenario
    }


    // --- Tests for updateReminderStatus ---
    @Test
    fun `updateReminderStatus_itemExists_updatesItemLocally`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        // 1. Populate the schedule
        viewModel.loadTodaySchedule(testMedicationId)
        val originalItems = viewModel.todayScheduleItems.value
        assertTrue("Schedule should be populated", originalItems.isNotEmpty())

        val itemToUpdate = originalItems.first()
        val newTakenStatus = !itemToUpdate.isTaken

        // 2. Call updateReminderStatus
        viewModel.updateReminderStatus(itemToUpdate.id, newTakenStatus, testMedicationId)

        // 3. Assert local list is updated
        val updatedItems = viewModel.todayScheduleItems.value
        val updatedItem = updatedItems.find { it.id == itemToUpdate.id }

        assertFalse("Original list should not be the same instance", originalItems === updatedItems)
        assertTrue("Updated item should not be null", updatedItem != null)
        assertEquals(newTakenStatus, updatedItem!!.isTaken)

        // In a non-placeholder scenario, verify repository calls here
        // e.g., coVerify { mockReminderRepository.update(any()) }
    }

    @Test
    fun `updateReminderStatus_itemDoesNotExist_doesNotCrashAndListUnchanged`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        // 1. Populate the schedule (optional, could start empty)
        viewModel.loadTodaySchedule(testMedicationId)
        val initialItems = viewModel.todayScheduleItems.value

        // 2. Call updateReminderStatus with a non-existent ID
        viewModel.updateReminderStatus("nonExistentId", true, testMedicationId)

        // 3. Assert list is unchanged
        assertEquals(initialItems, viewModel.todayScheduleItems.value)
        // Also, no repository calls should be made (coVerify would be used if repo calls were expected)
    }
}
