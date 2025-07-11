package com.d4viddf.medicationreminder.wear.presentation

import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class DataLayerService : WearableListenerService() {

    private val CAPABILITY_NAME = "medication_reminder_wear_app"
    private val TAG = "DataLayerService"

    private lateinit var capabilityClient: CapabilityClient

    override fun onCreate() {
        super.onCreate()
        capabilityClient = Wearable.getCapabilityClient(this)
        Log.d(TAG, "Service created, advertising capability.")
        // Advertise the capability as soon as the service is created
        capabilityClient.addLocalCapability(CAPABILITY_NAME)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed, removing capability.")
        // Clean up when the service is destroyed
        capabilityClient.removeLocalCapability(CAPABILITY_NAME)
    }

    // This is called when a connected device's capabilities change
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        super.onCapabilityChanged(capabilityInfo)
        Log.d(TAG, "Capability Changed: $capabilityInfo")
        // You can add logic here if you need to react to phone app changes
    }

    // This is called when you receive a message from the phone
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "Message received: ${messageEvent.path}")
        // Handle incoming messages from the phone app here
    }

    // This is called when data items are changed (synced)
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "Data changed. Event count: ${dataEvents.count}")
        // Handle incoming data from the phone app here
    }
}