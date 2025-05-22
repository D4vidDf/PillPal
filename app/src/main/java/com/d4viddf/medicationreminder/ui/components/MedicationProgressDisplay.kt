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
import androidx.compose.ui.graphics.drawscope.Stroke // Para definir el grosor del trazo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    modifier: Modifier = Modifier
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
    val desiredStrokeWidth = 12.dp
    val desiredStrokeWidthPx = with(density) { desiredStrokeWidth.toPx() }
    val indicatorSize = 220.dp

    val accessibilityDescription = if (progressDetails != null && progressDetails.totalFromPackage > 0) {
        "Progreso de la medicación: ${progressDetails.taken} dosis tomadas de ${progressDetails.totalFromPackage}. Quedan ${progressDetails.remaining} dosis."
    } else if (progressDetails != null && progressDetails.displayText != "N/A") {
        "Medicación: ${progressDetails.remaining} restantes, ${progressDetails.taken} tomadas. Progreso no basado en paquete."
    } else {
        "Progreso de la medicación: Información no disponible."
    }

    // El Column exterior ahora solo envuelve el Box que contiene el indicador y el texto
    // Se eliminó el Text("Progreso") que estaba fuera del Box.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = indicatorSize + 16.dp) // Altura mínima basada en el tamaño del indicador + padding
            .padding(vertical = 8.dp)
            .semantics { contentDescription = accessibilityDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center, // Centra el contenido del Box (la Column con los textos)
            modifier = Modifier.size(indicatorSize)
        ) {
            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.progressBarColor,
                trackColor = colorScheme.progressBackColor,
                stroke = Stroke(width = desiredStrokeWidthPx),
                trackStroke = Stroke(width = desiredStrokeWidthPx),
                wavelength = 52.dp,
                waveSpeed = 4.dp,
                gapSize = 0.dp
            )

            // Column para agrupar "Progreso" y los números, y centrar esta Column dentro del Box
            if (progressDetails != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center // Centra los textos verticalmente dentro de esta Column
                ) {
                    Text(
                        text = "Progreso",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.textColor,
                        // No necesita padding inferior aquí si el Spacer lo maneja
                    )
                    Spacer(modifier = Modifier.height(8.dp)) // Espacio entre "Progreso" y los números
                    Text(
                        text = progressDetails.displayText,
                        fontSize = if (progressDetails.displayText == "N/A") 48.sp else 32.sp,
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