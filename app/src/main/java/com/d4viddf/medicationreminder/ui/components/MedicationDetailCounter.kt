package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import java.time.format.DateTimeFormatter
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

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun CounterItemPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        CounterItem(
            value = "250mg",
            label = "Dose",
            valueColor = MedicationColor.LIGHT_ORANGE.onBackgroundColor
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationDetailCountersPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        MedicationDetailCounters(
            colorScheme = MedicationColor.LIGHT_ORANGE,
            medication = Medication(
                id = 1,
                name = "Amoxicillin",
                dosage = "250mg Capsule",
                color = "LIGHT_ORANGE",
                reminderTime = "10:00 AM", // Not specified to change, kept as is
                // Updated parameters as per request
                typeId = 1,
                packageSize = 0,
                remainingDoses = 0,
                startDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            ),
            schedule = MedicationSchedule(
                medicationId = 1,
                scheduleType = ScheduleType.AS_NEEDED, // Consistent with AS_NEEDED
                daysOfWeek = null, // Consistent with emptyList()
                specificTimes = null, // AS_NEEDED might not have specific times
                intervalHours = 0, // Default for AS_NEEDED
                intervalMinutes = 0, // Default for AS_NEEDED
                intervalStartTime = null, // Default value
                intervalEndTime = null,   // Default value
            )
        )
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

    // Resolve strings in the Composable context
    val strDoseUnitSingle = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_dose_unit_single)
    val strDoseUnitPlural = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_dose_unit_plural)
    val strFreqOnceDaily = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_freq_once_daily)
    val strFreqMultipleDaily = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_freq_multiple_daily)
    val strFreqInterval = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_freq_interval)
    val strFreqAsNeeded = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_freq_as_needed)
    val strDurationTotalDays = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_duration_total_days)
    val strDurationRemainingDays = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_duration_remaining_days)
    val strSelectStartDatePlaceholder = stringResource(id = com.d4viddf.medicationreminder.R.string.select_start_date_placeholder)
    val strSelectEndDatePlaceholder = stringResource(id = com.d4viddf.medicationreminder.R.string.select_end_date_placeholder)
    val strDaysAll = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_days_all)
    val strDaysSingle = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_days_single)
    val strDaysMultipleWeekly = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_counter_days_multiple_weekly)


    val frequencyPair: Pair<String?, String?> = remember(schedule, strDoseUnitSingle, strDoseUnitPlural, strFreqOnceDaily, strFreqMultipleDaily, strFreqInterval, strFreqAsNeeded) {
        schedule?.let { sched ->
            val timesCount = sched.specificTimes?.count() ?: 0
            when (sched.scheduleType) {
                ScheduleType.DAILY, ScheduleType.WEEKLY -> {
                    // For DAILY/WEEKLY, if daysOfWeek is specified, it implies a "X times on specific days" type of schedule.
                    // If daysOfWeek is NOT specified, it's a general "X times a day" or "X times a week" (though weekly usually has days).
                    // The distinction for label (strDoseUnitSingle/Plural vs strFreqOnceDaily/MultipleDaily) might depend on context not fully captured here.
                    // This simplified logic primarily uses timesCount for the value and strDoseUnit for label if specific days are involved,
                    // otherwise uses strFreq for more general daily/multiple daily descriptions.
                    if (!sched.daysOfWeek.isNullOrEmpty()) { // Prioritize this for more specific "X doses on these days"
                        if (timesCount == 1) "1" to strDoseUnitSingle
                        else if (timesCount > 1) "$timesCount" to strDoseUnitPlural
                        else null // No times specified, even with days? Or should default?
                    } else { // General daily/multiple times a day without specific day context from daysOfWeek
                        if (timesCount == 1) "1" to strFreqOnceDaily
                        else if (timesCount > 1) "$timesCount" to strFreqMultipleDaily
                        else null
                    }
                }
                ScheduleType.CUSTOM_ALARMS -> { // Typically multiple, specific alarms
                    if (timesCount > 0) "$timesCount" to strFreqMultipleDaily else null
                }
                ScheduleType.INTERVAL -> {
                    val h = sched.intervalHours ?: 0; val m = sched.intervalMinutes ?: 0
                    val vp = mutableListOf<String>()
                    if (h > 0) vp.add("${h}h"); if (m > 0) vp.add("${m}m")
                    if (vp.isNotEmpty()) "Cada ${vp.joinToString(" ")}" to strFreqInterval else null
                }
                ScheduleType.AS_NEEDED -> "S/N" to strFreqAsNeeded
                else -> null
            }
        }?.let { if (it.first == null && it.second == null) null else it }
            ?: (null to null)
    }
    val frequencyValue = frequencyPair.first
    val frequencyUnit = frequencyPair.second

    val durationPair: Pair<String?, String?> = remember(medication, strDurationTotalDays, strDurationRemainingDays, strSelectStartDatePlaceholder, strSelectEndDatePlaceholder) {
        medication?.let { med ->
            val today = LocalDate.now()
            var pStartDate: LocalDate? = null
            if (!med.startDate.isNullOrBlank() && med.startDate != strSelectStartDatePlaceholder) {
                try { pStartDate = LocalDate.parse(med.startDate, ReminderCalculator.dateStorableFormatter) } catch (e: DateTimeParseException) {}
            }
            var pEndDate: LocalDate? = null
            if (!med.endDate.isNullOrBlank() && med.endDate != strSelectEndDatePlaceholder) {
                try { pEndDate = LocalDate.parse(med.endDate, ReminderCalculator.dateStorableFormatter) } catch (e: DateTimeParseException) {}
            }
            when {
                pStartDate != null && pEndDate != null -> {
                    if (pEndDate.isBefore(pStartDate)) return@remember null to null
                    val total = ChronoUnit.DAYS.between(pStartDate, pEndDate) + 1
                    if (total >= 0) "$total" to strDurationTotalDays else null
                }
                pEndDate != null && !pEndDate.isBefore(today) -> {
                    val ref = today
                    if (pEndDate.isBefore(ref)) return@remember null to null
                    val remaining = ChronoUnit.DAYS.between(ref, pEndDate) + 1
                    if (remaining >= 0) "$remaining" to strDurationRemainingDays else null
                }
                else -> null
            }
        }?.let { if (it.first == null && it.second == null) null else it }
            ?: (null to null)
    }
    val durationValue = durationPair.first
    val durationUnit = durationPair.second

    val daysOfWeekInternal: List<java.time.DayOfWeek>? = remember(schedule) {
        schedule?.takeIf { (it.scheduleType == ScheduleType.DAILY || it.scheduleType == ScheduleType.WEEKLY) && !it.daysOfWeek.isNullOrEmpty() }
            ?.daysOfWeek?.sorted()
    }

    val daysSummaryPair: Pair<String?, String?> = remember(daysOfWeekInternal, strDaysAll, strDaysSingle, strDaysMultipleWeekly) {
        daysOfWeekInternal?.let { days ->
            if (days.isNotEmpty()) {
                val count = days.size
                val label = when (count) {
                    7 -> strDaysAll
                    1 -> strDaysSingle
                    // Ensure strDaysMultipleWeekly can handle being formatted with a count, or adjust logic here.
                    // For example, if strDaysMultipleWeekly is "%d days/week", you might do:
                    // else -> String.format(strDaysMultipleWeekly, count)
                    // However, if it's just "Days/Week", then count.toString() is fine for the value part.
                    else -> strDaysMultipleWeekly
                }
                // Assuming the 'value' part is the count, and 'label' is the descriptive text.
                count.toString() to label
            } else {
                null to null
            }
        } ?: (null to null)
    }
    val daysSummaryValue = daysSummaryPair.first
    val daysSummaryLabel = daysSummaryPair.second

    val itemsToDisplay = mutableListOf<Pair<String, String>>()

    // 1. Add Dose (if available and space permits)
    if (doseValue != null && doseUnit != null) {
        itemsToDisplay.add(doseValue to doseUnit)
    }

    // 2. Add Frequency/Days Information (only one of these, if space permits)
    if (itemsToDisplay.size < 3) {
        if (daysSummaryValue != null && daysSummaryLabel != null) {
            itemsToDisplay.add(daysSummaryValue to daysSummaryLabel)
        } else if (frequencyValue != null && frequencyUnit != null) {
            // This 'else if' ensures general frequency (including interval)
            // is only added if a specific day summary was not.
            itemsToDisplay.add(frequencyValue to frequencyUnit)
        }
    }

    // 3. Add Duration (if available and space permits)
    if (itemsToDisplay.size < 3 && durationValue != null && durationUnit != null) {
        itemsToDisplay.add(durationValue to durationUnit)
    }

    val finalItems = itemsToDisplay.take(3) // This line already exists and is correct.

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