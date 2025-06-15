package com.d4viddf.medicationreminder.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationReminder
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.notifications.NotificationScheduler
import com.d4viddf.medicationreminder.repository.MedicationRepository
import com.d4viddf.medicationreminder.repository.MedicationScheduleRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.junit.Assert.*
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString

@RunWith(MockitoJUnitRunner::class)
class ReminderSchedulingWorkerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockWorkerParams: WorkerParameters

    @Mock
    private lateinit var mockMedicationRepository: MedicationRepository

    @Mock
    private lateinit var mockMedicationScheduleRepository: MedicationScheduleRepository

    @Mock
    private lateinit var mockMedicationReminderRepository: MedicationReminderRepository

    @Mock
    private lateinit var mockNotificationScheduler: NotificationScheduler

    // ReminderCalculator is an object, so it's harder to mock directly without PowerMock or similar.
    // For this test, we'll assume ReminderCalculator.generateRemindersForPeriod works as tested in its own unit tests.
    // Or, we can wrap its call if we need to control its output directly, but the prompt implies using it.

    @Captor
    private lateinit var localDateCaptor: ArgumentCaptor<LocalDate>

    @Captor
    private lateinit var localDateTimeCaptor: ArgumentCaptor<LocalDateTime>

    @Captor
    private lateinit var medicationReminderCaptor: ArgumentCaptor<MedicationReminder>


    private lateinit var worker: ReminderSchedulingWorker

    private val storableDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateStorableFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    @Before
    fun setUp() {
        // Initialize the worker with mocked dependencies
        worker = ReminderSchedulingWorker(
            mockContext,
            mockWorkerParams,
            mockMedicationRepository,
            mockMedicationScheduleRepository,
            mockMedicationReminderRepository,
            mockNotificationScheduler
        )
    }

    private fun createTestMedication(id: Int, startDate: String?, endDate: String?): Medication {
        return Medication(
            id = id, name = "TestMed $id", typeId = 1, color = "Blue",
            startDate = startDate, endDate = endDate, registrationDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }

    private fun createTestSchedule(medId: Int, type: ScheduleType, intervalHours: Int? = null): MedicationSchedule {
        return MedicationSchedule(
            id = medId, medicationId = medId, scheduleType = type,
            intervalHours = intervalHours, intervalMinutes = 0,
            intervalStartTime = if (type == ScheduleType.INTERVAL && intervalHours != null) null else "08:00", // Type B if intervalStartTime is null
            specificTimes = if (type == ScheduleType.CUSTOM_ALARMS) "08:00,14:00,20:00" else null
        )
    }

    private fun createTestReminder(id: Int, medId: Int, time: LocalDateTime, isTaken: Boolean = false): MedicationReminder {
        return MedicationReminder(id, medId, medId, time.format(storableDateTimeFormatter), isTaken, if(isTaken) time.minusMinutes(5).format(storableDateTimeFormatter) else null, null)
    }


    @Test
    fun testScheduleNext_14DayWindowUsed_AlarmsOnlyForFuture() = runBlocking {
        val medicationId = 1
        val medication = createTestMedication(medicationId, LocalDate.now().minusDays(5).format(dateStorableFormatter), null)
        val schedule = createTestSchedule(medicationId, ScheduleType.CUSTOM_ALARMS) // e.g., 3 times a day

        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(medication)
        `when`(mockMedicationScheduleRepository.getSchedulesForMedication(medicationId)).thenReturn(flowOf(listOf(schedule)))
        `when`(mockMedicationReminderRepository.getMostRecentTakenReminder(medicationId)).thenReturn(null) // No taken reminders
        `when`(mockMedicationReminderRepository.getRemindersForMedicationInWindow(eq(medicationId), anyString(), anyString()))
            .thenReturn(emptyList()) // No existing reminders in DB for simplicity initially
        `when`(mockMedicationReminderRepository.getFutureUntakenRemindersForMedication(eq(medicationId), anyString()))
             .thenReturn(flowOf(emptyList())) // No stale reminders to clean initially
        `when`(mockMedicationReminderRepository.insertReminder(any(MedicationReminder::class.java)))
            .thenAnswer { invocation ->
                val reminder = invocation.getArgument<MedicationReminder>(0)
                (reminder.hashCode() % 10000).toLong() // Simulate DB generated ID
            }


        // --- This is where direct mocking of ReminderCalculator would be ideal.
        // For now, we'll let it run but check its inputs and then control what gets scheduled.
        // To truly test this, we'd need ReminderCalculator.generateRemindersForPeriod to be an interface or use PowerMockito.
        // Instead, we will check the periodEndDate passed to ReminderCalculator via a spy or by inference from other calls.
        // The prompt allows using ReminderCalculator as is, so we'll verify its usage.

        // Let's define what ReminderCalculator *should* produce for this test's purpose.
        // This map simulates the output of ReminderCalculator.generateRemindersForPeriod
        val now = LocalDateTime.now()
        val idealRemindersMap = mutableMapOf<LocalDate, List<LocalTime>>()
        // Past reminder (should be saved to DB if valid by isValidForScheduling, but not alarmed)
        idealRemindersMap[now.toLocalDate().minusDays(1)] = listOf(now.toLocalTime().minusHours(2).withMinute(0).withSecond(0))
        // Today - one past, one future (past saved, future saved & alarmed)
        idealRemindersMap[now.toLocalDate()] = listOf(
            now.toLocalTime().minusHours(1).withMinute(0).withSecond(0), // Past today
            now.toLocalTime().plusHours(1).withMinute(0).withSecond(0)   // Future today
        )
        // Future reminders (saved & alarmed)
        for (i in 1..14) {
            idealRemindersMap[now.toLocalDate().plusDays(i.toLong())] = listOf(LocalTime.of(8,0), LocalTime.of(14,0), LocalTime.of(20,0))
        }

        // We cannot directly mock ReminderCalculator.generateRemindersForPeriod as it's an object.
        // So we can't directly return idealRemindersMap.
        // The test will call the real ReminderCalculator. We must ensure its setup leads to the desired scenario.
        // For this specific test, the schedule is CUSTOM_ALARMS (08:00, 14:00, 20:00).
        // The worker will call ReminderCalculator.generateRemindersForPeriod.

        val worker = ReminderSchedulingWorker(mockContext, mockWorkerParams, mockMedicationRepository, mockMedicationScheduleRepository, mockMedicationReminderRepository, mockNotificationScheduler)
        // Simulate input data for a specific medication
        `when`(mockWorkerParams.inputData).thenReturn(androidx.work.Data.Builder().putInt(ReminderSchedulingWorker.KEY_MEDICATION_ID, medicationId).build())


        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        // 1. Verify ReminderCalculator.generateRemindersForPeriod was effectively used for a ~14 day window
        // We check this by verifying the dates for which medicationReminderRepository.insertReminder was called.
        // This is an indirect way since we don't mock ReminderCalculator itself.
        // The real generateRemindersForPeriod will be called. We look at what it tries to insert.

        // We need to capture arguments to medicationReminderRepository.insertReminder
        verify(mockMedicationReminderRepository, atLeastOnce()).insertReminder(medicationReminderCaptor.capture())

        val insertedReminderTimes = medicationReminderCaptor.allValues.map { LocalDateTime.parse(it.reminderTime, storableDateTimeFormatter) }

        val maxReminderDate = insertedReminderTimes.maxByOrNull { it }?.toLocalDate()
        assertNotNull("Should have inserted some reminders", maxReminderDate)

        // effectiveSearchStartDateTime is 'now' or 'medicationStartDate'
        // For a running medication, it's effectively now.toLocalDate().atStartOfDay() or now
        val expectedEndDateApprox = LocalDate.now().plusDays(13) // Worker uses plusDays(14) but then adjusts if med ends. 13 to 14 days out.
        assertTrue("Reminders should be generated for up to approx 14 days. Max date: $maxReminderDate, Expected near: $expectedEndDateApprox",
            maxReminderDate!! >= expectedEndDateApprox && maxReminderDate <= expectedEndDateApprox.plusDays(1) // Allow some leeway
        )


        // 2. Verify notificationScheduler.scheduleNotification is called ONLY for future times
        val scheduleNotificationCaptorTime = ArgumentCaptor.forClass(Long::class.java)
        verify(mockNotificationScheduler, atLeastOnce()).scheduleNotification(
            any(Context::class.java),
            any(MedicationReminder::class.java),
            anyString(),
            anyString(),
            any(Boolean::class.java),
            anyLongOrNull(), // nextDoseTimeForHelperMillis
            scheduleNotificationCaptorTime.capture()
        )

        val currentTimeMillis = System.currentTimeMillis()
        scheduleNotificationCaptorTime.allValues.forEach { scheduledTimeMillis ->
            assertTrue("Notification scheduled for time $scheduledTimeMillis should be in the future (current: $currentTimeMillis). Diff: ${scheduledTimeMillis - currentTimeMillis}",
                scheduledTimeMillis > currentTimeMillis - 1000 // Allow 1s slack for test execution time
            )
        }

        // Check that reminders that were in the past (but within 12 hours for today) were inserted but not scheduled for alarm
        // Example: CUSTOM_ALARM at 08:00, 14:00, 20:00. If current time is 15:00.
        // 14:00 reminder should be inserted (isValidForScheduling is true).
        // But 14:00 reminder should NOT have scheduleNotification called.
        // 20:00 reminder should be inserted AND have scheduleNotification called.

        val eightAmToday = LocalDateTime.of(LocalDate.now(), LocalTime.of(8,0))
        val twoPmToday = LocalDateTime.of(LocalDate.now(), LocalTime.of(14,0))
        val eightPmToday = LocalDateTime.of(LocalDate.now(), LocalTime.of(20,0))

        val wasEightAmInserted = insertedReminderTimes.any { it.isEqual(eightAmToday) }
        val wasTwoPmInserted = insertedReminderTimes.any { it.isEqual(twoPmToday) }
        val wasEightPmInserted = insertedReminderTimes.any { it.isEqual(eightPmToday) }

        assertTrue(wasEightAmInserted || wasTwoPmInserted || wasEightPmInserted) // At least one of our test times should be there

        val scheduledAlarmsEpochMillis = scheduleNotificationCaptorTime.allValues

        if (eightAmToday.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() < currentTimeMillis) {
            assertFalse("Alarm for 8 AM today should not be scheduled if it's in the past",
                scheduledAlarmsEpochMillis.contains(eightAmToday.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
        }
         if (twoPmToday.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() < currentTimeMillis) {
            assertFalse("Alarm for 2 PM today should not be scheduled if it's in the past",
                scheduledAlarmsEpochMillis.contains(twoPmToday.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
        }
        if (eightPmToday.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() > currentTimeMillis) {
             assertTrue("Alarm for 8 PM today should be scheduled if it's in the future",
                scheduledAlarmsEpochMillis.contains(eightPmToday.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
        }
    }

    fun <T> anyLongOrNull(): T = ArgumentMatchers.anyLong()


    @Test
    fun testScheduleNext_StaleRemindersCleanedBasedOn14DayIdealSet() = runBlocking {
        val medicationId = 2
        val medication = createTestMedication(medicationId, LocalDate.now().minusDays(5).format(dateStorableFormatter), null)
        val schedule = createTestSchedule(medicationId, ScheduleType.DAILY) // Once a day at 08:00

        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(medication)
        `when`(mockMedicationScheduleRepository.getSchedulesForMedication(medicationId)).thenReturn(flowOf(listOf(schedule)))
        `when`(mockMedicationReminderRepository.getMostRecentTakenReminder(medicationId)).thenReturn(null)
        `when`(mockMedicationReminderRepository.insertReminder(any(MedicationReminder::class.java))).thenReturn(1L) // Dummy ID

        // ReminderCalculator will generate daily reminders at 08:00 for the next 14 days.
        // These are the "ideal" reminders.

        val now = LocalDateTime.now()
        val staleReminderTime = now.plusDays(3).withHour(10).withMinute(0) // This time is not 08:00, so it's stale
        val staleReminder = createTestReminder(101, medicationId, staleReminderTime)

        val futureIdealReminderTime = now.plusDays(2).withHour(8).withMinute(0) // This one is ideal
        val futureIdealReminderInDb = createTestReminder(102, medicationId, futureIdealReminderTime)


        `when`(mockMedicationReminderRepository.getFutureUntakenRemindersForMedication(eq(medicationId), anyString()))
            .thenReturn(flowOf(listOf(staleReminder, futureIdealReminderInDb))) // One stale, one ideal

        val worker = ReminderSchedulingWorker(mockContext, mockWorkerParams, mockMedicationRepository, mockMedicationScheduleRepository, mockMedicationReminderRepository, mockNotificationScheduler)
        `when`(mockWorkerParams.inputData).thenReturn(androidx.work.Data.Builder().putInt(ReminderSchedulingWorker.KEY_MEDICATION_ID, medicationId).build())

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        // Verify that the stale reminder was deleted and its notification cancelled
        verify(mockMedicationReminderRepository, times(1)).deleteReminderById(staleReminder.id)
        verify(mockNotificationScheduler, times(1)).cancelAllAlarmsForReminder(mockContext, staleReminder.id)

        // Verify that the ideal future reminder (that was already in DB) was NOT deleted/cancelled
        verify(mockMedicationReminderRepository, never()).deleteReminderById(futureIdealReminderInDb.id)
        verify(mockNotificationScheduler, never()).cancelAllAlarmsForReminder(mockContext, futureIdealReminderInDb.id)

        // Verify new ideal reminders are still scheduled
        verify(mockNotificationScheduler, atLeastOnce()).scheduleNotification(any(), any(), anyString(), anyString(), anyBoolean(), anyLongOrNull(), anyLong())
    }
}
