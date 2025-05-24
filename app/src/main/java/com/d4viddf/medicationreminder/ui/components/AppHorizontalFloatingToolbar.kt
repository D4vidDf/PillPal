package com.d4viddf.medicationreminder.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.expressive.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.expressive.HorizontalFloatingToolbar
import androidx.compose.material3.expressive.FloatingToolbarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.d4viddf.medicationreminder.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppHorizontalFloatingToolbar(
    onHomeClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalFloatingToolbar(
        modifier = modifier,
        expanded = true, // Keep all items visible
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(), // Or vibrantFloatingToolbarColors()
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Medication" // TODO: Use stringResource
                )
            }
        }
    ) {
        // Content for the toolbar (RowScope)
        IconButton(onClick = onHomeClick) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_home_24),
                contentDescription = "Home" // TODO: Use stringResource
            )
        }
        IconButton(onClick = onCalendarClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_calendar_today_24),
                contentDescription = "Calendar" // TODO: Use stringResource
            )
        }
        IconButton(onClick = onProfileClick) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_person_24),
                contentDescription = "Profile" // TODO: Use stringResource
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_settings_24),
                contentDescription = "Settings" // TODO: Use stringResource
            )
        }
    }
}
