package com.d4viddf.medicationreminder.ui.features.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import com.d4viddf.medicationreminder.ui.theme.MedicationReminderTheme

class PermissionsRationaleActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedicationReminderTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Permission Rationale") }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        Text(
                            text = "We need these permissions to read and write your health data to Health Connect. " +
                                "This allows you to see a complete picture of your health across different apps."
                        )
                        Button(onClick = { finish() }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}
