package com.d4viddf.medicationreminder.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.viewmodel.SettingsViewModel
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import android.util.Log // Required for LaunchedEffect error logging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Collect shareRequest for the share intent
    LaunchedEffect(Unit) {
        viewModel.shareRequest.collect { intent: android.content.Intent ->
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot share logs: No app available to handle this action.", Toast.LENGTH_LONG).show()
                Log.e("DeveloperSettingsScreen", "Error starting share intent activity", e)
                // com.d4viddf.medicationreminder.utils.FileLogger.log("DeveloperSettingsScreenError", "Error starting share intent activity", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_category_developer)) }, // Use category title
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_developer_options_description), // Assume new string resource like "These options are for debugging and development."
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedButton(
                onClick = {
                    viewModel.restartDailyWorker()
                    Toast.makeText(context, "Daily worker restart initiated.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restart Daily Worker") // Consider R.string resource
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    viewModel.shareAppLogs()
                    // Toast for log sharing is handled by observing shareIntent result or errors,
                    // or can be added here if immediate feedback is desired before intent resolution.
                    // For now, rely on LaunchedEffect for feedback on actual share attempt.
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share App Logs") // Consider R.string resource
            }
        }
    }
}

// Define necessary string resources in strings.xml (conceptual)
// <string name="settings_category_developer">Developer Options</string> // Already assumed for SettingsListScreen
// <string name="settings_developer_options_description">These options are for debugging and development purposes.</string>
// <string name="settings_restart_daily_worker_button">Restart Daily Worker</string>
// <string name="settings_share_app_logs_button">Share App Logs</string>

@Preview(showBackground = true)
@Composable
fun DeveloperSettingsScreenPreview() {
    AppTheme {
        DeveloperSettingsScreen(onNavigateBack = {})
    }
}
