@file:OptIn(ExperimentalSharedTransitionApi::class) // Moved OptIn to file-level
package com.d4viddf.medicationreminder.ui.features.medication.details.components

import android.content.res.Configuration
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor

@Composable
fun MedicationDetailHeader(
    medicationId: Int,
    medicationName: String?,
    medicationDosage: String?,
    medicationImageUrl: String?,
    colorScheme: MedicationColor,
    // sharedTransitionScope: SharedTransitionScope?, // Removed
    // animatedVisibilityScope: AnimatedVisibilityScope?, // Removed
    modifier: Modifier = Modifier
) {
    val loadingText = stringResource(id = R.string.medication_detail_header_loading)
    val noDosageText = stringResource(id = R.string.medication_detail_header_no_dosage)
    val imageAccText = stringResource(id = R.string.medication_detail_header_image_acc)

    val displayName = remember(medicationName, loadingText) {
        val words = medicationName?.split(" ")
        if (words != null && words.size > 3) {
            words.take(3).joinToString(" ")
        } else {
            medicationName ?: loadingText
        }
    }

    // Removed: val sharedTransitionScope = LocalSharedTransitionScope.current
    Row(
        modifier = modifier.fillMaxWidth(), // El modifier se aplica al Row principal
        verticalAlignment = Alignment.CenterVertically // Alineación vertical de los elementos del Row
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 30.sp, // Reverted
                fontWeight = FontWeight.Bold,
                color = colorScheme.textColor,
                lineHeight = 34.sp, // Reverted
                maxLines = 2, // Reverted
                overflow = TextOverflow.Ellipsis, // Ensured
                modifier = Modifier // sharedElement modifier removed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = medicationDosage?.takeIf { it.isNotBlank() } ?: noDosageText,
                fontSize = 20.sp,
                color = colorScheme.textColor
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = rememberAsyncImagePainter(model = medicationImageUrl ?: "https://placehold.co/100x100.png"),
            contentDescription = imageAccText,
            modifier = Modifier.size(64.dp)
        )
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationDetailHeaderPreview() {
    AppTheme(dynamicColor = false) {
        MedicationDetailHeader(
            medicationId = 0, // Dummy ID for preview
            medicationName = "Amoxicillin Trihydrate Suspension",
            medicationDosage = "250mg / 5ml",
            medicationImageUrl = null, // Or a sample image URL
            colorScheme = MedicationColor.LIGHT_BLUE
            // sharedTransitionScope = null, // Removed
            // animatedVisibilityScope = null // Removed
        )
    }
}