package com.d4viddf.medicationreminder.ui.features.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionWeightSettingsScreen(
    navController: NavController,
    viewModel: NutritionWeightSettingsViewModel = hiltViewModel()
) {
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
        ) {
            Text(
                text = stringResource(id = R.string.daily_water_intake_goal),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
            OutlinedTextField(
                value = viewModel.waterIntakeGoal.collectAsState().value,
                onValueChange = viewModel::onWaterIntakeGoalChange,
                label = { Text(text = stringResource(id = R.string.water_intake_goal_ml)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
            Button(
                onClick = viewModel::saveWaterIntakeGoal,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(id = R.string.save))
            }

            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

            Text(
                text = stringResource(id = R.string.weight_goal),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.lose_weight))
                Switch(
                    checked = viewModel.weightGoalType.collectAsState().value == "gain",
                    onCheckedChange = {
                        viewModel.onWeightGoalTypeChange(if (it) "gain" else "lose")
                    }
                )
                Text(text = stringResource(id = R.string.gain_weight))
            }
            Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
            OutlinedTextField(
                value = viewModel.weightGoalValue.collectAsState().value,
                onValueChange = viewModel::onWeightGoalValueChange,
                label = { Text(text = stringResource(id = R.string.weight_goal_kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
            Button(
                onClick = viewModel::saveWeightGoal,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(id = R.string.save))
            }
        }
    }
}
