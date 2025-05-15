package com.d4viddf.medicationreminder.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke // For OutlinedButton border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FrequencySelector(
    selectedFrequency: String,
    onFrequencySelected: (String) -> Unit,
    // "Once a day" props
    selectedDays: List<Int>,
    onDaysSelected: (List<Int>) -> Unit,
    onceADayTime: LocalTime?,
    onOnceADayTimeSelected: (LocalTime) -> Unit, // Usually set with non-null from picker
    // "Multiple times a day" props
    selectedTimes: List<LocalTime>,
    onTimesSelected: (List<LocalTime>) -> Unit,
    // "Interval" props
    intervalHours: Int,
    onIntervalHoursChanged: (Int) -> Unit,
    intervalMinutes: Int,
    onIntervalMinutesChanged: (Int) -> Unit,
    intervalStartTime: LocalTime?,
    onIntervalStartTimeSelected: (LocalTime) -> Unit, // Usually set with non-null from picker
    intervalEndTime: LocalTime?,
    onIntervalEndTimeSelected: (LocalTime?) -> Unit, // <<< CORRECTED to accept LocalTime?
    modifier: Modifier = Modifier
) {
    val frequencies = listOf("Once a day", "Multiple times a day", "Interval")
    val uiTimeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    var showTimePickerFor by remember { mutableStateOf<TimePickerTarget?>(null) }
    val timePickerState = rememberTimePickerState(is24Hour = true)

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Frequency", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            DropdownMenuFrequencies(selectedFrequency, frequencies, onFrequencySelected)
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        when (selectedFrequency) {
            "Once a day" -> {
                SectionTitle("Daily Reminder Time & Days")
                OutlinedButton(
                    onClick = {
                        timePickerState.hour = onceADayTime?.hour ?: LocalTime.now().hour
                        timePickerState.minute = onceADayTime?.minute ?: LocalTime.now().minute
                        showTimePickerFor = TimePickerTarget.ONCE_A_DAY
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Filled.ThumbUp, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(onceADayTime?.format(uiTimeFormatter) ?: "Select Reminder Time")
                }
                Spacer(Modifier.height(8.dp))
                DaySelector(selectedDays, onDaysSelected)
            }
            "Multiple times a day" -> {
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
            "Interval" -> {
                SectionTitle("Repetition Interval")
                IntervalDurationSelector(intervalHours, intervalMinutes, onIntervalHoursChanged, onIntervalMinutesChanged)

                Spacer(Modifier.height(16.dp))
                SectionTitle("Daily Active Range for Interval")
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeRangeButton(
                        label = "Start Time", time = intervalStartTime, placeholder = "Set Start",
                        onClick = {
                            timePickerState.hour = intervalStartTime?.hour ?: 6
                            timePickerState.minute = intervalStartTime?.minute ?: 0
                            showTimePickerFor = TimePickerTarget.INTERVAL_START
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TimeRangeButton(
                        label = "End Time", time = intervalEndTime, placeholder = "Set End",
                        onClick = {
                            timePickerState.hour = intervalEndTime?.hour ?: 22
                            timePickerState.minute = intervalEndTime?.minute ?: 0
                            showTimePickerFor = TimePickerTarget.INTERVAL_END
                        },
                        modifier = Modifier.weight(1f)
                        // enable button even if start time is not set, validation on confirm
                    )
                }
            }
        }
    }

    if (showTimePickerFor != null) {
        TimePickerDialog(
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
                                onIntervalEndTimeSelected(null) // Pass null to clear it
                            }
                            onIntervalStartTimeSelected(selectedLocalTime)
                        }
                        TimePickerTarget.INTERVAL_END -> {
                            if (intervalStartTime == null || selectedLocalTime.isAfter(intervalStartTime)) {
                                onIntervalEndTimeSelected(selectedLocalTime)
                            } else {
                                // TODO: Show Toast/Snackbar: "End time must be after start time."
                            }
                        }
                        null -> {} // Should not happen
                    }
                    showTimePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePickerFor = null }) { Text("Cancel") } }
        ) { TimePicker(state = timePickerState, modifier = Modifier.fillMaxWidth()) }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TimeRangeButton(
    label: String,
    time: LocalTime?,
    placeholder: String,
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
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp) // Adjusted padding
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(2.dp))
            Text(time?.format(timeFormatter) ?: placeholder, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
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
    selectedFrequency: String,
    options: List<String>,
    onSelectedOption: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        TextButton(onClick = { expanded = true }) {
            Text(selectedFrequency, style = MaterialTheme.typography.bodyLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelectedOption(option); expanded = false }
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
    val daysOfWeekLabels = remember { listOf("M", "T", "W", "T", "F", "S", "S") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Repeat on days:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeekLabels.forEachIndexed { index, dayLabel ->
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
        SectionTitle("Custom Alarm Times")
        Button(
            onClick = onShowTimePicker,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Add, "Add New Alarm Time", Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Add Alarm Time")
        }

        if (selectedTimes.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Scheduled times:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
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
                                Icon(Icons.Filled.Close, contentDescription = "Delete time ${time.format(timeFormatter)}")
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
        Text("Every", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
        Box(Modifier.width(70.dp)) {
            IOSWheelPicker((0..23).toList(), hours, onHoursChanged, Modifier.height(120.dp))
        }
        Text("hrs", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))
        Box(Modifier.width(70.dp)) {
            IOSWheelPicker((0..55 step 5).toList(), minutes, onMinutesChanged, Modifier.height(120.dp))
        }
        Text("min", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center), modifier=Modifier.fillMaxWidth()) },
        text = { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center, content = content) },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        modifier = Modifier.fillMaxWidth(0.95f)
    )
}
