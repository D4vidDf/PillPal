package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider // Importar VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.ScheduleType
import com.d4viddf.medicationreminder.logic.ReminderCalculator
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// CounterItem restaurado a la versión más simple que coincide con tu diseño original
@Composable
fun CounterItem(value: String, label: String, valueColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp) // Padding para que no estén pegados al divisor
    ) {
        Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = valueColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
fun MedicationDetailCounters(
    colorScheme: MedicationColor,
    medication: Medication?,
    schedule: MedicationSchedule?,
    modifier: Modifier = Modifier // Este modifier se aplicará al Row principal de contadores
) {
    // --- Lógica de cálculo de datos para los contadores ---
    val dosePair: Pair<String?, String?> = remember(medication) {
        val fullDosage = medication?.dosage?.trim()
        if (fullDosage.isNullOrBlank()) { null to null }
        else {
            val parts = fullDosage.split(" ", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else fullDosage to ""
        }
    }
    val doseValue = dosePair.first
    val doseUnit = dosePair.second

    val frequencyPair: Pair<String?, String?> = remember(schedule) {
        schedule?.let { sched ->
            if (!sched.daysOfWeek.isNullOrBlank() &&
                (sched.scheduleType == ScheduleType.DAILY || sched.scheduleType == ScheduleType.WEEKLY) &&
                (sched.specificTimes?.split(',')?.count { it.isNotBlank() } ?: 0) <= 1 &&
                sched.specificTimes?.split(',')?.firstOrNull()?.isNotBlank() == true) {
                val timesCount = sched.specificTimes?.split(',')?.count { it.isNotBlank() } ?: 0
                if (timesCount == 1) "1" to "toma" // Más genérico que "vez al día" si hay días
                else if (timesCount > 1) "$timesCount" to "tomas"
                else null
            } else {
                when (sched.scheduleType) {
                    ScheduleType.DAILY -> {
                        val timesCount = sched.specificTimes?.split(',')?.count { it.isNotBlank() } ?: 0
                        if (timesCount == 1) "1" to "vez al día"
                        else if (timesCount > 1) "$timesCount" to "veces al día"
                        else null
                    }
                    ScheduleType.CUSTOM_ALARMS -> {
                        val timesCount = sched.specificTimes?.split(',')?.count { it.isNotBlank() } ?: 0
                        if (timesCount > 0) "$timesCount" to "veces al día" else null
                    }
                    ScheduleType.INTERVAL -> {
                        val h = sched.intervalHours ?: 0; val m = sched.intervalMinutes ?: 0
                        val vp = mutableListOf<String>()
                        if (h > 0) vp.add("${h}h"); if (m > 0) vp.add("${m}m")
                        if (vp.isNotEmpty()) "Cada ${vp.joinToString(" ")}" to "intervalo" else null
                    }
                    ScheduleType.AS_NEEDED -> "S/N" to "según necesidad"
                    else -> null
                }
            }
        }?.let { if (it.first == null && it.second == null) null else it }
            ?: (null to null)
    }
    val frequencyValue = frequencyPair.first
    val frequencyUnit = frequencyPair.second

    val durationPair: Pair<String?, String?> = remember(medication) {
        medication?.let { med ->
            val today = LocalDate.now()
            var pStartDate: LocalDate? = null
            if (!med.startDate.isNullOrBlank() && med.startDate != "Select Start Date") {
                try { pStartDate = LocalDate.parse(med.startDate, ReminderCalculator.dateStorableFormatter) } catch (e: DateTimeParseException) {}
            }
            var pEndDate: LocalDate? = null
            if (!med.endDate.isNullOrBlank() && med.endDate != "Select End Date") {
                try { pEndDate = LocalDate.parse(med.endDate, ReminderCalculator.dateStorableFormatter) } catch (e: DateTimeParseException) {}
            }
            when {
                pStartDate != null && pEndDate != null -> {
                    if (pEndDate.isBefore(pStartDate)) return@remember null to null
                    val total = ChronoUnit.DAYS.between(pStartDate, pEndDate) + 1
                    if (total >= 0) "$total" to "días total" else null
                }
                pEndDate != null && !pEndDate.isBefore(today) -> {
                    val ref = if (pStartDate != null && pStartDate.isAfter(today)) pStartDate else today
                    if (pEndDate.isBefore(ref)) return@remember null to null
                    val remaining = ChronoUnit.DAYS.between(ref, pEndDate) + 1
                    if (remaining >= 0) "$remaining" to "días rest." else null
                }
                else -> null
            }
        }?.let { if (it.first == null && it.second == null) null else it }
            ?: (null to null)
    }
    val durationValue = durationPair.first
    val durationUnit = durationPair.second

    val daysOfWeekInternal: List<Int>? = remember(schedule) {
        schedule?.takeIf { (it.scheduleType == ScheduleType.DAILY || it.scheduleType == ScheduleType.WEEKLY) && !it.daysOfWeek.isNullOrBlank() }
            ?.daysOfWeek?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.sorted()
    }

    val daysSummaryPair: Pair<String?, String?> = remember(daysOfWeekInternal) {
        daysOfWeekInternal?.let { days ->
            if (days.isNotEmpty()) {
                val count = days.size
                val label = when (count) {
                    7 -> "todos los días"
                    1 -> "día" // o "1 día/sem."
                    else -> "$count días/semana"
                }
                // Para el valor, si es "todos los días", no necesitamos el "7" explícitamente
                // a menos que el diseño lo requiera. Vamos a usar el count.
                count.toString() to label
            } else {
                null to null
            }
        } ?: (null to null)
    }
    val daysSummaryValue = daysSummaryPair.first
    val daysSummaryLabel = daysSummaryPair.second
    // --- Fin de la lógica de cálculo ---

    val itemsToDisplay = mutableListOf<Pair<String, String>>()

    // Lógica de selección de hasta 3 items para mostrar
    if (doseValue != null && doseUnit != null) {
        itemsToDisplay.add(doseValue to doseUnit)
    }

    if (daysSummaryValue != null && daysSummaryLabel != null) {
        // Solo añadir si no hemos alcanzado el límite y no es redundante con una frecuencia ya simple
        if (itemsToDisplay.size < 3) {
            itemsToDisplay.add(daysSummaryValue to daysSummaryLabel)
        }
    }

    if (itemsToDisplay.size < 3 && frequencyValue != null && frequencyUnit != null) {
        // Evitar mostrar "1 vez al día" si ya tenemos un contador de días, ya que la frecuencia
        // en ese caso suele ser implícita (1 toma en esos días).
        // A menos que la frecuencia explícitamente sea > 1 toma en esos días específicos.
        val isRedundantWithDaysSummary = daysSummaryValue != null && frequencyValue == "1" && frequencyUnit == "toma"
        if (!isRedundantWithDaysSummary) {
            itemsToDisplay.add(frequencyValue to frequencyUnit)
        }
    }

    if (itemsToDisplay.size < 3 && durationValue != null && durationUnit != null) {
        itemsToDisplay.add(durationValue to durationUnit)
    }

    // Asegurarse de no tener más de 3 elementos
    val finalItems = itemsToDisplay.take(3)


    // Aplicar el modifier del parámetro al Row principal
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = colorScheme.cardColor,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 8.dp), // Padding horizontal para el contenido interno del Row
        horizontalArrangement = Arrangement.SpaceAround, // Mantenemos SpaceAround
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (finalItems.isEmpty()) {
            Text(
                "No hay detalles de conteo.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterVertically) // Centrar texto si es el único elemento
            )
        } else {
            finalItems.forEachIndexed { index, (itemValue, itemLabel) ->
                CounterItem(
                    value = itemValue,
                    label = itemLabel,
                    valueColor = colorScheme.onBackgroundColor
                )
                // Añadir VerticalDivider si no es el último elemento y hay más de un elemento
                if (index < finalItems.size - 1) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(40.dp) // Altura del divisor
                            .width(1.dp),
                        color = colorScheme.onBackgroundColor.copy(alpha = 0.3f) // Color del divisor
                    )
                }
            }
        }
    }
}