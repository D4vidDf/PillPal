package com.d4viddf.medicationreminder.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import app.cash.turbine.test
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.ui.features.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    // Mock dependencies
    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAudioManager: AudioManager

    @Mock
    private lateinit var mockUserPreferencesRepository: UserPreferencesRepository // Though not used for volume tests, it's a constructor param

    // Test dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher()

    // Subject under test
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Configure mocks
        Mockito.`when`(mockApplication.applicationContext).thenReturn(mockContext)
        Mockito.`when`(mockContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager)

        // Mock AudioManager behavior
        Mockito.`when`(mockAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)).thenReturn(15) // Changed to STREAM_ALARM
        Mockito.`when`(mockAudioManager.getStreamVolume(AudioManager.STREAM_ALARM)).thenReturn(7) // Changed to STREAM_ALARM

        // Initialize ViewModel
        viewModel = SettingsViewModel(mockUserPreferencesRepository, mockApplication)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset the main dispatcher to the original one
    }

    @Test
    fun `init loads initial volume and max volume correctly`() = runTest {
        // Advance time to allow StateFlows to initialize if they are collected lazily or with delays.
        // For current implementation with direct init, this might not be strictly necessary but good practice.
        testDispatcher.scheduler.advanceUntilIdle()


        assertEquals(15, viewModel.maxVolume.value, "Max volume should be initialized from AudioManager")
        assertEquals(7, viewModel.currentVolume.value, "Current volume should be initialized from AudioManager")

        // Using Turbine for more robust StateFlow testing (optional if direct value access is sufficient)
        viewModel.maxVolume.test {
            assertEquals(15, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.currentVolume.test {
            assertEquals(7, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setVolume updates currentVolume StateFlow and calls AudioManager`() = runTest {
        val newVolume = 10

        // Act
        viewModel.setVolume(newVolume)
        testDispatcher.scheduler.advanceUntilIdle()


        // Assert StateFlow update
        assertEquals(newVolume, viewModel.currentVolume.value, "currentVolume StateFlow should be updated to newVolume")

        // Verify AudioManager interaction
        Mockito.verify(mockAudioManager).setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0) // Changed to STREAM_ALARM

        // Using Turbine for more robust StateFlow testing
        viewModel.currentVolume.test {
            // The initial value is 7, then it's set to 10.
            // Depending on how test is run and if previous state is carried or re-initialized for test scope.
            // If runTest creates a fresh scope, it should be 7, then 10.
            // If the VM instance is reused across tests without re-init in setUp for *this specific test*,
            // it might be different. Let's assume fresh init via setUp.
            assertEquals(7, awaitItem()) // Initial value from setUp
            viewModel.setVolume(newVolume) // Re-trigger inside test to ensure flow emission capture
            assertEquals(newVolume, awaitItem()) // New value
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setVolume with 0 updates currentVolume StateFlow and calls AudioManager`() = runTest {
        val newVolume = 0

        // Act
        viewModel.setVolume(newVolume)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert StateFlow update
        assertEquals(newVolume, viewModel.currentVolume.value, "currentVolume StateFlow should be updated to 0")

        // Verify AudioManager interaction
        Mockito.verify(mockAudioManager).setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0) // Changed to STREAM_ALARM
    }

    @Test
    fun `setVolume to maxVolume updates currentVolume StateFlow and calls AudioManager`() = runTest {
        val newVolume = 15 // Max volume as per mock setup

        // Act
        viewModel.setVolume(newVolume)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert StateFlow update
        assertEquals(newVolume, viewModel.currentVolume.value, "currentVolume StateFlow should be updated to maxVolume")

        // Verify AudioManager interaction
        Mockito.verify(mockAudioManager).setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0) // Changed to STREAM_ALARM
    }
    
    @Test
    fun `refreshCurrentVolume updates currentVolume from AudioManager`() = runTest {
        val expectedVolumeAfterExternalChange = 5
        Mockito.`when`(mockAudioManager.getStreamVolume(AudioManager.STREAM_ALARM)).thenReturn(expectedVolumeAfterExternalChange) // Changed to STREAM_ALARM

        viewModel.refreshCurrentVolume()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(expectedVolumeAfterExternalChange, viewModel.currentVolume.value, "currentVolume should be updated after refresh")

        viewModel.currentVolume.test {
             // Initial value is 7 from setUp
            assertEquals(7, awaitItem())
            
            // Simulate external change and refresh
            Mockito.`when`(mockAudioManager.getStreamVolume(AudioManager.STREAM_ALARM)).thenReturn(expectedVolumeAfterExternalChange) // Changed to STREAM_ALARM
            viewModel.refreshCurrentVolume()
            
            assertEquals(expectedVolumeAfterExternalChange, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
