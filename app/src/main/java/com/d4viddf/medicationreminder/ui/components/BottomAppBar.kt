package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.ui.theme.DarkGreen
import com.d4viddf.medicationreminder.ui.theme.White

@Composable
fun BottomNavBar(
    selectedIndex: Int, // Add a parameter to track the selected index
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit
) {
    // Use NavigationBar for the bottom bar
    NavigationBar(
        containerColor = White // Set the background color of the NavigationBar
    ) {
        // Use NavigationBarItem for each item in the bar
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            selected = selectedIndex == 0, // Set selected state based on index
            onClick = onHomeClick,
            // Add background color for selected state
            colors = NavigationBarItemDefaults.colors(
            )
        )
        // Empty NavigationBarItem for spacing, you can adjust the weight as needed
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = DarkGreen,
            contentColor = White
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
        }
// Another NavigationBarItem for the Settings
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            selected = selectedIndex == 1, // Set selected state based on index
            onClick = onSettingsClick,
            // Add background color for selected state
            colors = NavigationBarItemDefaults.colors(
            )
        )
    }

}