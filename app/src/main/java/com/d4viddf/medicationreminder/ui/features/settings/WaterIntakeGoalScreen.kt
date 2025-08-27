package com.d4viddf.medicationreminder.ui.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.util.NumberVisualTransformation
import com.d4viddf.medicationreminder.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterIntakeGoalScreen(
    navController: NavController,
    viewModel: NutritionWeightSettingsViewModel = hiltViewModel()
) {
    val waterIntakeGoal by viewModel.waterIntakeGoal.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.water)) },
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
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimensions.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BasicTextField(
                value = waterIntakeGoal,
                onValueChange = viewModel::onWaterIntakeGoalChange,
                textStyle = MaterialTheme.typography.displayLarge.copy(
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = NumberVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(id = R.string.ml_at_day),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))

            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val buttonSize = 64.dp
                ToggleButton(
                    checked = false,
                    onCheckedChange = {
                        val currentGoal = waterIntakeGoal.toIntOrNull() ?: 0
                        viewModel.onWaterIntakeGoalChange((currentGoal - 100).toString())
                    },
                    modifier = Modifier.size(buttonSize),
                    shape = ButtonDefaults.outlinedShape
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrement")
                }
                ToggleButton(
                    checked = false,
                    onCheckedChange = {
                        val currentGoal = waterIntakeGoal.toIntOrNull() ?: 0
                        viewModel.onWaterIntakeGoalChange((currentGoal + 100).toString())
                    },
                    modifier = Modifier.size(buttonSize),
                    shape = ButtonDefaults.outlinedShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increment")
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    viewModel.saveWaterIntakeGoal()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.set_objective))
            }
        }
    }
}
