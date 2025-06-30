package com.d4viddf.medicationreminder.ui.common.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
// import androidx.compose.material.icons.filled.Add // Removed
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.navigation.Screen

@Composable
fun AppNavigationRail(
    onHomeClick: () -> Unit,
    onMedicationVaultClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentRoute: String? = null
) {
    NavigationRail(
        modifier = modifier,
        header = {
            // Box to provide padding and alignment for the FAB if needed.
            // NavigationRail's header slot might already center, but explicit control is safer.
            Box(
                modifier = Modifier.padding(vertical = 20.dp), // Adjust padding as needed
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onAddClick,
                    // containerColor and contentColor can be customized if defaults are not suitable
                    // e.g., containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    // contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_add_24),
                        contentDescription = stringResource(R.string.add_medication_title)
                    )
                }
            }
        }
    ) {
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
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        val vaultSelected = currentRoute == Screen.MedicationVault.route
        NavigationRailItem(
            icon = {
                Icon(
                    painter = painterResource(id = if (vaultSelected) R.drawable.ic_inventory_filled else R.drawable.ic_inventory_outline),
                    contentDescription = stringResource(R.string.medication_vault_title) // Add R.string.medication_vault_title
                )
            },
            selected = vaultSelected,
            onClick = onMedicationVaultClick,
            label = { Text(stringResource(R.string.medication_vault_title)) }, // Add R.string.medication_vault_title
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
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

        val analysisSelected = currentRoute == Screen.Analysis.route
        NavigationRailItem(
            icon = {
                Icon(
                    painter = painterResource(id = if (analysisSelected) R.drawable.ic_analytics_filled else R.drawable.ic_analytics_outline),
                    contentDescription = stringResource(R.string.analysis_screen_title) // Add R.string.analysis_screen_title
                )
            },
            selected = analysisSelected,
            onClick = onAnalysisClick,
            label = { Text(stringResource(R.string.analysis_screen_title)) }, // Add R.string.analysis_screen_title
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
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun AppNavigationRailPreview() {
    AppTheme(dynamicColor = false) {
        // Add R.string.medication_vault_title and R.string.analysis_screen_title to strings.xml
        // Add ic_inventory_filled, ic_inventory_outline, ic_analytics_filled, ic_analytics_outline to drawables
        AppNavigationRail(
            onHomeClick = {},
            onMedicationVaultClick = {},
            onCalendarClick = {},
            onAnalysisClick = {},
            onProfileClick = {},
            onAddClick = {},
            currentRoute = Screen.Home.route
        )
    }
}
