package com.d4viddf.medicationreminder.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors

class WearConnectivityHelper(private val context: Context) {

    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val executor = Executors.newSingleThreadExecutor() // Executor for RemoteActivityHelper

    companion object {
        private const val TAG = "WearConnectivityHelper"
        const val WATCH_APP_CAPABILITY = "medication_reminder_wear_app"
    }

    suspend fun isWatchConnected(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for connected watch", e)
            false
        }
    }

    fun observeConnectionStatus(): Flow<Boolean> = flow {
        emit(isWatchConnected())
        // A more robust implementation would use a listener for NodeClient or CapabilityClient
    }

    suspend fun isWatchAppInstalled(): Boolean {
        if (!isWatchConnected()) {
            return false
        }
        return try {
            val capabilityInfo = capabilityClient
                .getCapability(WATCH_APP_CAPABILITY, com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE)
                .await()
            capabilityInfo.nodes.any { it.isNearby }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if watch app is installed", e)
            false
        }
    }

    suspend fun getConnectedWatchNodeId(): String? {
        try {
            val capabilityInfo = capabilityClient
                .getCapability(WATCH_APP_CAPABILITY, com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE)
                .await()
            val nodeWithCapability = capabilityInfo.nodes.firstOrNull { it.isNearby }
            if (nodeWithCapability != null) {
                return nodeWithCapability.id
            }
            val connectedNodes = nodeClient.connectedNodes.await()
            return connectedNodes.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected watch node ID", e)
            return null
        }
    }

    fun openPlayStoreOnWatch(nodeId: String) {
        val watchAppPackageName = "com.d4viddf.medicationreminder.wear" // Ensure this is correct
        val playStoreUri = Uri.parse("market://details?id=$watchAppPackageName")
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(playStoreUri)
            .addCategory(Intent.CATEGORY_BROWSABLE)

        try {
            val remoteActivityHelper = RemoteActivityHelper(context, executor)
            val resultFuture = remoteActivityHelper.startRemoteActivity(intent, nodeId)

            Futures.addCallback(resultFuture, object : FutureCallback<Void> {
                override fun onSuccess(result: Void?) {
                    Log.i(TAG, "Successfully requested Play Store on watch $nodeId")
                }

                override fun onFailure(t: Throwable) {
                    Log.e(TAG, "Failed to request Play Store on watch $nodeId", t)
                }
            }, executor) // Can use ContextCompat.getMainExecutor(context) for UI thread callback if needed

        } catch (e: Exception) {
            Log.e(TAG, "Exception while trying to open Play Store on watch: ${e.message}", e)
        }
    }
}
