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
import com.d4viddf.medicationreminder.ui.screens.Screen // Required for Screen routes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItemDefaults

@Composable
fun AppNavigationRail(
    onHomeClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentRoute: String? = null
) {
    NavigationRail(
        modifier = modifier,
        // Optional: Apply custom container color if needed, e.g., MaterialTheme.colorScheme.surfaceVariant
    ) {
        // "Add" item - typically no selected state
        NavigationRailItem(
            icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.add_medication_title)) },
            selected = false,
            onClick = onAddClick,
            label = { Text(stringResource(R.string.add_medication_short_action)) },
            colors = NavigationRailItemDefaults.colors( // Ensure consistent unselected color if needed
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        val homeSelected = currentRoute == Screen.Home.route
        NavigationRailItem(
            icon = {
                Icon(
                    painter = painterResource(id = if (homeSelected) R.drawable.ic_home_filled else R.drawable.rounded_home_24),
                    contentDescription = stringResource(R.string.home_screen_title)
                )
            },
            selected = homeSelected,
            onClick = onHomeClick,
            label = { Text(stringResource(R.string.home_screen_title)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer // Or adjust as needed
            )
        )

        val calendarSelected = currentRoute == Screen.Calendar.route
        NavigationRailItem(
            icon = {
                Icon(
                    painter = painterResource(id = if (calendarSelected) R.drawable.ic_calendar_filled else R.drawable.ic_calendar),
                    contentDescription = stringResource(R.string.calendar_screen_title)
                )
            },
            selected = calendarSelected,
            onClick = onCalendarClick,
            label = { Text(stringResource(R.string.calendar_screen_title)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        val profileSelected = currentRoute == Screen.Profile.route
        NavigationRailItem(
            icon = {
                Icon(
                    painter = painterResource(id = if (profileSelected) R.drawable.ic_person_filled else R.drawable.rounded_person_24),
                    contentDescription = stringResource(R.string.profile_screen_title)
                )
            },
            selected = profileSelected,
            onClick = onProfileClick,
            label = { Text(stringResource(R.string.profile_screen_title)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        val settingsSelected = currentRoute == Screen.Settings.route
        NavigationRailItem(
            icon = {
                Icon(
                    painter = painterResource(id = if (settingsSelected) R.drawable.ic_outline_settings_24 else R.drawable.ic_outline_settings_24),
                    contentDescription = stringResource(R.string.settings_screen_title)
                )
            },
            selected = settingsSelected,
            onClick = onSettingsClick,
            label = { Text(stringResource(R.string.settings_screen_title)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
