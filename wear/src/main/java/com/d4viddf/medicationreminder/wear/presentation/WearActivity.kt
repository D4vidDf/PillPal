package com.d4viddf.medicationreminder.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.d4viddf.medicationreminder.wear.presentation.components.WearApp // Ensure this is the correct path to your M3 WearApp

class WearActivity : ComponentActivity() {

    private lateinit var capabilityClient: CapabilityClient
    private val CAPABILITY_NAME = "medication_reminder_wear_app_capability" // Corrected to match wear.xml

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        capabilityClient = Wearable.getCapabilityClient(this)

        setContent {
            // ViewModel is obtained using the viewModel() delegate,
            // factory is provided for Application context.
            val wearViewModel: WearViewModel = viewModel(factory = WearViewModelFactory(application))

            // Call the WearApp composable from the components package
            WearApp(wearViewModel = wearViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        capabilityClient.addLocalCapability(CAPABILITY_NAME)
            .addOnSuccessListener {
                Log.d("WearActivity", "Successfully advertised capability: $CAPABILITY_NAME")
            }
            .addOnFailureListener {
                Log.e("WearActivity", "Failed to advertise capability: $CAPABILITY_NAME", it)
            }
    }

    override fun onPause() {
        super.onPause()
        capabilityClient.removeLocalCapability(CAPABILITY_NAME)
            .addOnSuccessListener {
                Log.d("WearActivity", "Successfully removed capability: $CAPABILITY_NAME")
            }
            .addOnFailureListener {
                Log.e("WearActivity", "Failed to remove capability: $CAPABILITY_NAME", it)
            }
    }
}
