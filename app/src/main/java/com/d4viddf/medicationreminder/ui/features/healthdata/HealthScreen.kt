package com.d4viddf.medicationreminder.ui.features.healthdata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.ui.navigation.Screen

@Composable
fun HealthScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { navController.navigate(Screen.WaterIntake.route) }) {
            Text("Water Intake")
        }
        Button(onClick = { navController.navigate(Screen.Weight.route) }) {
            Text("Weight")
        }
        Button(onClick = { navController.navigate(Screen.BodyTemperature.route) }) {
            Text("Body Temperature")
        }
    }
}
