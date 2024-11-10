package com.d4viddf.medicationreminder.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.d4viddf.medicationreminder.ui.screens.AppNavigation
import com.d4viddf.medicationreminder.ui.theme.MedicationReminderLightTheme

@Composable
fun MedicationReminderApp() {
    MedicationReminderLightTheme {
        // A surface container using the 'surface' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            val navController = rememberNavController()
            AppNavigation(navController = navController) // Pass the navController to AppNavigation
        }
    }
}
