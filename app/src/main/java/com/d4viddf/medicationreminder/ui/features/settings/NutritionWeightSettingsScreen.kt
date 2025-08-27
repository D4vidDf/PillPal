package com.d4viddf.medicationreminder.ui.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.component.ConfirmationDialog
import com.d4viddf.medicationreminder.ui.navigation.Screen
import com.d4viddf.medicationreminder.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionWeightSettingsScreen(
    navController: NavController
) {
    val showNutritionDataDialog = remember { mutableStateOf(false) }
    val showWeightDataDialog = remember { mutableStateOf(false) }

    if (showNutritionDataDialog.value) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_nutrition_data),
            text = stringResource(R.string.delete_nutrition_data_confirmation),
            onConfirm = {
                // TODO: Add logic to delete nutrition data
                showNutritionDataDialog.value = false
            },
            onDismiss = { showNutritionDataDialog.value = false }
        )
    }

    if (showWeightDataDialog.value) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_weight_data),
            text = stringResource(R.string.delete_weight_data_confirmation),
            onConfirm = {
                // TODO: Add logic to delete weight data
                showWeightDataDialog.value = false
            },
            onDismiss = { showWeightDataDialog.value = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.nutrition_weight_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_arrow_back_ios_24),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(Dimensions.PaddingLarge)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.goals),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.PaddingMedium)
            ) {
                GoalCard(
                    title = stringResource(R.string.weight_goal),
                    goal = "75 kg", // TODO: Replace with actual weight goal
                    onClick = { /* navController.navigate(Screen.WeightGoal.route) */ },
                    modifier = Modifier.weight(1f)
                )
                GoalCard(
                    title = stringResource(R.string.water_goal),
                    goal = "2000 ml", // TODO: Replace with actual water goal
                    onClick = { navController.navigate(Screen.WaterIntakeGoal.route) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.PaddingExtraLarge))

            Text(
                text = stringResource(id = R.string.connection),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
            Card(
                modifier = Modifier.clickable {
                    // TODO: Navigate to Health Connect settings
                }
            ) {
                Text(
                    text = stringResource(id = R.string.health_connect_settings),
                    modifier = Modifier
                        .padding(Dimensions.PaddingLarge)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.PaddingExtraLarge))

            Text(
                text = stringResource(id = R.string.data),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
            Card(
                modifier = Modifier.clickable {
                    showNutritionDataDialog.value = true
                }
            ) {
                Text(
                    text = stringResource(id = R.string.delete_nutrition_data),
                    modifier = Modifier
                        .padding(Dimensions.PaddingLarge)
                        .fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
            Card(
                modifier = Modifier.clickable {
                    showWeightDataDialog.value = true
                }
            ) {
                Text(
                    text = stringResource(id = R.string.delete_weight_data),
                    modifier = Modifier
                        .padding(Dimensions.PaddingLarge)
                        .fillMaxWidth()
                )
            }
        }
    }
}


@Composable
fun GoalCard(
    title: String,
    goal: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .padding(Dimensions.PaddingLarge)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = goal, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
