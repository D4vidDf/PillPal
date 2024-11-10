package com.d4viddf.medicationreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import com.d4viddf.medicationreminder.ui.MedicationReminderApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedicationReminderApp() // Call MedicationReminderApp as the root composable
        }
    }
}
