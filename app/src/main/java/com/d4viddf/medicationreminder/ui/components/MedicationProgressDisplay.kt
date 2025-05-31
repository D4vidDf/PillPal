package com.d4viddf.medicationreminder.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
// import androidx.compose.material3.WavyProgressIndicatorDefaults // No es estrictamente necesario si defines todo manualmente
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke 
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.medicationreminder.ui.colors.MedicationColor

// Data class ProgressDetails (sin cambios)
data class ProgressDetails(
    val taken: Int,
    val remaining: Int,
    val totalFromPackage: Int,
    val progressFraction: Float,
    val displayText: String
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MedicationProgressDisplay(
    progressDetails: ProgressDetails?,
    colorScheme: MedicationColor,
    modifier: Modifier = Modifier,
    indicatorSizeDp: Dp = 220.dp // Add new parameter for size
) {
    var startAnimation by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnimation && progressDetails != null) progressDetails.progressFraction else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 200),
        label = "ProgressAnimation"
    )

    LaunchedEffect(progressDetails) {
        startAnimation = false
        startAnimation = true
    }

    val density = LocalDensity.current
    val desiredStrokeWidth = 8.dp
    val desiredStrokeWidthPx = with(density) { desiredStrokeWidth.toPx() }
    val indicatorSize = indicatorSizeDp // Use the parameter value

    // Construct the semantic description for the progress indicator here
    val progressIndicatorSemanticDesc: String = if (progressDetails != null) {
        if (progressDetails.totalFromPackage > 0) {
            progressDetails.displayText + ". " + stringResource(
                id = com.d4viddf.medicationreminder.R.string.medication_progress_display_acc,
                progressDetails.taken,
                progressDetails.totalFromPackage,
                progressDetails.remaining
            )
        } else {
            progressDetails.displayText
        }
    } else {
        stringResource(id = com.d4viddf.medicationreminder.R.string.medication_progress_display_info_not_available_acc)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = indicatorSize + 16.dp)
            .padding(vertical = 8.dp),
            // Removed: .semantics { contentDescription = accessibilityDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(indicatorSize)
        ) {
            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        if (progressDetails != null && progressDetails.totalFromPackage > 0) {
                            progressBarRangeInfo = ProgressBarRangeInfo(
                                current = animatedProgress * progressDetails.totalFromPackage, // Current value of progress
                                range = 0f..(progressDetails.totalFromPackage.toFloat()), // Total range
                                steps = progressDetails.totalFromPackage // Number of discrete steps, if applicable
                            )
                        }
                        // Assign the pre-constructed string to contentDescription
                        this.contentDescription = progressIndicatorSemanticDesc
                    },
                color = colorScheme.progressBarColor,
                trackColor = colorScheme.progressBackColor,
                stroke = Stroke(width = desiredStrokeWidthPx,cap = StrokeCap.Round),
                trackStroke = Stroke(width = desiredStrokeWidthPx,cap = StrokeCap.Round),
                wavelength = 42.dp,
                waveSpeed = 3.dp,
                gapSize = 12.dp
            )

            // Column para agrupar "Progreso" y los números, y centrar esta Column dentro del Box
            if (progressDetails != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_progress_display_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.textColor,
                        modifier = Modifier.semantics { heading() } // Mark as heading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progressDetails.displayText,
                        fontSize = if (progressDetails.displayText == stringResource(id = com.d4viddf.medicationreminder.R.string.info_not_available_short)) 48.sp else 32.sp, // Compare with resource
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.textColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationProgressDisplayPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        MedicationProgressDisplay(
            progressDetails = ProgressDetails(
                taken = 5,
                remaining = 5,
                totalFromPackage = 10,
                progressFraction = 0.5f,
                displayText = "5 / 10"
            ),
            colorScheme = com.d4viddf.medicationreminder.ui.colors.MedicationColor.GREEN,
            indicatorSizeDp = 200.dp
        )
    }
}