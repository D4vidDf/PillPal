package com.d4viddf.medicationreminder.viewmodel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationRepository
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.MedicationScheduleRepository
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.data.TodayScheduleItem
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    private lateinit var mockMedicationRepository: MedicationRepository

    @MockK
    private lateinit var mockScheduleRepository: MedicationScheduleRepository

    @MockK
    private lateinit var mockReminderRepository: MedicationReminderRepository

    @MockK
    private lateinit var mockContext: Context // Required by ViewModel constructor

    private lateinit var viewModel: MedicationReminderViewModel

    private val testMedicationId = 1
    private val testMedication = Medication(id = testMedicationId, name = "TestMed", typeId = 1, color = "Blue", dosage = "1 pill", packageSize = 30, remainingDoses = 30, startDate = LocalDate.now().minusDays(5).format(ReminderCalculator.dateStorableFormatter))


    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true) // relaxUnitFun for coEvery { ... } just runs
        val mockWorkManager = mockk<androidx.work.WorkManager>(relaxed = true)
        coEvery { mockContext.applicationContext } returns mockContext
        coEvery { mockContext.getSystemService(Context.WORK_SERVICE) } returns mockWorkManager
        coEvery { mockContext.packageName } returns "com.d4viddf.medicationreminder.test"

        viewModel = MedicationReminderViewModel(
            mockMedicationRepository,
            mockScheduleRepository,
            mockReminderRepository,
            mockContext
        )
    }

    @After
    fun tearDown() {
        // Clean up, if necessary
    }

    // --- Tests for loadTodaySchedule ---

    @Test
    fun `loadTodaySchedule_noMedicationFound_emitsEmptyList`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        coEvery { mockMedicationRepository.getMedicationById(testMedicationId) } returns null

        viewModel.loadTodaySchedule(testMedicationId)

        assertTrue(viewModel.todayScheduleItems.value.isEmpty())
    }

    @Test
    fun `loadTodaySchedule_noSchedulesForMedication_emitsEmptyList`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        coEvery { mockMedicationRepository.getMedicationById(testMedicationId) } returns testMedication
        coEvery { mockScheduleRepository.getSchedulesForMedication(testMedicationId) } returns flowOf(emptyList())

        viewModel.loadTodaySchedule(testMedicationId)

        assertTrue(viewModel.todayScheduleItems.value.isEmpty())
    }

    @Test
    fun `loadTodaySchedule_schedulesExist_noExistingReminders_emitsCorrectItems`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val today = LocalDate.now()
        val time0900 = LocalTime.of(9, 0)
        val schedule1 = MedicationSchedule(id = 1, medicationId = testMedicationId, scheduleType = ScheduleType.DAILY, daysOfWeek = "1,2,3,4,5,6,7", specificTimes = time0900.format(ReminderCalculator.timeStorableFormatter), intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null)
        val mockMedicationWithRecentStartDate = testMedication.copy(startDate = today.minusDays(1).format(ReminderCalculator.dateStorableFormatter))


        coEvery { mockMedicationRepository.getMedicationById(testMedicationId) } returns mockMedicationWithRecentStartDate
        coEvery { mockScheduleRepository.getSchedulesForMedication(testMedicationId) } returns flowOf(listOf(schedule1))
        coEvery { mockReminderRepository.getRemindersForMedication(testMedicationId) } returns flowOf(emptyList())

        viewModel.loadTodaySchedule(testMedicationId)

        val items = viewModel.todayScheduleItems.value
        assertEquals(1, items.size)
        assertEquals(testMedication.name, items[0].medicationName)
        assertEquals(time0900, items[0].time)
        assertEquals(false, items[0].isTaken)
        assertEquals(0L, items[0].underlyingReminderId)
        assertEquals(schedule1.id, items[0].medicationScheduleId)
    }

    @Test
    fun `loadTodaySchedule_schedulesExist_oneReminderTaken_emitsCorrectItems`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val today = LocalDate.now()
        val time1000 = LocalTime.of(10, 0)
        val schedule2 = MedicationSchedule(id = 2, medicationId = testMedicationId, scheduleType = ScheduleType.DAILY, daysOfWeek = today.dayOfWeek.value.toString(), specificTimes = time1000.format(ReminderCalculator.timeStorableFormatter), intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null)
        val reminderTimeStr = LocalDateTime.of(today, time1000).format(ReminderCalculator.storableDateTimeFormatter)
        val mockTakenReminder = MedicationReminder(id = 100, medicationId = testMedicationId, medicationScheduleId = schedule2.id, reminderTime = reminderTimeStr, isTaken = true, takenAt = reminderTimeStr)
        val mockMedicationWithOldStartDate = testMedication.copy(startDate = today.minusDays(10).format(ReminderCalculator.dateStorableFormatter))


        coEvery { mockMedicationRepository.getMedicationById(testMedicationId) } returns mockMedicationWithOldStartDate
        coEvery { mockScheduleRepository.getSchedulesForMedication(testMedicationId) } returns flowOf(listOf(schedule2))
        coEvery { mockReminderRepository.getRemindersForMedication(testMedicationId) } returns flowOf(listOf(mockTakenReminder))

        viewModel.loadTodaySchedule(testMedicationId)

        val items = viewModel.todayScheduleItems.value
        assertEquals(1, items.size)
        val item = items.find { it.time == time1000 }
        assertNotNull(item)
        assertEquals(true, item!!.isTaken)
        assertEquals(mockTakenReminder.id.toLong(), item.underlyingReminderId)
        assertEquals(schedule2.id, item.medicationScheduleId)
    }


    // --- Tests for addPastMedicationTaken ---
    @Test
    fun `addPastMedicationTaken_validPastDose_noMatchingSchedule_insertsReminderWithScheduleId0`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val pastDate = LocalDate.now().minusDays(1)
        val pastTime = LocalTime.of(14, 0)
        // Ensure ReminderCalculator returns empty for this specific time, or for any schedule
        val scheduleNotMatching = MedicationSchedule(id = 3, medicationId = testMedicationId, scheduleType = ScheduleType.DAILY, daysOfWeek = "1", specificTimes = "10:00", intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null)


        coEvery { mockMedicationRepository.getMedicationById(testMedicationId) } returns testMedication
        coEvery { mockScheduleRepository.getSchedulesForMedication(testMedicationId) } returns flowOf(listOf(scheduleNotMatching))
        coEvery { mockReminderRepository.insertReminder(any()) } returns 1L // return dummy ID

        viewModel.addPastMedicationTaken(testMedicationId, testMedication.name, pastDate, pastTime)

        coVerify { mockReminderRepository.insertReminder(match {
            it.medicationId == testMedicationId &&
            it.medicationScheduleId == 0 && // No matching schedule
            it.isTaken &&
            it.reminderTime == LocalDateTime.of(pastDate, pastTime).format(ReminderCalculator.storableDateTimeFormatter)
        }) }
    }

    @Test
    fun `addPastMedicationTaken_validPastDose_matchingSchedule_insertsReminderWithScheduleId`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val pastDate = LocalDate.now().minusDays(1) // A specific past date
        val pastTime = LocalTime.of(10,0) // Time that will match scheduleForPastDate
        val scheduleForPastDate = MedicationSchedule(
            id = 5, medicationId = testMedicationId, scheduleType = ScheduleType.DAILY,
            daysOfWeek = pastDate.dayOfWeek.value.toString(), // Match the day of week for pastDate
            specificTimes = pastTime.format(ReminderCalculator.timeStorableFormatter), // Match the pastTime
            intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null
        )
        val medicationForPastDate = testMedication.copy(startDate = pastDate.minusDays(2).format(ReminderCalculator.dateStorableFormatter))


        coEvery { mockMedicationRepository.getMedicationById(testMedicationId) } returns medicationForPastDate
        coEvery { mockScheduleRepository.getSchedulesForMedication(testMedicationId) } returns flowOf(listOf(scheduleForPastDate))
        coEvery { mockReminderRepository.insertReminder(any()) } returns 1L

        viewModel.addPastMedicationTaken(testMedicationId, medicationForPastDate.name, pastDate, pastTime)

        coVerify { mockReminderRepository.insertReminder(match {
            it.medicationId == testMedicationId &&
            it.medicationScheduleId == scheduleForPastDate.id && // Matching schedule
            it.isTaken
        }) }
    }


    @Test
    fun `addPastMedicationTaken_futureDateTime_doesNotCallRepository`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val futureDate = LocalDate.now().plusDays(1)
        val futureTime = LocalTime.NOON

        viewModel.addPastMedicationTaken(testMedicationId, testMedication.name, futureDate, futureTime)

        coVerify(exactly = 0) { mockReminderRepository.insertReminder(any()) }
    }

    @Test
    fun `addPastMedicationTaken_validPastDateToday_refreshesTodaySchedule`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val today = LocalDate.now()
        val pastTime = LocalTime.now().minusHours(1)
        val medicationForCalc = testMedication.copy(startDate = today.minusDays(1).format(ReminderCalculator.dateStorableFormatter))

        coEvery { mockMedicationRepository.getMedicationById(testMedicationId) } returns medicationForCalc
        coEvery { mockScheduleRepository.getSchedulesForMedication(testMedicationId) } returns flowOf(emptyList()) // for addPast...
        coEvery { mockReminderRepository.insertReminder(any()) } returns 1L
        coEvery { mockReminderRepository.getRemindersForMedication(testMedicationId) } returns flowOf(emptyList()) // for loadToday...

        viewModel.addPastMedicationTaken(testMedicationId, testMedication.name, today, pastTime)

        // Verify insert was called
        coVerify { mockReminderRepository.insertReminder(any()) }
        // Verify that loadTodaySchedule's dependencies were called again
        coVerify(atLeast = 2) { mockMedicationRepository.getMedicationById(testMedicationId) } // Once for add, once for load
        coVerify(atLeast = 2) { mockScheduleRepository.getSchedulesForMedication(testMedicationId) } // Once for add, once for load
    }


    // --- Tests for updateReminderStatus ---
    private suspend fun setupLoadTodayScheduleWithOneItem(taken: Boolean, scheduleId: Int = 1, time: LocalTime = LocalTime.of(9,0)): TodayScheduleItem {
        val today = LocalDate.now()
        val itemTime = time
        val testSchedule = MedicationSchedule(id = scheduleId, medicationId = testMedicationId, scheduleType = ScheduleType.DAILY, daysOfWeek = today.dayOfWeek.value.toString(), specificTimes = itemTime.format(ReminderCalculator.timeStorableFormatter), intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null)
        val reminderTimeStr = LocalDateTime.of(today, itemTime).format(ReminderCalculator.storableDateTimeFormatter)
        val existingReminder = if (taken) MedicationReminder(id = 101, medicationId = testMedicationId, medicationScheduleId = scheduleId, reminderTime = reminderTimeStr, isTaken = true, takenAt = reminderTimeStr) else null

        coEvery { mockMedicationRepository.getMedicationById(testMedicationId) } returns testMedication
        coEvery { mockScheduleRepository.getSchedulesForMedication(testMedicationId) } returns flowOf(listOf(testSchedule))
        coEvery { mockReminderRepository.getRemindersForMedication(testMedicationId) } returns flowOf(if(existingReminder != null) listOf(existingReminder) else emptyList())

        viewModel.loadTodaySchedule(testMedicationId)
        return viewModel.todayScheduleItems.value.first { it.time == itemTime }
    }

    @Test
    fun `updateReminderStatus_markAsTaken_itemNotYetInDB_insertsNewReminder`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val testItem = setupLoadTodayScheduleWithOneItem(taken = false, scheduleId = 20) // This item will have underlyingReminderId = 0L

        // Ensure no existing reminder for this specific time when updateReminderStatus calls getRemindersForMedication
        coEvery { mockReminderRepository.getRemindersForMedication(testMedicationId) } returns flowOf(emptyList()) // Simulate no reminder existing at the point of update check
        coEvery { mockReminderRepository.insertReminder(any()) } returns 1L // Mock insert

        viewModel.updateReminderStatus(testItem.id, true, testMedicationId)

        coVerify { mockReminderRepository.insertReminder(match {
            it.medicationId == testMedicationId &&
            it.medicationScheduleId == testItem.medicationScheduleId && // Uses ID from TodayScheduleItem
            it.isTaken &&
            it.reminderTime == LocalDateTime.of(LocalDate.now(), testItem.time).format(ReminderCalculator.storableDateTimeFormatter)
        }) }
        coVerify(atLeast = 2) { mockMedicationRepository.getMedicationById(testMedicationId) } // For initial load + reload
    }

    @Test
    fun `updateReminderStatus_markAsTaken_itemExistsNotTaken_updatesReminder`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val itemTime = LocalTime.of(11,0)
        val today = LocalDate.now()
        val scheduleId = 21
        val reminderId = 102
        val reminderTimeStr = LocalDateTime.of(today, itemTime).format(ReminderCalculator.storableDateTimeFormatter)
        val dbReminder = MedicationReminder(id = reminderId, medicationId = testMedicationId, medicationScheduleId = scheduleId, reminderTime = reminderTimeStr, isTaken = false, takenAt = null)

        // Setup for loadTodaySchedule
        val testSchedule = MedicationSchedule(id = scheduleId, medicationId = testMedicationId, scheduleType = ScheduleType.DAILY, daysOfWeek = today.dayOfWeek.value.toString(), specificTimes = itemTime.format(ReminderCalculator.timeStorableFormatter), intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null)
        coEvery { mockMedicationRepository.getMedicationById(testMedicationId) } returns testMedication
        coEvery { mockScheduleRepository.getSchedulesForMedication(testMedicationId) } returns flowOf(listOf(testSchedule))
        coEvery { mockReminderRepository.getRemindersForMedication(testMedicationId) } returns flowOf(listOf(dbReminder)) // This is for loadTodaySchedule AND the find in updateReminderStatus

        viewModel.loadTodaySchedule(testMedicationId)
        val testItem = viewModel.todayScheduleItems.value.first { it.underlyingReminderId == reminderId.toLong() }

        coEvery { mockReminderRepository.updateReminder(any()) } just runs

        viewModel.updateReminderStatus(testItem.id, true, testMedicationId)

        coVerify { mockReminderRepository.updateReminder(match {
            it.id == reminderId && it.isTaken && it.takenAt != null
        }) }
    }

    @Test
    fun `updateReminderStatus_markAsNotTaken_itemExistsTaken_updatesReminder`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        val itemTime = LocalTime.of(12,0)
        val today = LocalDate.now()
        val scheduleId = 22
        val reminderId = 103
        val reminderTimeStr = LocalDateTime.of(today, itemTime).format(ReminderCalculator.storableDateTimeFormatter)
        val dbReminder = MedicationReminder(id = reminderId, medicationId = testMedicationId, medicationScheduleId = scheduleId, reminderTime = reminderTimeStr, isTaken = true, takenAt = reminderTimeStr)

        val testSchedule = MedicationSchedule(id = scheduleId, medicationId = testMedicationId, scheduleType = ScheduleType.DAILY, daysOfWeek = today.dayOfWeek.value.toString(), specificTimes = itemTime.format(ReminderCalculator.timeStorableFormatter), intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null)
        coEvery { mockMedicationRepository.getMedicationById(testMedicationId) } returns testMedication
        coEvery { mockScheduleRepository.getSchedulesForMedication(testMedicationId) } returns flowOf(listOf(testSchedule))
        coEvery { mockReminderRepository.getRemindersForMedication(testMedicationId) } returns flowOf(listOf(dbReminder))

        viewModel.loadTodaySchedule(testMedicationId)
        val testItem = viewModel.todayScheduleItems.value.first { it.underlyingReminderId == reminderId.toLong() }

        coEvery { mockReminderRepository.updateReminder(any()) } just runs

        viewModel.updateReminderStatus(testItem.id, false, testMedicationId)

        coVerify { mockReminderRepository.updateReminder(match {
            it.id == reminderId && !it.isTaken && it.takenAt == null
        }) }
    }


    @Test
    fun `updateReminderStatus_itemIdNotFound_doesNotCrashOrCallRepo`() = mainCoroutineRule.testDispatcher.runBlockingTest {
        setupLoadTodayScheduleWithOneItem(false) // Populate with some data

        viewModel.updateReminderStatus("nonExistentId", true, testMedicationId)

        coVerify(exactly = 0) { mockReminderRepository.insertReminder(any()) }
        coVerify(exactly = 0) { mockReminderRepository.updateReminder(any()) }
    }
}
