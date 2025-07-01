@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.d4viddf.medicationreminder.ui.features.medicationvault.components // Updated package

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor

@Composable
fun MedicationCard(
    medication: Medication,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    enableTransition: Boolean
) {
    val color = try {
        MedicationColor.valueOf(medication.color)
    } catch (e: IllegalArgumentException) {
        Log.w("MedicationCard", "Invalid color string: '${medication.color}' for medication '${medication.name}'. Defaulting.", e)
        MedicationColor.LIGHT_ORANGE
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
            .then(
                if (sharedTransitionScope != null && animatedVisibilityScope != null && enableTransition) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            rememberSharedContentState(key = "medication-background-${medication.id}"),
                            animatedVisibilityScope!!
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(2f)
            ) {
                val displayName = medication.name.split(" ").take(3).joinToString(" ")
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color= color.textColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                )
                Text(
                    text = "${medication.dosage}",
                    style = MaterialTheme.typography.bodyLarge,
                    color= color.textColor
                )
                if (medication.reminderTime != null) {
                    Text(
                        text = "Time: ${medication.reminderTime}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            MedicationAvatar(color = Color.White)
        }
    }
}

@Composable
fun MedicationAvatar(color: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = color, shape = CircleShape)
    )
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationCardPreview() {
    AppTheme(dynamicColor = false) {
        MedicationCard(
            medication = Medication(
                id = 1,
                name = "Amoxicillin Long Name For Testing Ellipsis",
                dosage = "250mg",
                color = "LIGHT_BLUE",
                reminderTime = "10:00 AM",
                typeId = 1,
                packageSize = 0,
                remainingDoses = 0,
                startDate = null,
                endDate = null
            ),
            onClick = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            enableTransition = true
        )
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationAvatarPreview() {
    AppTheme(dynamicColor = false) {
        MedicationAvatar(color = Color.White)
    }
}
