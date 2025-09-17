package com.d4viddf.medicationreminder.ui.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.util.NumberVisualTransformation
import com.d4viddf.medicationreminder.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightGoalScreen(
    navController: NavController,
    viewModel: NutritionWeightSettingsViewModel = hiltViewModel()
) {
    val weightGoalValue by viewModel.weightGoalValue.collectAsState()
    val weightGoalType by viewModel.weightGoalType.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp > 600

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.weight_goal)) },
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
                .padding(Dimensions.PaddingLarge)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GoalTypeButton(
                        text = stringResource(id = R.string.lose_weight),
                        isSelected = weightGoalType == "lose",
                        onClick = { viewModel.onWeightGoalTypeChange("lose") }
                    )
                    GoalTypeButton(
                        text = stringResource(id = R.string.maintain_weight),
                        isSelected = weightGoalType == "maintain",
                        onClick = { viewModel.onWeightGoalTypeChange("maintain") }
                    )
                    GoalTypeButton(
                        text = stringResource(id = R.string.gain_weight),
                        isSelected = weightGoalType == "gain",
                        onClick = { viewModel.onWeightGoalTypeChange("gain") }
                    )
                }
                Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    BasicTextField(
                        value = weightGoalValue,
                        onValueChange = viewModel::onWeightGoalValueChange,
                        textStyle = (if (isTablet) {
                            MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp)
                        } else {
                            MaterialTheme.typography.displayLarge
                        }).copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = NumberVisualTransformation(),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "kg",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.goal),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Button(
                onClick = {
                    viewModel.saveWeightGoal()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.set_objective))
            }
        }
    }
}

@Composable
private fun GoalTypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = !isSelected
    ) {
        Text(
            text = text,
            style = if (isSelected) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleMedium
            }
        )
    }
}
