package com.d4viddf.medicationreminder.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.R

@Composable
fun AppNavigationRail(
    onHomeClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentRoute: String? = null // Optional: To determine selected state
) {
    NavigationRail(modifier = modifier) {
        NavigationRailItem(
            icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.add_medication_title)) },
            selected = false, // FAB-like action, typically not "selected"
            onClick = onAddClick,
            label = { Text(stringResource(R.string.add_medication_short_action)) } // Or "Add"
        )
        NavigationRailItem(
            icon = { Icon(painter = painterResource(id = R.drawable.rounded_home_24), contentDescription = stringResource(R.string.home_screen_title)) },
            selected = currentRoute == "home", // Example selection logic
            onClick = onHomeClick,
            label = { Text(stringResource(R.string.home_screen_title)) }
        )
        NavigationRailItem(
            icon = { Icon(painter = painterResource(id = R.drawable.ic_round_calendar_today_24), contentDescription = stringResource(R.string.calendar_screen_title)) },
            selected = currentRoute == "calendar", // Example selection logic
            onClick = onCalendarClick,
            label = { Text(stringResource(R.string.calendar_screen_title)) }
        )
        NavigationRailItem(
            icon = { Icon(painter = painterResource(id = R.drawable.rounded_person_24), contentDescription = stringResource(R.string.profile_screen_title)) },
            selected = currentRoute == "profile", // Example selection logic
            onClick = onProfileClick,
            label = { Text(stringResource(R.string.profile_screen_title)) }
        )
        NavigationRailItem(
            icon = { Icon(painter = painterResource(id = R.drawable.rounded_settings_24), contentDescription = stringResource(R.string.settings_screen_title)) },
            selected = currentRoute == "settings", // Example selection logic
            onClick = onSettingsClick,
            label = { Text(stringResource(R.string.settings_screen_title)) }
        )
    }
}
