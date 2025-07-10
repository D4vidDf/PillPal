package com.d4viddf.medicationreminder.wear.services

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import com.google.android.gms.tasks.Tasks // Import for blocking Tasks.await()
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
// import kotlinx.coroutines.tasks.await // Comment out or remove if not resolving
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
                // Using blocking Tasks.await() as a fallback
                val nodeListTask = Wearable.getNodeClient(context).connectedNodes
                val nodes: List<Node> = Tasks.await(nodeListTask)

                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected phone nodes found to send mark_as_taken message.")
                    return@launch
                }

                nodes.forEach { node -> // node is already of type Node here
                    val reminderIdBytes = reminderId.toString().toByteArray(StandardCharsets.UTF_8)
                    val sendMessageTask = messageClient.sendMessage(node.id, PATH_MARK_AS_TAKEN, reminderIdBytes)
                    // Using blocking Tasks.await() here too for consistency in this fallback
                    Tasks.await(sendMessageTask)
                    Log.i(TAG, "Mark_as_taken message sent to ${node.displayName} for reminder ID: $reminderId (using blocking await)")
                    // Note: AddOnSuccessListener/FailureListener are harder to use with blocking Tasks.await()
                    // unless you wrap them or check task.isSuccessful, task.exception etc.
                    // For simplicity in this fallback, direct await is used.
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
