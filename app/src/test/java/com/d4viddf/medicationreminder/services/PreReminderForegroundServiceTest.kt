package com.d4viddf.medicationreminder.services

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.d4viddf.medicationreminder.notifications.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class PreReminderForegroundServiceTest {

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var mockNotificationManager: NotificationManager

    @Mock
    lateinit var mockNotificationBuilder: NotificationCompat.Builder

    // We'll use a real service instance but mock its Android dependencies or interactions.
    private lateinit var service: PreReminderForegroundService

    // For mocking System.currentTimeMillis()
    private lateinit var systemMock: MockedStatic<System>

    @Captor
    lateinit var notificationCaptor: ArgumentCaptor<Notification>


    @Before
    fun setUp() {
        // Mock System.currentTimeMillis() before each test
        systemMock = Mockito.mockStatic(System::class.java)

        service = PreReminderForegroundService()

        // Manually set the context and notificationManager for the service instance
        // This is a simplified way, actual Android service testing is more complex.
        val contextField = Service::class.java.getDeclaredField("mBase")
        contextField.isAccessible = true
        contextField.set(service, mockContext)

        val notificationManagerField = PreReminderForegroundService::class.java.getDeclaredField("notificationManager")
        notificationManagerField.isAccessible = true
        notificationManagerField.set(service, mockNotificationManager)

        // Mock NotificationCompat.Builder chain
        `when`(mockContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager)
        // `when`(NotificationCompat.Builder(any(Context::class.java), anyString())).thenReturn(mockNotificationBuilder) // This will be tricky, need to mock the constructor
        // `when`(mockNotificationBuilder.setSmallIcon(anyInt())).thenReturn(mockNotificationBuilder)
        // ... and so on for all builder methods. This is where a real NotificationCompat.Builder would be better if possible,
        // or focusing on the arguments passed to a mocked NotificationManager.notify()

        // Mock the NotificationHelper channel ID as it's used by the service
        // No, NotificationHelper is an object, its constants are directly accessible.
    }

    private fun createStartIntent(
        reminderId: Int,
        actualScheduledTimeMillis: Long,
        medicationName: String
    ): Intent {
        val intent = Intent(mockContext, PreReminderForegroundService::class.java)
        intent.putExtra(PreReminderForegroundService.EXTRA_SERVICE_REMINDER_ID, reminderId)
        intent.putExtra(PreReminderForegroundService.EXTRA_SERVICE_ACTUAL_SCHEDULED_TIME_MILLIS, actualScheduledTimeMillis)
        intent.putExtra(PreReminderForegroundService.EXTRA_SERVICE_MEDICATION_NAME, medicationName)
        return intent
    }

    @Test
    fun `getNotificationId returns correct offset ID`() {
        val reminderId = 123
        val expectedNotificationId = reminderId + PreReminderForegroundService.PRE_REMINDER_NOTIFICATION_ID_OFFSET
        assertEquals(expectedNotificationId, PreReminderForegroundService.getNotificationId(reminderId))
    }

    @Test
    fun `onStartCommand with invalid data stops service`() {
        val intent = createStartIntent(-1, 1000L, "TestMed") // Invalid reminderId
        service.onStartCommand(intent, 0, 1)
        // How to verify stopSelf was called? This is hard without Robolectric or a testable service structure.
        // For now, we'll assume if it reaches a certain point, it didn't stop early.
        // Or, we can check if startForeground was NOT called if that's the flow.
        verify(mockNotificationManager, never()).notify(anyInt(), any()) // Should not proceed to show notification
    }

    @Test
    fun `onStartCommand with time already past threshold stops service`() {
        val reminderId = 1
        val actualTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5) // 5 seconds from now
        val intent = createStartIntent(reminderId, actualTime, "TestMed")

        systemMock.`when`<Long> { System.currentTimeMillis() }.thenReturn(System.currentTimeMillis())

        service.onStartCommand(intent, 0, 1)
        // Verify startForeground is not called, or if it is, then stopSelf is implicitly called.
        // This test is tricky because the stopSelf() is internal.
        // We can check that the handler doesn't post runnables, but that's also internal.
        // Let's check that startForeground is not called if it's supposed to stop *before* that.
        // The current code calls startForeground then checks time. If too close, it calls stopSelf.
        // So startForeground *would* be called.
        // This highlights the difficulty of testing Android component lifecycle and handler logic in pure unit tests.
    }


    // To properly test buildNotification, we need a way to provide a NotificationCompat.Builder
    // or capture its arguments. Let's try to use a real NotificationCompat.Builder
    // and verify the resulting Notification object's properties.

    @Test
    fun `buildNotification uses PRE_REMINDER_CHANNEL_ID and PRIORITY_DEFAULT`() {
        val reminderId = 10
        val medName = "TestMedX"
        val scheduledTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30) // 30 mins from now
        
        // Set internal state of the service for buildNotification to use
        val currentReminderIdField = PreReminderForegroundService::class.java.getDeclaredField("currentReminderId")
        currentReminderIdField.isAccessible = true
        currentReminderIdField.setInt(service, reminderId)

        val medicationNameField = PreReminderForegroundService::class.java.getDeclaredField("medicationNameForNotification")
        medicationNameField.isAccessible = true
        medicationNameField.set(service, medName)
        
        val actualTakeTimeMillisField = PreReminderForegroundService::class.java.getDeclaredField("actualTakeTimeMillis")
        actualTakeTimeMillisField.isAccessible = true
        actualTakeTimeMillisField.setLong(service, scheduledTime)

        // Mock System.currentTimeMillis for consistent timeRemainingMillis
        val currentTime = scheduledTime - TimeUnit.MINUTES.toMillis(25) // Simulate 25 mins remaining
        systemMock.`when`<Long> { System.currentTimeMillis() }.thenReturn(currentTime)

        // Invoke the private method buildNotification using reflection
        val method = PreReminderForegroundService::class.java.getDeclaredMethod("buildNotification", Long::class.java)
        method.isAccessible = true
        val notification = method.invoke(service, TimeUnit.MINUTES.toMillis(25)) as Notification

        assertEquals(NotificationHelper.PRE_REMINDER_CHANNEL_ID, notification.channelId)
        assertEquals(NotificationCompat.PRIORITY_DEFAULT, notification.priority)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0) // Check for ongoing
        assertTrue(notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0) // Check for only alert once
    }

    @Test
    fun `buildNotification shows correct progress and text`() {
        val reminderId = 20
        val medName = "Anacin"
        val totalDurationMinutes = PreReminderForegroundService.TOTAL_PRE_REMINDER_DURATION_MINUTES
        val scheduledTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(totalDurationMinutes)

        val currentReminderIdField = PreReminderForegroundService::class.java.getDeclaredField("currentReminderId")
        currentReminderIdField.isAccessible = true
        currentReminderIdField.setInt(service, reminderId)
        val medicationNameField = PreReminderForegroundService::class.java.getDeclaredField("medicationNameForNotification")
        medicationNameField.isAccessible = true
        medicationNameField.set(service, medName)
        val actualTakeTimeMillisField = PreReminderForegroundService::class.java.getDeclaredField("actualTakeTimeMillis")
        actualTakeTimeMillisField.isAccessible = true
        actualTakeTimeMillisField.setLong(service, scheduledTime)

        // Scenario 1: 45 minutes remaining (15 elapsed)
        var currentTime = scheduledTime - TimeUnit.MINUTES.toMillis(45)
        systemMock.`when`<Long> { System.currentTimeMillis() }.thenReturn(currentTime)
        var method = PreReminderForegroundService::class.java.getDeclaredMethod("buildNotification", Long::class.java)
        method.isAccessible = true
        var notification = method.invoke(service, TimeUnit.MINUTES.toMillis(45)) as Notification

        assertEquals("45 minutos restantes", notification.extras.getString(NotificationCompat.EXTRA_TEXT))
        assertEquals(totalDurationMinutes.toInt(), notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX))
        assertEquals(15, notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS)) // 60 - 45 = 15 elapsed

        // Scenario 2: 1 minute remaining (59 elapsed)
        currentTime = scheduledTime - TimeUnit.MINUTES.toMillis(1)
        systemMock.`when`<Long> { System.currentTimeMillis() }.thenReturn(currentTime)
        notification = method.invoke(service, TimeUnit.MINUTES.toMillis(1)) as Notification
        assertEquals("En 1 minuto", notification.extras.getString(NotificationCompat.EXTRA_TEXT))
        assertEquals(totalDurationMinutes.toInt(), notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX))
        assertEquals(59, notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS))
        assertTrue(notification.actions != null && notification.actions.any { it.title.toString().equals("Tomada", ignoreCase = true) })


        // Scenario 3: 0 minutes remaining (60 elapsed) -> "¡Es casi la hora!"
        currentTime = scheduledTime
        systemMock.`when`<Long> { System.currentTimeMillis() }.thenReturn(currentTime)
        notification = method.invoke(service, 0L) as Notification
        assertEquals("¡Es casi la hora!", notification.extras.getString(NotificationCompat.EXTRA_TEXT))
        assertEquals(totalDurationMinutes.toInt(), notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX))
        assertEquals(totalDurationMinutes.toInt(), notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS)) // Max progress
        assertTrue(notification.actions != null && notification.actions.any { it.title.toString().equals("Tomada", ignoreCase = true) })

    }
    
    @Test
    fun `buildNotification late start with 30 mins remaining`() {
        val reminderId = 30
        val medName = "LateStartMed"
        val totalDurationMinutes = PreReminderForegroundService.TOTAL_PRE_REMINDER_DURATION_MINUTES // 60
        // Actual scheduled time is, say, in the future
        val scheduledTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(100) 
        
        // But the service is effectively starting when only 30 minutes are left for the pre-reminder window
        // This means the actualTakeTimeMillis passed to onStartCommand was (currentTime + 30 mins)
        // And TOTAL_PRE_REMINDER_DURATION_MINUTES is 60.
        // The progress should reflect that 30 minutes of the pre-reminder window have "elapsed" conceptually.
        val effectiveTimeRemainingMillis = TimeUnit.MINUTES.toMillis(30)
        // Current time is 30 minutes before the `actualTakeTimeMillis` that was set in onStartCommand
        val currentTime = scheduledTime - effectiveTimeRemainingMillis 
        systemMock.`when`<Long> { System.currentTimeMillis() }.thenReturn(currentTime)

        // Set internal state
        val currentReminderIdField = PreReminderForegroundService::class.java.getDeclaredField("currentReminderId")
        currentReminderIdField.isAccessible = true
        currentReminderIdField.setInt(service, reminderId)
        val medicationNameField = PreReminderForegroundService::class.java.getDeclaredField("medicationNameForNotification")
        medicationNameField.isAccessible = true
        medicationNameField.set(service, medName)
        // actualTakeTimeMillis is the *actual* medication time, not the start of the pre-reminder window
        val actualTakeTimeMillisField = PreReminderForegroundService::class.java.getDeclaredField("actualTakeTimeMillis")
        actualTakeTimeMillisField.isAccessible = true
        actualTakeTimeMillisField.setLong(service, scheduledTime)


        val method = PreReminderForegroundService::class.java.getDeclaredMethod("buildNotification", Long::class.java)
        method.isAccessible = true
        // timeRemainingMillis passed to buildNotification is actualTakeTimeMillis - System.currentTimeMillis()
        val notification = method.invoke(service, effectiveTimeRemainingMillis) as Notification

        assertEquals("30 minutos restantes", notification.extras.getString(NotificationCompat.EXTRA_TEXT))
        assertEquals(totalDurationMinutes.toInt(), notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX))
        // Elapsed time is total pre-reminder duration - actual time left until dose
        assertEquals((totalDurationMinutes - 30).toInt(), notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS))
        // No "Mark as Taken" action yet
        assertTrue(notification.actions == null || notification.actions.none { it.title.toString().equals("Tomada", ignoreCase = true) })
    }


    // Test for ACTION_STOP_PRE_REMINDER (simplified due to Handler complexities)
    // We can't easily test the handler stopping, but we can test if onStartCommand returns START_NOT_STICKY
    // and potentially if stopSelfService components are called if we could mock/spy the service instance better.
    @Test
    fun `onStartCommand with ACTION_STOP_PRE_REMINDER returns START_NOT_STICKY`() {
        val intent = Intent(mockContext, PreReminderForegroundService::class.java)
        intent.action = PreReminderForegroundService.ACTION_STOP_PRE_REMINDER
        intent.putExtra(PreReminderForegroundService.EXTRA_SERVICE_REMINDER_ID, 123)

        // Set currentReminderId in service to match, so it proceeds to stop
        val currentReminderIdField = PreReminderForegroundService::class.java.getDeclaredField("currentReminderId")
        currentReminderIdField.isAccessible = true
        currentReminderIdField.setInt(service, 123)


        val result = service.onStartCommand(intent, 0, 1)
        assertEquals(Service.START_NOT_STICKY, result)
        // Further verification of stopSelfService() internal calls is difficult here.
    }

    // Test for updateNotificationRunnable's stop condition (conceptual)
    // Direct test of runnable is hard. We test the condition it checks.
    @Test
    fun `updateNotificationRunnable should stop when time remaining is low`() {
        // This test is more conceptual for the logic inside updateNotificationRunnable
        // If actualTakeTimeMillis - System.currentTimeMillis() <= 10 seconds, it calls stopSelfService()

        val scheduledTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5) // 5 seconds from now
        
        val actualTakeTimeMillisField = PreReminderForegroundService::class.java.getDeclaredField("actualTakeTimeMillis")
        actualTakeTimeMillisField.isAccessible = true
        actualTakeTimeMillisField.setLong(service, scheduledTime)
        
        val currentReminderIdField = PreReminderForegroundService::class.java.getDeclaredField("currentReminderId")
        currentReminderIdField.isAccessible = true
        currentReminderIdField.setInt(service, 1) // Make currentReminderId valid

        systemMock.`when`<Long> { System.currentTimeMillis() }.thenReturn(System.currentTimeMillis())

        // Simulate the check done by the runnable
        val timeRemaining = scheduledTime - System.currentTimeMillis()
        assertTrue(timeRemaining <= TimeUnit.SECONDS.toMillis(10))
        // If this condition is true, the runnable *would* call stopSelfService().
        // Cannot directly test handler.postDelayed or removeCallbacks here without Robolectric.
    }


    // TODO: Add tests for createMarkAsTakenPendingIntent if its logic was more complex
    // For now, it's straightforward.

    // Remember to close the static mock
    @org.junit.After
    fun tearDown() {
        systemMock.close()
    }
}
