package com.d4viddf.medreminder.wear.services

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // Ensure this import is present
import java.nio.charset.StandardCharsets

class WearableCommunicationService(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val messageClient by lazy { Wearable.getMessageClient(context) }

    companion object {
        private const val TAG = "WearCommService"
        private const val PATH_MARK_AS_TAKEN = "/mark_as_taken"
        // Add other paths as needed, e.g., for snooze
        // private const val PATH_SNOOZE_REMINDER = "/snooze_reminder"
    }

    fun sendMarkAsTakenMessage(reminderId: Int) {
        coroutineScope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected phone nodes found to send mark_as_taken message.")
                    // Optionally, handle this case, e.g., queue the message or notify the user
                    return@launch
                }
                // Send the message to all connected phone nodes (typically one)
                nodes.forEach { node: Node -> // Explicitly type node
                    val reminderIdBytes = reminderId.toString().toByteArray(StandardCharsets.UTF_8)
                    messageClient.sendMessage(node.id, PATH_MARK_AS_TAKEN, reminderIdBytes)
                        .addOnSuccessListener {
                            Log.i(TAG, "Mark_as_taken message sent to ${node.displayName} for reminder ID: $reminderId")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send mark_as_taken message to ${node.displayName} for reminder ID: $reminderId", e)
                        }
                        .await() // Make it sequential for logging if needed, or remove await for fire-and-forget
                }
            } catch (e: ApiException) {
                Log.e(TAG, "API exception while sending mark_as_taken message: ${e.statusCode}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Generic exception while sending mark_as_taken message", e)
            }
        }
    }

    // Example for a snooze function if you implement it later
    /*
    fun sendSnoozeMessage(reminderId: Int) {
        coroutineScope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected phone nodes found to send snooze message.")
                    return@launch
                }
                nodes.forEach { node ->
                    val reminderIdBytes = reminderId.toString().toByteArray(StandardCharsets.UTF_8)
                    messageClient.sendMessage(node.id, PATH_SNOOZE_REMINDER, reminderIdBytes)
                        .addOnSuccessListener {
                            Log.i(TAG, "Snooze message sent to ${node.displayName} for reminder ID: $reminderId")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send snooze message to ${node.displayName} for reminder ID: $reminderId", e)
                        }
                        .await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while sending snooze message", e)
            }
        }
    }
    */
}
