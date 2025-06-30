package com.d4viddf.medicationreminder.ui.common.components

// import androidx.compose.material.icons.filled.Add // Removed
import android.content.res.Configuration
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.navigation.Screen

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppHorizontalFloatingToolbar(
    onHomeClick: () -> Unit,
    onMedicationVaultClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    onProfileClick: () -> Unit,
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
                    painter = painterResource(id = R.drawable.rounded_add_24),
                    contentDescription = stringResource(id = R.string.add_medication_title)
                )
            }
        }
    ) {
        val homeSelected = currentRoute == Screen.Home.route
        IconButton(onClick = { if (!homeSelected) onHomeClick() }) {
            Icon(
                painter = painterResource(id = if (homeSelected) R.drawable.ic_home_filled else R.drawable.rounded_home_24),
                contentDescription = stringResource(id = R.string.home_screen_title),
                tint = if (homeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val vaultSelected = currentRoute == Screen.MedicationVault.route
        IconButton(onClick = { if (!vaultSelected) onMedicationVaultClick() }) {
            Icon(
                // Using a placeholder inventory icon, replace with actual if available
                painter = painterResource(id = if (vaultSelected) R.drawable.ic_inventory_filled else R.drawable.ic_inventory_outline),
                contentDescription = stringResource(id = R.string.medication_vault_title), // Add this string resource
                tint = if (vaultSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val calendarSelected = currentRoute == Screen.Calendar.route
        IconButton(onClick = { if (!calendarSelected) onCalendarClick() }) {
            Icon(
                painter = painterResource(id = if (calendarSelected) R.drawable.ic_calendar_filled else R.drawable.ic_calendar),
                contentDescription = stringResource(id = R.string.calendar_screen_title),
                tint = if (calendarSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val analysisSelected = currentRoute == Screen.Analysis.route
        IconButton(onClick = { if(!analysisSelected) onAnalysisClick() }) {
            Icon(
                painter = painterResource(id = if (analysisSelected) R.drawable.ic_analytics_filled else R.drawable.ic_analytics_outline),
                contentDescription = stringResource(id = R.string.analysis_screen_title), // Add this string resource
                tint = if (analysisSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val profileSelected = currentRoute == Screen.Profile.route
        IconButton(onClick = { if (!profileSelected) onProfileClick() }) {
            Icon(
                painter = painterResource(id = if (profileSelected) R.drawable.ic_person_filled else R.drawable.rounded_person_24),
                contentDescription = stringResource(id = R.string.profile_screen_title),
                tint = if (profileSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun AppHorizontalFloatingToolbarPreview() {
    AppTheme(dynamicColor = false) {
        // Add R.string.medication_vault_title and R.string.analysis_screen_title to strings.xml
        // Add ic_inventory_filled, ic_inventory_outline, ic_analytics_filled, ic_analytics_outline to drawables
        AppHorizontalFloatingToolbar(
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
