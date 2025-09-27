package com.d4viddf.medicationreminder.ui.features.medication.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationChoiceScreen(
    onSearchMedication: () -> Unit,
    onUseCamera: () -> Unit, // Will be disabled for now
    onClose: () -> Unit // Add this parameter
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.add_medication_choice_title)) },
                actions = { // Add actions parameter
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.close) // Assuming R.string.close exists or add it
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onUseCamera,
                enabled = false, // Disabled as per requirement
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(56.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera), // TODO: Add this drawable
                    contentDescription = stringResource(id = R.string.add_medication_camera_button_desc), // TODO: Add this string resource
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(id = R.string.add_medication_camera_button)) // TODO: Add this string resource
            }

            Button(
                onClick = onSearchMedication,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search), // TODO: Add this drawable
                    contentDescription = stringResource(id = R.string.add_medication_search_button_desc), // TODO: Add this string resource
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(id = R.string.add_medication_search_button)) // TODO: Add this string resource
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddMedicationChoiceScreenPreview() {
    AppTheme {
        AddMedicationChoiceScreen(
            onSearchMedication = {},
            onUseCamera = {},
            onClose = {} // Add this
        )
    }
}
