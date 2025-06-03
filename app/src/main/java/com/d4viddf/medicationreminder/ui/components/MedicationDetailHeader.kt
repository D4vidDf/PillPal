package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.LocalSharedTransitionScope
import androidx.compose.animation.rememberSharedContentState
import androidx.compose.animation.sharedElement

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MedicationDetailHeader(
    medicationId: Int, // Add this
    medicationName: String?,
    medicationDosage: String?,
    medicationImageUrl: String?,
    colorScheme: MedicationColor,
    animatedVisibilityScope: AnimatedVisibilityScope, // Add this
    modifier: Modifier = Modifier
) {
    val loadingText = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_header_loading)
    val noDosageText = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_header_no_dosage)
    val imageAccText = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_header_image_acc)

    val displayName = remember(medicationName, loadingText) {
        val words = medicationName?.split(" ")
        if (words != null && words.size > 3) {
            words.take(3).joinToString(" ")
        } else {
            medicationName ?: loadingText
        }
    }

    val sharedTransitionScope = LocalSharedTransitionScope.current
    Row(
        modifier = modifier.fillMaxWidth(), // El modifier se aplica al Row principal
        verticalAlignment = Alignment.CenterVertically // Alineación vertical de los elementos del Row
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 30.sp, // Changed from 36.sp
                fontWeight = FontWeight.Bold,
                color = colorScheme.textColor,
                lineHeight = 34.sp, // Adjusted lineHeight
                maxLines = 2, // Permitir hasta 2 líneas para el nombre
                overflow = TextOverflow.Ellipsis, // Añadir elipsis si el texto es muy largo
                modifier = Modifier.then(
                    if (sharedTransitionScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                state = rememberSharedContentState(key = "medication-name-${medicationId}"), // Use medicationId
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else Modifier
                )
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

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationDetailHeaderPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        MedicationDetailHeader(
            medicationId = 0, // Dummy ID for preview
            medicationName = "Amoxicillin Trihydrate Suspension",
            medicationDosage = "250mg / 5ml",
            medicationImageUrl = null, // Or a sample image URL
            colorScheme = com.d4viddf.medicationreminder.ui.colors.MedicationColor.LIGHT_BLUE,
            animatedVisibilityScope = null // Preview won't have a real scope
        )
    }
}