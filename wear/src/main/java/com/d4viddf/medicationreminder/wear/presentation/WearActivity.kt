package com.d4viddf.medicationreminder.wear.presentation

import android.os.Bundle
import android.util.Log // Added for logging
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.d4viddf.medicationreminder.wear.presentation.components.WearApp

class WearActivity : ComponentActivity() {

    private lateinit var capabilityClient: CapabilityClient
    // Ensure this capability name matches exactly what's in wear/src/main/res/xml/wear.xml
    private val WEAR_APP_CAPABILITY_NAME = "medication_reminder_wear_app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        capabilityClient = Wearable.getCapabilityClient(this)

        setContent {
            val wearViewModel: WearViewModel = viewModel(factory = WearViewModelFactory(application))
            WearApp(wearViewModel = wearViewModel) // This is components.WearApp
        }
    }

    override fun onResume() {
        super.onResume()
        capabilityClient.addLocalCapability(WEAR_APP_CAPABILITY_NAME)
            .addOnSuccessListener {
                Log.d("WearActivity", "Successfully advertised capability: $WEAR_APP_CAPABILITY_NAME")
            }
            .addOnFailureListener { e -> // Added exception logging
                Log.e("WearActivity", "Failed to advertise capability: $WEAR_APP_CAPABILITY_NAME", e)
            }
    }

    override fun onPause() {
        super.onPause()
        capabilityClient.removeLocalCapability(WEAR_APP_CAPABILITY_NAME)
            .addOnSuccessListener {
                Log.d("WearActivity", "Successfully removed capability: $WEAR_APP_CAPABILITY_NAME")
            }
            .addOnFailureListener { e -> // Added exception logging
                Log.e("WearActivity", "Failed to remove capability: $WEAR_APP_CAPABILITY_NAME", e)
            }
    }
}
