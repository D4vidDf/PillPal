package com.d4viddf.medicationreminder.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.screens.Screen // Required for Screen routes
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppHorizontalFloatingToolbar(
    onHomeClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentRoute: String? = null
) {
    HorizontalFloatingToolbar(
        modifier = modifier,
        expanded = true,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.add_medication_title)
                )
            }
        }
    ) {
        val homeSelected = currentRoute == Screen.Home.route
        IconButton(onClick = { if (currentRoute != Screen.Home.route) onHomeClick() }) {
            Icon(
                painter = painterResource(id = if (homeSelected) R.drawable.ic_home_filled else R.drawable.rounded_home_24),
                contentDescription = stringResource(id = R.string.home_screen_title),
                tint = if (homeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val calendarSelected = currentRoute == Screen.Calendar.route
        IconButton(onClick = { if (currentRoute != Screen.Calendar.route) onCalendarClick() }) {
            Icon(
                painter = painterResource(id = if (calendarSelected) R.drawable.ic_calendar_filled else R.drawable.ic_calendar),
                contentDescription = stringResource(id = R.string.calendar_screen_title),
                tint = if (calendarSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val profileSelected = currentRoute == Screen.Profile.route
        IconButton(onClick = { if (currentRoute != Screen.Profile.route) onProfileClick() }) {
            Icon(
                painter = painterResource(id = if (profileSelected) R.drawable.ic_person_filled else R.drawable.rounded_person_24),
                contentDescription = stringResource(id = R.string.profile_screen_title),
                tint = if (profileSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val settingsSelected = currentRoute == Screen.Settings.route
        IconButton(onClick = { if (currentRoute != Screen.Settings.route) onSettingsClick() }) {
            Icon(
                painter = painterResource(id = if (settingsSelected) R.drawable.ic_outline_settings_24 else R.drawable.ic_outline_settings_24),
                contentDescription = stringResource(id = R.string.settings_screen_title),
                tint = if (settingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun AppHorizontalFloatingToolbarPreview() {
    AppTheme {
        AppHorizontalFloatingToolbar(
            onHomeClick = {},
            onCalendarClick = {},
            onProfileClick = {},
            onSettingsClick = {},
            onAddClick = {},
            currentRoute = Screen.Home.route
        )
    }
}
