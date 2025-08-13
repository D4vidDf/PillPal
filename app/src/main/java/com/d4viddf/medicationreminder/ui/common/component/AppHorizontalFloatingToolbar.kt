package com.d4viddf.medicationreminder.ui.common.component

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.navigation.Screen

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppHorizontalFloatingToolbar(
    onHomeClick: () -> Unit,
    onMedicationVaultClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHealthClick: () -> Unit,
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
        // The ButtonGroup has been removed. The items are now direct children
        // of the HorizontalFloatingToolbar's RowScope.

        // Home Button
        val homeSelected = currentRoute == Screen.Home.route
        FilledIconToggleButton(
            checked = homeSelected,
            onCheckedChange = { if (!homeSelected) onHomeClick() }
        ) {
            Icon(
                painter = painterResource(id = if (homeSelected) R.drawable.ic_home_filled else R.drawable.rounded_home_24),
                contentDescription = stringResource(id = R.string.home_screen_title)
            )
        }

        // Medication Vault Button
        val vaultSelected = currentRoute == Screen.MedicationVault.route
        FilledIconToggleButton(
            checked = vaultSelected,
            onCheckedChange = { if (!vaultSelected) onMedicationVaultClick() }
        ) {
            Icon(
                painter = painterResource(id = if (vaultSelected) R.drawable.medication_filled else R.drawable.rounded_medication_24),
                contentDescription = stringResource(id = R.string.medication_vault_title)
            )
        }

        // Calendar Button
        val calendarSelected = currentRoute == Screen.Calendar.route
        FilledIconToggleButton(
            checked = calendarSelected,
            onCheckedChange = { if (!calendarSelected) onCalendarClick() }
        ) {
            Icon(
                painter = painterResource(id = if (calendarSelected) R.drawable.ic_calendar_filled else R.drawable.ic_calendar),
                contentDescription = stringResource(id = R.string.calendar_screen_title)
            )
        }

        // Health Button
        val healthSelected = currentRoute == Screen.Health.route
        FilledIconToggleButton(
            checked = healthSelected,
            onCheckedChange = { if (!healthSelected) onHealthClick() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_analytics),
                contentDescription = stringResource(id = R.string.health_screen_title)
            )
        }

//        // Analysis Button
//        val analysisSelected = currentRoute == Screen.Analysis.route
//        FilledIconToggleButton(
//            checked = analysisSelected,
//            onCheckedChange = { if (!analysisSelected) onAnalysisClick() }
//        ) {
//            Icon(
//                painter = painterResource(id = if (analysisSelected) R.drawable.health_and_safety_24px_filled else R.drawable.health_and_safety_24px),
//                contentDescription = stringResource(id = R.string.analysis_screen_title)
//            )
//        }

        // Profile Button
        val profileSelected = currentRoute == Screen.Profile.route
        FilledIconToggleButton(
            checked = profileSelected,
            onCheckedChange = { if (!profileSelected) onProfileClick() }
        ) {
            Icon(
                painter = painterResource(id = if (profileSelected) R.drawable.ic_person_filled else R.drawable.rounded_person_24),
                contentDescription = stringResource(id = R.string.profile_screen_title)
            )
        }
    }
}


// Preview remains the same.
@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun AppHorizontalFloatingToolbarPreview() {
    AppTheme(dynamicColor = false) {
        AppHorizontalFloatingToolbar(
            onHomeClick = {},
            onMedicationVaultClick = {},
            onCalendarClick = {},
            onAnalysisClick = {},
            onProfileClick = {},
            onHealthClick = {},
            onAddClick = {},
            currentRoute = Screen.Home.route
        )
    }
}