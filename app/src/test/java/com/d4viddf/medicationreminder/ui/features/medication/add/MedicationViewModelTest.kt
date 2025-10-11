package com.d4viddf.medicationreminder.ui.features.medication.add

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.data.model.MedicationReminder
import com.d4viddf.medicationreminder.data.model.MedicationSchedule
import com.d4viddf.medicationreminder.data.model.ScheduleType
import com.d4viddf.medicationreminder.data.repository.MedicationDosageRepository
import com.d4viddf.medicationreminder.data.repository.MedicationReminderRepository
import com.d4viddf.medicationreminder.data.repository.MedicationRepository
import com.d4viddf.medicationreminder.data.repository.MedicationScheduleRepository
import com.d4viddf.medicationreminder.domain.usecase.ReminderCalculator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import java.time.LocalTime

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MedicationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: MedicationViewModel
    private val medicationRepository: MedicationRepository = mockk()
    private val reminderRepository: MedicationReminderRepository = mockk()
    private val scheduleRepository: MedicationScheduleRepository = mockk()
    private val dosageRepository: MedicationDosageRepository = mockk()
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Mock the call made in the ViewModel's init block
        every { medicationRepository.getAllMedications() } returns flowOf(emptyList())
        viewModel = MedicationViewModel(
            medicationRepository,
            reminderRepository,
            scheduleRepository,
            dosageRepository,
            context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `calculate daily progress correctly`() = runBlocking {
        val medication = Medication(id = 1, name = "Aspirin", startDate = "01/01/2024", endDate = "31/12/2024", typeId = 1, color = "", packageSize = 1, remainingDoses = 1, reminderTime = null)
        val schedule = MedicationSchedule(medicationId = 1, scheduleType = ScheduleType.DAILY, startDate = "01/01/2024", specificTimes = listOf(LocalTime.of(8, 0)), daysOfWeek = emptyList(), intervalHours = null, intervalMinutes = null, intervalStartTime = null, intervalEndTime = null)
        val reminders = listOf(
            MedicationReminder(medicationId = 1, reminderTime = LocalDateTime.now().withHour(8).withMinute(0).format(ReminderCalculator.storableDateTimeFormatter), isTaken = true, medicationScheduleId = 1, medicationDosageId = 1, takenAt = null, notificationId = 1)
        )

        coEvery { medicationRepository.getMedicationById(1) } returns medication
        coEvery { scheduleRepository.getSchedulesForMedication(1) } returns flowOf(listOf(schedule))
        coEvery { reminderRepository.getRemindersForMedication(1) } returns flowOf(reminders)


        viewModel.observeMedicationAndRemindersForDailyProgress(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val progressDetails = viewModel.medicationProgressDetails.value
        assertEquals(1, progressDetails?.taken)
        assertEquals(0, progressDetails?.remaining)
        assertEquals(0, progressDetails?.totalFromPackage)
        assertEquals(0f, progressDetails?.progressFraction)
        assertEquals("1 / 0", progressDetails?.displayText)
    }
}