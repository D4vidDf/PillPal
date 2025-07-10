package com.d4viddf.medicationreminder.services

import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint

import android.util.Log
import com.d4viddf.medicationreminder.data.MedicationReminderRepository
import com.google.android.gms.wearable.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DataLayerListenerService : WearableListenerService() {

    @Inject
    lateinit var medicationReminderRepository: MedicationReminderRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Message received: ${messageEvent.path}")

        when (messageEvent.path) {
            PATH_MARK_AS_TAKEN -> {
                val reminderIdString = String(messageEvent.data, StandardCharsets.UTF_8)
                val reminderId = reminderIdString.toIntOrNull()
                if (reminderId != null) {
                    Log.d(TAG, "Received mark_as_taken for reminder ID: $reminderId")
                    serviceScope.launch {
                        try {
                            val currentTimeIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                            val success = medicationReminderRepository.markReminderAsTaken(reminderId, currentTimeIso)
                            if (success) {
                                Log.i(TAG, "Reminder $reminderId marked as taken successfully via watch.")
                                // Optionally, send a confirmation message back to the watch
                            } else {
                                Log.e(TAG, "Failed to mark reminder $reminderId as taken via watch.")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error marking reminder as taken: ${e.message}", e)
                        }
                    }
                } else {
                    Log.e(TAG, "Could not parse reminder ID from message data: $reminderIdString")
                }
            }
            // Add other message paths here if needed
            else -> {
                Log.w(TAG, "Unknown message path: ${messageEvent.path}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel coroutines when the service is destroyed
    }

    companion object {
        private const val TAG = "DataLayerListenerSvc"
        private const val PATH_MARK_AS_TAKEN = "/mark_as_taken"
        // Define other paths as constants here
    }
}
