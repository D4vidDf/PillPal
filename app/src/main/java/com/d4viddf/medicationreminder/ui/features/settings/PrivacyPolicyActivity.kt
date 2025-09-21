package com.d4viddf.medicationreminder.ui.features.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.d4viddf.medicationreminder.ui.theme.MedicationReminderTheme

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedicationReminderTheme {
                PrivacyPolicyScreen(onBack = { finish() })
            }
        }
    }
}
