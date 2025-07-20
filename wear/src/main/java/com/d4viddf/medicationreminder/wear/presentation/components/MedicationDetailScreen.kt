package com.d4viddf.medicationreminder.wear.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.d4viddf.medicationreminder.wear.R
import com.d4viddf.medicationreminder.wear.persistence.MedicationWithSchedulesPojo
import com.d4viddf.medicationreminder.wear.presentation.WearViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun MedicationDetailScreen(
    medicationId: Int?,
    viewModel: WearViewModel,
    onOpenOnPhone: () -> Unit
) {
    // Collect state from the ViewModel
    val medicationWithSchedules by viewModel.selectedMedication.collectAsStateWithLifecycle()
    val progressDetails by viewModel.progressDetails.collectAsStateWithLifecycle()
    var nextDoseTime by remember { mutableStateOf<String?>(null) }

    // Effect to calculate the next dose time when medication data changes
    LaunchedEffect(medicationWithSchedules) {
        medicationWithSchedules?.let {
            nextDoseTime = calculateNextDoseTime(it)
        }
    }

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        timeText = null, // You can add a TimeText here if needed
        edgeButton = {
            EdgeButton(
                onClick = onOpenOnPhone,
                buttonSize = EdgeButtonSize.Small
            ) {
                Text(stringResource(R.string.open_on_phone), textAlign = TextAlign.Center)
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (medicationWithSchedules == null) {
                // Show a loading indicator while data is being fetched
                item { CircularProgressIndicator() }
            } else {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(
                            text = medicationWithSchedules!!.medication.name.split(" ").first(),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                item {
                    Text(
                        text = nextDoseTime?.let { stringResource(R.string.next_dose, it) } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    Text(
                        text = medicationWithSchedules!!.medication.dosage ?: "-",
                        style = MaterialTheme.typography.numeralSmall
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.dose_quantity),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    // Use the lastTaken value from progressDetails
                    Text(
                        text = progressDetails.lastTaken?.let {
                            LocalDateTime.parse(it).format(DateTimeFormatter.ofPattern("HH:mm"))
                        } ?: "N/A",
                        style = MaterialTheme.typography.numeralSmall
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.last_dose_taken),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    // Use the remaining value from progressDetails
                    Text(
                        text = progressDetails.remaining?.toString() ?: "N/A",
                        style = MaterialTheme.typography.numeralSmall
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.remaining_doses),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

private fun calculateNextDoseTime(medicationWithSchedules: MedicationWithSchedulesPojo): String? {
    val now = LocalTime.now()
    var nextDose: LocalTime? = null

    medicationWithSchedules.schedules.forEach { schedule ->
        val specificTimes: List<LocalTime>? = schedule.specificTimesJson?.let { json ->
            val typeToken = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(json, typeToken).mapNotNull {
                try {
                    LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: Exception) {
                    null
                }
            }
        }

        specificTimes?.forEach { time ->
            if (time.isAfter(now)) {
                if (nextDose == null || time.isBefore(nextDose)) {
                    nextDose = time
                }
            }
        }
    }

    return nextDose?.format(DateTimeFormatter.ofPattern("HH:mm"))
}