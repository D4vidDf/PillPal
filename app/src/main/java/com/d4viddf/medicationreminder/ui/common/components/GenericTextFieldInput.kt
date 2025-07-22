package com.d4viddf.medicationreminder.ui.common.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
// import androidx.compose.material.icons.filled.Edit // Removed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource // Added import
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericTextFieldInput(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    description: String? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.padding(16.dp)) {
        // Input Label Above the TextField
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Text Field for Input
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            trailingIcon = {
                Icon(painter = painterResource(id = R.drawable.rounded_edit_24), contentDescription = stringResource(id = R.string.generic_textfield_edit_acc))
            },
            keyboardOptions = keyboardOptions,
            isError = isError,
            shape = MaterialTheme.shapes.large // Use rounded corners, following the style from `MedicationNameInput`
        )

        // Optional Description Below the TextField
        description?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isError) {
            Text(
                text = stringResource(id = R.string.error_required_field),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun GenericTextFieldInputPreview() {
    AppTheme(dynamicColor = false) {
        GenericTextFieldInput(
            label = "Medication Name",
            value = "Amoxicillin",
            onValueChange = {},
            description = "Enter the full name of the medication.",
            isError = false
        )
    }
}
