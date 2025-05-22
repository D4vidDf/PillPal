package com.d4viddf.medicationreminder.viewmodel

import app.cash.turbine.test
import com.d4viddf.medicationreminder.data.*
import com.d4viddf.medicationreminder.logic.ReminderCalculator // Actual object, not mocked
import com.d4viddf.medicationreminder.rules.TestCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
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

    private lateinit var viewModel: MedicationViewModel

    private val dateStorableFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeStorableFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    private val storableDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME


    private val testMedId = 1

    @Before
    fun setUp() {
        viewModel = MedicationViewModel(medicationRepository, reminderRepository, scheduleRepository)
    }

    private fun createMedication(
        id: Int = testMedId,
        startDate: String, // Using String to match data class
        endDate: String? = null
    ): Medication {
        return Medication(
            id = id,
            name = "TestMed $id",
            dosage = "1 pill",
            startDate = startDate,
            endDate = endDate,
            notes = null
        )
    }

    private fun createSchedule(
        medId: Int = testMedId,
        scheduleType: ScheduleType,
        intervalHours: Int? = null,
        intervalMinutes: Int? = null,
        intervalStartTime: String? = null, // Using String to match data class
        specificTimes: String? = null // e.g., "08:00,12:00"
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
            specificTimes = LocalTime.of(9,0).format(timeStorableFormatter) // "09:00"
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
        val medication = createMedication(startDate = medicationStartDate.format(dateStorableFormatter))
        // CUSTOM_ALARMS schedule, times at 10:00, 14:00, 18:00
        val specificTimesStr = "${LocalTime.of(10,0).format(timeStorableFormatter)},${LocalTime.of(14,0).format(timeStorableFormatter)},${LocalTime.of(18,0).format(timeStorableFormatter)}"
        val schedule = createSchedule(
            scheduleType = ScheduleType.CUSTOM_ALARMS,
            specificTimes = specificTimesStr
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
}
