package com.d4viddf.medicationreminder.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }

    suspend fun sendReminderTakenMessage(reminderId: Int) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.forEach { node ->
                messageClient.sendMessage(
                    node.id,
                    "/reminder-taken",
                    reminderId.toString().toByteArray()
                ).await()
                Log.i("WearRepository", "Sent reminder taken message for reminder $reminderId to node ${node.id}")
            }
        } catch (e: Exception) {
            Log.e("WearRepository", "Failed to send reminder taken message", e)
        }
    }
}
