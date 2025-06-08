package com.d4viddf.medicationreminder.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton // Needed for Preview's TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold // Needed for Preview
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar // Needed for Preview's Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.viewmodel.SettingsViewModel
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxSize // Moved to main import block

// import androidx.compose.foundation.layout.Box // No longer needed for empty topBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSettingsScreen(
    onNavigateBack: () -> Unit, // Still passed by ResponsiveSettingsScaffold
    viewModel: SettingsViewModel = hiltViewModel()
    // showTopAppBar: Boolean // Parameter removed
) {
    val context = LocalContext.current
    val currentVolume by viewModel.currentVolume.collectAsState()
    val maxVolume by viewModel.maxVolume.collectAsState()
    val currentNotificationSoundUri by viewModel.currentNotificationSoundUri.collectAsState()
    val notificationSoundName = viewModel.getNotificationSoundName(currentNotificationSoundUri)

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.updateNotificationSoundUri(uri?.toString())
        }
    }

    // Scaffold removed, root is now Column
    Column(
        modifier = Modifier
            .fillMaxSize() // Takes full size given by parent
            .padding(16.dp) // General content padding
    ) {
        // Volume Control Group
        Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_volume_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (currentVolume > 0) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = stringResource(id = if (currentVolume > 0) R.string.volume_up_content_description else R.string.volume_off_content_description)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = currentVolume.toFloat(),
                            onValueChange = { newVolume ->
                                viewModel.setVolume(newVolume.roundToInt())
                            },
                            valueRange = 0f..(if (maxVolume > 0) maxVolume.toFloat() else 1f),
                            steps = if (maxVolume > 1) maxVolume - 1 else 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notification Sound Setting Group
            // This reuses the NotificationSoundSettingItem composable, which should be made public
            // or moved to a common place if it was private in the original SettingsScreen.
            // For this subtask, we'll assume it's available or define it if it was private.
            // Let's define it here for now if it's small and was private.
            // If NotificationSoundSettingItem was already public or in a shared file, this redefinition is not needed.
            // Based on original SettingsScreen.kt, it was a private composable.
            NotificationSoundSettingItemInternal(
                soundName = notificationSoundName,
                onClick = {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        )
                        currentNotificationSoundUri?.let {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                        }
                    }
                    ringtonePickerLauncher.launch(intent)
                }
            )
        }
    }
}

// Copied from original SettingsScreen.kt, assuming it was private.
// If it becomes shared, it should be moved to a common components file.
@Composable
private fun NotificationSoundSettingItemInternal( // Renamed to avoid conflict if original becomes public
    soundName: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.settings_notification_sound_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = soundName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class) // Added OptIn for Preview
@Preview(showBackground = true, name = "Sound Settings Screen Preview")
@Composable
fun SoundSettingsScreenPreview() {
    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Sound Settings") }, // Preview-specific Title
                    navigationIcon = {
                        IconButton(onClick = {}) { // Dummy action for preview
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back" // Preview description
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
                SoundSettingsScreen(onNavigateBack = {}, viewModel = hiltViewModel())
            }
        }
    }
}
