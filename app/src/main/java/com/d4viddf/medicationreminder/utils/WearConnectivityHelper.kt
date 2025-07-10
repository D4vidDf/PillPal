package com.d4viddf.medicationreminder.utils

import android.content.Context
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import android.util.Log

class WearConnectivityHelper(context: Context) {

import com.google.android.gms.wearable.Node
import kotlinx.coroutines.guava.await

class WearConnectivityHelper(private val context: Context) { // Made context a property for easier access

    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)


    companion object {
        private const val TAG = "WearConnectivityHelper"
        // Add your watch app's capability string here, as defined in wear.xml
        // This should match the value in wear/src/main/res/values/wear.xml (e.g., <string name="medication_reminder_wear_app_capability_name">medication_reminder_wear_app</string>)
        const val WATCH_APP_CAPABILITY = "medication_reminder_wear_app" // Made public for potential use elsewhere
    }

    /**
     * Checks if there is at least one Wear OS device connected.
     * @return true if a device is connected, false otherwise.
     */
    suspend fun isWatchConnected(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for connected watch", e)
            false
        }
    }

    /**
     * Provides a Flow that emits the connection status.
     * This can be used to observe connection changes.
     */
    fun observeConnectionStatus(): Flow<Boolean> = flow {
        // Initial check
        emit(isWatchConnected())
        // Ideally, you'd also listen for connection events from NodeClient.CapabilityChangedListener
        // For simplicity in this example, we're just doing an initial check.
        // A more robust implementation would involve a listener.
    }

    /**
     * Checks if the specific watch app is installed on any connected device.
     * Requires the watch app to declare a capability in its wear.xml.
     * e.g., <capability android:name="medication_reminder_wear_app" />
     * @return true if the app is installed on at least one connected node, false otherwise.
     */
    suspend fun isWatchAppInstalled(): Boolean {
        if (!isWatchConnected()) {
            return false
        }
        return try {
            val capabilityInfo = capabilityClient
                .getCapability(WATCH_APP_CAPABILITY, com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE)
                .await()
            capabilityInfo.nodes.any { it.isNearby } // Check if any of the nodes with the capability is reachable
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if watch app is installed", e)
            false
        }
    }

    /**
     * Gets the node ID of a connected watch.
     * Prefers a node that has the app capability, but falls back to any connected node.
     * @return The node ID of a connected watch, or null if no watch is connected.
     */
    suspend fun getConnectedWatchNodeId(): String? {
        try {
            // First, try to find a node with the app capability
            val capabilityInfo = capabilityClient
                .getCapability(WATCH_APP_CAPABILITY, com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE)
                .await()
            val nodeWithCapability = capabilityInfo.nodes.firstOrNull { it.isNearby }
            if (nodeWithCapability != null) {
                return nodeWithCapability.id
            }

            // If no node with capability, fall back to any connected node
            val connectedNodes = nodeClient.connectedNodes.await()
            return connectedNodes.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected watch node ID", e)
            return null
        }
    }

    /**
     * Opens the Play Store on the specified watch node to the app's listing.
     * @param nodeId The ID of the watch node.
     */
    fun openPlayStoreOnWatch(nodeId: String) {
        // Replace with your app's package name for the watch app
        val watchAppPackageName = "com.d4viddf.medicationreminder.wear" // Assuming this is your watch app's package name
        val playStoreUri = android.net.Uri.parse("market://details?id=$watchAppPackageName")

        try {
            com.google.android.gms.wearable.RemoteActivityHelper(context)
                .startRemoteActivity(playStoreUri, nodeId)
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully requested Play Store on watch $nodeId")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to request Play Store on watch $nodeId", exception)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while trying to open Play Store on watch", e)
            // Fallback or error message for the user might be needed here
            // For example, if RemoteActivityHelper is not available or throws an exception during instantiation
        }
    }
}
