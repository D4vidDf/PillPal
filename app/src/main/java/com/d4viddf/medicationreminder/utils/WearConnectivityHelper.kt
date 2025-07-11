package com.d4viddf.medicationreminder.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import androidx.core.content.ContextCompat // Added for getMainExecutor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearConnectivityHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
    private val capabilityClient: CapabilityClient = Wearable.getCapabilityClient(context)
    private val executor: Executor = Executors.newSingleThreadExecutor()

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

    suspend fun isAppInstalledOnNode(nodeId: String): Boolean {
        if (!isWatchConnected()) {
            Log.d(TAG, "isAppInstalledOnNode: No watch connected, returning false for node $nodeId.")
            return false
        }
        try {
            Log.d(TAG, "isAppInstalledOnNode: Checking capability $WATCH_APP_CAPABILITY for specific node $nodeId")
            val capabilityInfo = capabilityClient
                .getCapability(WATCH_APP_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
            // Check if the specific node is in the list of nodes with the capability and is nearby
            val nodeInCapability = capabilityInfo.nodes.find { it.id == nodeId }
            val isInstalledOnThisNode = nodeInCapability != null && nodeInCapability.isNearby
            Log.d(TAG, "isAppInstalledOnNode: App installed on node $nodeId and nearby: $isInstalledOnThisNode")
            return isInstalledOnThisNode
        } catch (e: Exception) {
            Log.e(TAG, "isAppInstalledOnNode: Error checking capability for node $nodeId", e)
            return false
        }
    }

    fun observeConnectionStatus(): Flow<Boolean> = flow {
        emit(isWatchConnected())
        // A more robust implementation would use a listener for NodeClient or CapabilityClient
    }

    suspend fun isWatchAppInstalled(): Boolean {
        if (!isWatchConnected()) {
            Log.d(TAG, "isWatchAppInstalled: No watch connected, returning false.")
            return false
        }
        try {
            Log.d(TAG, "isWatchAppInstalled: Attempting to get capability: $WATCH_APP_CAPABILITY")
            val capabilityInfo = capabilityClient
                .getCapability(WATCH_APP_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
            Log.d(TAG, "isWatchAppInstalled: Capability info found for '$WATCH_APP_CAPABILITY'. Nodes: ${capabilityInfo.nodes.joinToString { "${it.displayName}(${it.id}, nearby=${it.isNearby})" }}")
            if (capabilityInfo.nodes.isEmpty()) {
                Log.w(TAG, "isWatchAppInstalled: Capability $WATCH_APP_CAPABILITY found, but no nodes associated with it.")
            }
            val isInstalledAndNearby = capabilityInfo.nodes.any { it.isNearby }
            Log.d(TAG, "isWatchAppInstalled: App is installed and nearby on at least one node: $isInstalledAndNearby")
            return isInstalledAndNearby
        } catch (e: Exception) {
            Log.e(TAG, "isWatchAppInstalled: Error checking if watch app is installed for capability '$WATCH_APP_CAPABILITY'", e)
            try {
                Log.d(TAG, "isWatchAppInstalled: Attempting to fetch all capabilities as a fallback.")
                val allCapabilities = capabilityClient.getAllCapabilities(CapabilityClient.FILTER_REACHABLE).await()
                Log.d(TAG, "isWatchAppInstalled: All reachable capabilities on connected nodes: ${allCapabilities.keys.joinToString()}")
                allCapabilities.forEach { (capName, capInfoValue) ->
                    Log.d(TAG, "isWatchAppInstalled: Details for Cap: '$capName', Nodes: ${capInfoValue.nodes.joinToString { "${it.displayName}(${it.id}, nearby=${it.isNearby})" }}")
                }
            } catch (allCapError: Exception) {
                Log.e(TAG, "isWatchAppInstalled: Error fetching all capabilities during fallback", allCapError)
            }
            return false
        }
    }

    suspend fun getConnectedWatchNodeId(): String? {
        try {
            Log.d(TAG, "getConnectedWatchNodeId: Attempting to find node with capability $WATCH_APP_CAPABILITY")
            val capabilityInfo = capabilityClient
                .getCapability(WATCH_APP_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
            val nodeWithCapability = capabilityInfo.nodes.firstOrNull { it.isNearby }
            if (nodeWithCapability != null) {
                Log.d(TAG, "getConnectedWatchNodeId: Found node with capability: ${nodeWithCapability.id}")
                return nodeWithCapability.id
            }
            Log.d(TAG, "getConnectedWatchNodeId: No node found with capability, falling back to any connected node.")
            val connectedNodes = nodeClient.connectedNodes.await()
            val firstNodeId = connectedNodes.firstOrNull()?.id
            if (firstNodeId != null) {
                Log.d(TAG, "getConnectedWatchNodeId: Found first connected node: $firstNodeId")
            } else {
                Log.w(TAG, "getConnectedWatchNodeId: No connected nodes found at all.")
            }
            return firstNodeId
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected watch node ID", e)
            return null
        }
    }

    fun openPlayStoreOnWatch(nodeId: String) {
        val watchAppPackageName = "com.d4viddf.medicationreminder"
        val playStoreUri = Uri.parse("market://details?id=$watchAppPackageName")
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(playStoreUri)
            .addCategory(Intent.CATEGORY_BROWSABLE)

        try {
            Log.d(TAG, "openPlayStoreOnWatch: Attempting to open Play Store on node $nodeId for package $watchAppPackageName")
            val remoteActivityHelper = RemoteActivityHelper(context, executor)
            val resultFuture = remoteActivityHelper.startRemoteActivity(intent, nodeId)

            Futures.addCallback(resultFuture, object : FutureCallback<Void> {
                override fun onSuccess(result: Void?) {
                    Log.i(TAG, "Successfully requested Play Store on watch $nodeId")
                }

                override fun onFailure(t: Throwable) {
                    Log.e(TAG, "Failed to request Play Store on watch $nodeId", t)
                }
            }, ContextCompat.getMainExecutor(context)) // Using main executor for callback for safety if UI updates were needed

        } catch (e: Exception) {
            Log.e(TAG, "Exception while trying to open Play Store on watch for node $nodeId: ${e.message}", e)
        }
    }
}
