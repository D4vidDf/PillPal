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
import androidx.compose.ui.res.stringResource
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
            // Assuming the unit part (parts[1]) is already derived from non-translatable or already handled parts
            // The main concern here would be if parts[1] itself needs to be a translatable key.
            // For now, direct use from dosage string.
            if (parts.size == 2) parts[0] to parts[1] else fullDosage to ""
        }
    }
    val doseValue = dosePair.first
    val doseUnit = dosePair.second // This might need mapping to a string resource if it's a translatable unit name.

    val frequencyPair: Pair<String?, String?> = remember(schedule) {
        schedule?.let { sched ->
            val timesCount = sched.specificTimes?.split(',')?.count { it.isNotBlank() } ?: 0
            when (sched.scheduleType) {
                ScheduleType.DAILY, ScheduleType.WEEKLY -> { // Combined logic for daily/weekly
                    if (!sched.daysOfWeek.isNullOrBlank()) { // If specific days are set
                        if (timesCount == 1) "1" to stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_dose_unit_single)
                        else if (timesCount > 1) "$timesCount" to stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_dose_unit_plural)
                        else null // No specific times set for these days
                    } else { // No specific days, implies all days for DAILY
                        if (timesCount == 1) "1" to stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_freq_once_daily)
                        else if (timesCount > 1) "$timesCount" to stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_freq_multiple_daily)
                        else null
                    }
                }
                ScheduleType.CUSTOM_ALARMS -> {
                    if (timesCount > 0) "$timesCount" to stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_freq_multiple_daily) else null
                }
                ScheduleType.INTERVAL -> {
                    val h = sched.intervalHours ?: 0; val m = sched.intervalMinutes ?: 0
                    val vp = mutableListOf<String>()
                    if (h > 0) vp.add("${h}h"); if (m > 0) vp.add("${m}m")
                    // "Cada %1$s" would need a placeholder if "Cada" is part of the string resource
                    if (vp.isNotEmpty()) "Cada ${vp.joinToString(" ")}" to stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_freq_interval) else null
                }
                ScheduleType.AS_NEEDED -> "S/N" to stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_freq_as_needed)
                else -> null
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
            if (!med.startDate.isNullOrBlank() && med.startDate != stringResource(id = com.d4viddf.medicationreminder.R.string.select_start_date_placeholder)) {
                try { pStartDate = LocalDate.parse(med.startDate, ReminderCalculator.dateStorableFormatter) } catch (e: DateTimeParseException) {}
            }
            var pEndDate: LocalDate? = null
            if (!med.endDate.isNullOrBlank() && med.endDate != stringResource(id = com.d4viddf.medicationreminder.R.string.select_end_date_placeholder)) {
                try { pEndDate = LocalDate.parse(med.endDate, ReminderCalculator.dateStorableFormatter) } catch (e: DateTimeParseException) {}
            }
            when {
                pStartDate != null && pEndDate != null -> {
                    if (pEndDate.isBefore(pStartDate)) return@remember null to null
                    val total = ChronoUnit.DAYS.between(pStartDate, pEndDate) + 1
                    if (total >= 0) "$total" to stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_duration_total_days) else null
                }
                pEndDate != null && !pEndDate.isBefore(today) -> {
                    val ref = if (pStartDate != null && pStartDate.isAfter(today)) pStartDate else today
                    if (pEndDate.isBefore(ref)) return@remember null to null
                    val remaining = ChronoUnit.DAYS.between(ref, pEndDate) + 1
                    if (remaining >= 0) "$remaining" to stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_duration_remaining_days) else null
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
                    7 -> stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_days_all)
                    1 -> stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_days_single)
                    else -> stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_days_multiple_weekly) // string will need placeholder for count e.g. "%1$d días/semana"
                }
                // If label needs the count, it should be part of the string resource like "%1$d días/semana"
                // For now, using count as value and label as unit.
                if (label == stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_days_multiple_weekly)) {
                    count.toString() to label // For "X días/semana"
                } else {
                    // For "todos los días" or "día", the count might be implicit or stylistic.
                    // If "todos los días" should show "7", then: if (count == 7) "7" to label else count.toString() to label
                    // Keeping it simple for now:
                    count.toString() to label
                }
            } else {
                null to null
            }
        } ?: (null to null)
    }
    val daysSummaryValue = daysSummaryPair.first
    val daysSummaryLabel = daysSummaryPair.second

    val itemsToDisplay = mutableListOf<Pair<String, String>>()


    // Logic to select up to 3 items to display, prioritizing dose, then days/frequency, then duration.
    if (doseValue != null && doseUnit != null) {
        itemsToDisplay.add(doseValue to doseUnit)
    }

    // If specific days are set, use that summary. Otherwise, use the general frequency.
    if (daysSummaryValue != null && daysSummaryLabel != null) {
        if (itemsToDisplay.size < 3) {
            itemsToDisplay.add(daysSummaryValue to daysSummaryLabel)
        }
    } else if (frequencyValue != null && frequencyUnit != null) {
        if (itemsToDisplay.size < 3) {
            itemsToDisplay.add(frequencyValue to frequencyUnit)
        }
    }
    // If there's still space and we haven't added a day/frequency related counter (or if it's different), add general frequency.
    // This logic might need refinement to avoid redundancy, e.g. not showing "1 vez al día" if "todos los días" is already shown.
    // For now, keeping it simple: if daysSummary was added, this general frequency might be skipped if it's too similar or space is full.
    if (itemsToDisplay.size < 3 && frequencyValue != null && frequencyUnit != null && daysSummaryValue == null) {
         // Only add general frequency if specific day summary wasn't added and there's space.
        itemsToDisplay.add(frequencyValue to frequencyUnit)
    }


    if (itemsToDisplay.size < 3 && durationValue != null && durationUnit != null) {
        itemsToDisplay.add(durationValue to durationUnit)
    }

    val finalItems = itemsToDisplay.take(3)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = colorScheme.cardColor,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (finalItems.isEmpty()) {
            Text(
                stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_no_details),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        } else {
            finalItems.forEachIndexed { index, (itemValue, itemLabel) ->
                CounterItem(
                    value = itemValue,
                    label = itemLabel, // itemLabel should now be a localized string from the logic above
                    valueColor = colorScheme.onBackgroundColor
                )
                if (index < finalItems.size - 1) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = colorScheme.onBackgroundColor.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}