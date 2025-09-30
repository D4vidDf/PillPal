package com.d4viddf.medicationreminder.ui.features.notifications.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.Notification
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import java.time.format.DateTimeFormatter
import java.util.Locale

import java.time.format.FormatStyle

@Composable
fun NotificationItem(notification: Notification) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.ui.graphics.RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NotificationIcon(notification = notification)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium
                )
                val formatter = remember {
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                }
                Text(
                    text = notification.timestamp.format(formatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotificationIcon(notification: Notification) {
    val iconSize = 56.dp // Increased size for better visibility
    val squircleShape = RoundedCornerShape(16.dp)

    when (notification.type) {
        "security_alert" -> {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(squircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = stringResource(id = R.string.notification_icon_cd_security),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(iconSize * 0.6f)
                )
            }
        }
        "low_medication" -> {
            val medicationColor = remember(notification.color) {
                try {
                    notification.color?.let { MedicationColor.valueOf(it) } ?: MedicationColor.BLUE
                } catch (e: IllegalArgumentException) {
                    MedicationColor.BLUE
                }
            }

            val imageResId = remember(notification.icon) {
                when (notification.icon.lowercase(Locale.getDefault())) {
                    "pill" -> R.drawable.ic_med_pill
                    "capsule" -> R.drawable.ic_med_capsule
                    "syrup", "liquid" -> R.drawable.ic_med_syrup
                    "injection", "syringe" -> R.drawable.ic_med_injection
                    "drops" -> R.drawable.ic_med_drops
                    "inhaler" -> R.drawable.ic_med_inhaler
                    else -> R.drawable.ic_stat_medication
                }
            }

            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(squircleShape)
                    .background(medicationColor.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = imageResId),
                    contentDescription = stringResource(id = R.string.notification_icon_cd_medication),
                    tint = medicationColor.textColor,
                    modifier = Modifier.size(iconSize * 0.6f)
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(squircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(id = R.string.notification_icon_cd_general),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(iconSize * 0.6f)
                )
            }
        }
    }
}