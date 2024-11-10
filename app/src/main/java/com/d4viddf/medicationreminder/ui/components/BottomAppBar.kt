package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BottomNavBar(
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit
) {
    BottomAppBar {
        IconButton(onClick = onHomeClick) {
            Icon(Icons.Default.Home, contentDescription = "Home")
        }
        Spacer(modifier = Modifier.weight(1f)) // Spacer to push the middle button to the center
        FloatingActionButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}
