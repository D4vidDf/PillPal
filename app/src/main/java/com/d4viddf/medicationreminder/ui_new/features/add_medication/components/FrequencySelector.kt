package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
// import androidx.compose.material.icons.filled.ThumbUp // Removed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource // Added import
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.FrequencyType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Ensure other Material 3 imports like androidx.compose.material3.Text,
// androidx.compose.material3.TextButton, androidx.compose.material3.TimePicker etc. are present.
// Based on the previous file read, they seem to be.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FrequencySelector(
    selectedFrequency: FrequencyType,
    onFrequencySelected: (FrequencyType) -> Unit,
    // "Once a day" props
    selectedDays: List<Int>,
    onDaysSelected: (List<Int>) -> Unit,
    onceADayTime: LocalTime?,
    onOnceADayTimeSelected: (LocalTime) -> Unit,
    // "Multiple times a day" props
    selectedTimes: List<LocalTime>,
    onTimesSelected: (List<LocalTime>) -> Unit,
    // "Interval" props
    intervalHours: Int,
    onIntervalHoursChanged: (Int) -> Unit,
    intervalMinutes: Int,
    onIntervalMinutesChanged: (Int) -> Unit,
    intervalStartTime: LocalTime?,
    onIntervalStartTimeSelected: (LocalTime) -> Unit,
    intervalEndTime: LocalTime?,
    onIntervalEndTimeSelected: (LocalTime?) -> Unit,
    modifier: Modifier = Modifier
) {
    val frequencies = FrequencyType.values().toList()
    val uiTimeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    var showTimePickerFor by remember { mutableStateOf<TimePickerTarget?>(null) }
    val timePickerState = rememberTimePickerState(is24Hour = true)

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = com.d4viddf.medicationreminder.R.string.frequency_selector_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            DropdownMenuFrequencies(selectedFrequency, frequencies, onFrequencySelected)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        when (selectedFrequency) {
            FrequencyType.ONCE_A_DAY -> {
                SectionTitle(stringResource(id = com.d4viddf.medicationreminder.R.string.freq_daily_reminder_time_days))
                OutlinedButton(
                    onClick = {
                        timePickerState.hour = onceADayTime?.hour ?: LocalTime.now().hour
                        timePickerState.minute = onceADayTime?.minute ?: LocalTime.now().minute
                        showTimePickerFor = TimePickerTarget.ONCE_A_DAY
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(painter = painterResource(id = R.drawable.rounded_thumb_up_24), null, Modifier.size(ButtonDefaults.IconSize)) // contentDescription can be null for decorative icons
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(onceADayTime?.format(uiTimeFormatter) ?: stringResource(id = com.d4viddf.medicationreminder.R.string.freq_select_reminder_time_button))
                }
                Spacer(Modifier.height(8.dp))
                DaySelector(selectedDays, onDaysSelected)
            }
            FrequencyType.MULTIPLE_TIMES_A_DAY -> {
                CustomAlarmsSelector(
                    selectedTimes = selectedTimes,
                    onTimesSelected = onTimesSelected,
                    onShowTimePicker = {
                        timePickerState.hour = LocalTime.now().hour
                        timePickerState.minute = LocalTime.now().minute
                        showTimePickerFor = TimePickerTarget.CUSTOM_ALARM
                    }
                )
            }
            FrequencyType.INTERVAL -> {
                SectionTitle(stringResource(id = com.d4viddf.medicationreminder.R.string.freq_repetition_interval_title))
                IntervalDurationSelector(intervalHours, intervalMinutes, onIntervalHoursChanged, onIntervalMinutesChanged)

                Spacer(Modifier.height(16.dp))
                SectionTitle(stringResource(id = com.d4viddf.medicationreminder.R.string.freq_daily_active_range_label))
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeRangeButton(
                        label = stringResource(id = com.d4viddf.medicationreminder.R.string.freq_start_time_button_label),
                        time = intervalStartTime,
                        placeholder = stringResource(id = com.d4viddf.medicationreminder.R.string.freq_set_start_placeholder),
                        onClick = {
                            timePickerState.hour = intervalStartTime?.hour ?: 6
                            timePickerState.minute = intervalStartTime?.minute ?: 0
                            showTimePickerFor = TimePickerTarget.INTERVAL_START
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TimeRangeButton(
                        label = stringResource(id = com.d4viddf.medicationreminder.R.string.freq_end_time_button_label),
                        time = intervalEndTime,
                        placeholder = stringResource(id = com.d4viddf.medicationreminder.R.string.freq_set_end_placeholder),
                        onClick = {
                            timePickerState.hour = intervalEndTime?.hour ?: 22
                            timePickerState.minute = intervalEndTime?.minute ?: 0
                            showTimePickerFor = TimePickerTarget.INTERVAL_END
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showTimePickerFor != null) {
        TimePickerDialog(
            title = when (showTimePickerFor) { // Title for the dialog
                TimePickerTarget.ONCE_A_DAY -> stringResource(id = com.d4viddf.medicationreminder.R.string.freq_select_reminder_time_button)
                TimePickerTarget.CUSTOM_ALARM -> stringResource(id = com.d4viddf.medicationreminder.R.string.freq_add_alarm_time_dialog_title)
                TimePickerTarget.INTERVAL_START -> stringResource(id = com.d4viddf.medicationreminder.R.string.freq_start_time_button_label)
                TimePickerTarget.INTERVAL_END -> stringResource(id = com.d4viddf.medicationreminder.R.string.freq_end_time_button_label)
                null -> "" // Should not happen
            },
            onDismissRequest = { showTimePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    val selectedLocalTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    when (showTimePickerFor) {
                        TimePickerTarget.ONCE_A_DAY -> onOnceADayTimeSelected(selectedLocalTime)
                        TimePickerTarget.CUSTOM_ALARM -> {
                            if (!selectedTimes.contains(selectedLocalTime)) {
                                onTimesSelected((selectedTimes + selectedLocalTime).sorted())
                            }
                        }
                        TimePickerTarget.INTERVAL_START -> {
                            if (intervalEndTime != null && selectedLocalTime.isAfter(intervalEndTime)) {
                                onIntervalEndTimeSelected(null)
                            }
                            onIntervalStartTimeSelected(selectedLocalTime)
                        }
                        TimePickerTarget.INTERVAL_END -> {
                            if (intervalStartTime == null || selectedLocalTime.isAfter(intervalStartTime)) {
                                onIntervalEndTimeSelected(selectedLocalTime)
                            } else {
                                // TODO: Show Toast/Snackbar for error
                            }
                        }
                        null -> {}
                    }
                    showTimePickerFor = null
                }) { Text(stringResource(id = com.d4viddf.medicationreminder.R.string.dialog_ok_button)) }
            },
            dismissButton = { TextButton(onClick = { showTimePickerFor = null }) { Text(stringResource(id = com.d4viddf.medicationreminder.R.string.dialog_cancel_button)) } }
        ) { TimePicker(state = timePickerState, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun TimeRangeButton(
    label: String, // Already a string resource
    time: LocalTime?,
    placeholder: String, // Already a string resource
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium) // label is a stringResource
            Spacer(Modifier.height(2.dp))
            Text(time?.format(timeFormatter) ?: placeholder, style = MaterialTheme.typography.bodyLarge) // placeholder is a stringResource
        }
    }
}

@Composable
private fun SectionTitle(title: String) { // title is already passed as a string resource
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
    )
}

private enum class TimePickerTarget {
    ONCE_A_DAY, CUSTOM_ALARM, INTERVAL_START, INTERVAL_END
}

@Composable
fun DropdownMenuFrequencies(
    selectedFrequency: FrequencyType,
    options: List<FrequencyType>,
    onSelectedOption: (FrequencyType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(id = selectedFrequency.stringResId), style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(id = option.stringResId)) },
                    onClick = {
                        onSelectedOption(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DaySelector(
    selectedDays: List<Int>,
    onDaysSelected: (List<Int>) -> Unit
) {
    val daysOfWeekLabels = remember {
        listOf(
            com.d4viddf.medicationreminder.R.string.day_mon_initial, // Assuming new keys for initials
            com.d4viddf.medicationreminder.R.string.day_tue_initial,
            com.d4viddf.medicationreminder.R.string.day_wed_initial,
            com.d4viddf.medicationreminder.R.string.day_thu_initial,
            com.d4viddf.medicationreminder.R.string.day_fri_initial,
            com.d4viddf.medicationreminder.R.string.day_sat_initial,
            com.d4viddf.medicationreminder.R.string.day_sun_initial
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(id = com.d4viddf.medicationreminder.R.string.freq_repeat_on_days_label), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeekLabels.forEachIndexed { index, dayResId ->
                val dayLabel = stringResource(id = dayResId)
                val dayNumber = index + 1
                val isSelected = selectedDays.contains(dayNumber)
                OutlinedButton(
                    onClick = {
                        val updatedDays = selectedDays.toMutableList()
                        if (isSelected) updatedDays.remove(dayNumber) else updatedDays.add(dayNumber)
                        onDaysSelected(updatedDays.sorted())
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Text(
                        dayLabel,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomAlarmsSelector(
    selectedTimes: List<LocalTime>,
    onTimesSelected: (List<LocalTime>) -> Unit,
    onShowTimePicker: () -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(id = com.d4viddf.medicationreminder.R.string.freq_custom_alarm_times_title))
        Button(
            onClick = onShowTimePicker,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Add, stringResource(id = com.d4viddf.medicationreminder.R.string.freq_add_new_alarm_button) , Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(id = com.d4viddf.medicationreminder.R.string.freq_add_alarm_time_dialog_title)) // Or freq_add_new_alarm_button depending on context
        }

        if (selectedTimes.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(id = com.d4viddf.medicationreminder.R.string.freq_scheduled_times_label), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                selectedTimes.sorted().forEach { time ->
                    InputChip(
                        selected = false,
                        onClick = { /* TODO: Implement editing of existing time? */ },
                        label = { Text(time.format(timeFormatter)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { onTimesSelected(selectedTimes.filter { it != time }) },
                                modifier = Modifier.size(InputChipDefaults.IconSize)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(id = com.d4viddf.medicationreminder.R.string.freq_delete_time_acc, time.format(timeFormatter)))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun IntervalDurationSelector(
    hours: Int, minutes: Int,
    onHoursChanged: (Int) -> Unit, onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
    Text(stringResource(id = R.string.freq_every_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
        Box(Modifier.width(70.dp)) {
            IOSWheelPicker(
                items = (0..23).toList(),
                selectedItem = hours,
                onItemSelected = onHoursChanged,
                modifier = Modifier.height(120.dp),
                displayTransform = { it.toString().padStart(2, '0') }
            )
        }
        Text(stringResource(id = R.string.freq_hours_unit), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))
        Box(Modifier.width(70.dp)) {
            IOSWheelPicker(
                items = (0..55 step 5).toList(),
                selectedItem = minutes,
                onItemSelected = onMinutesChanged,
                modifier = Modifier.height(120.dp),
                displayTransform = { it.toString().padStart(2, '0') }
            )
        }
        Text(stringResource(id = R.string.freq_minutes_unit), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

// Removed local TimePickerDialog definition to use the one from ui.components
// Ensure com.d4viddf.medicationreminder.ui.components.TimePickerDialog is imported at the top of the file.

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun FrequencySelectorPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        FrequencySelector(
            selectedFrequency = FrequencyType.ONCE_A_DAY,
            onFrequencySelected = {},
            selectedDays = emptyList(),
            onDaysSelected = {},
            onceADayTime = null,
            onOnceADayTimeSelected = {},
            selectedTimes = emptyList(),
            onTimesSelected = {},
            intervalHours = 0,
            onIntervalHoursChanged = {},
            intervalMinutes = 0,
            onIntervalMinutesChanged = {},
            intervalStartTime = null,
            onIntervalStartTimeSelected = {},
            intervalEndTime = null,
            onIntervalEndTimeSelected = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun DropdownMenuFrequenciesPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        DropdownMenuFrequencies(
            selectedFrequency = FrequencyType.ONCE_A_DAY,
            options = FrequencyType.values().toList(),
            onSelectedOption = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun DaySelectorPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        DaySelector(
            selectedDays = emptyList(),
            onDaysSelected = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun CustomAlarmsSelectorPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        CustomAlarmsSelector(
            selectedTimes = emptyList(),
            onTimesSelected = {},
            onShowTimePicker = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun IntervalDurationSelectorPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        IntervalDurationSelector(
            hours = 0,
            minutes = 0,
            onHoursChanged = {},
            onMinutesChanged = {}
        )
    }
}
