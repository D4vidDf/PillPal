package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.util.*

@Composable
fun FrequencySelector(
    selectedFrequency: String,
    onFrequencySelected: (String) -> Unit,
    onDaysSelected: (List<Int>) -> Unit,
    selectedDays: List<Int>,
    onIntervalChanged: (Int, Int) -> Unit,  // hours and minutes
    onTimesSelected: (List<LocalTime>) -> Unit,
    selectedTimes: List<LocalTime>,
    modifier: Modifier = Modifier
) {
    val frequencies = listOf("Once a day", "Multiple times a day", "Interval")

    Column(modifier = modifier.padding(16.dp)) {
        // Title and Dropdown Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reminder",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            DropdownMenu(
                selectedFrequency = selectedFrequency,
                options = frequencies,
                onSelectedOption = onFrequencySelected
            )
        }

        // Show NotificationSelector for "Once a day"
        if (selectedFrequency == "Once a day") {
            NotificationSelector(
                onDaysSelected = onDaysSelected,
                selectedDays = selectedDays
            )
        }

        // Show IntervalSelector for "Interval"
        if (selectedFrequency == "Interval") {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interval",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = {
                        // Show info dialog about interval
                    }
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                }
            }
            IntervalSelector(
                onIntervalChanged = onIntervalChanged,
            )
        }

        // Show Custom Alarms for "Multiple times a day"
        if (selectedFrequency == "Multiple times a day") {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Alarms",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = {
                        // Show info dialog about custom alarms
                    }
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                }
            }
            CustomAlarmsSelector(
                selectedTimes = selectedTimes,
                onTimesSelected = onTimesSelected
            )
        }
    }
}

@Composable
fun DropdownMenu(
    selectedFrequency: String,
    options: List<String>,
    onSelectedOption: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        TextButton(onClick = { expanded = true }) {
            Text(selectedFrequency, style = MaterialTheme.typography.bodyLarge)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelectedOption(option)
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationSelector(
    onDaysSelected: (List<Int>) -> Unit,
    selectedDays: List<Int>
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDaysState by remember { mutableStateOf(selectedDays) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Select the days",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            daysOfWeek.forEachIndexed { index, day ->
                val isSelected = selectedDaysState.contains(index + 1)
                val color = if (isSelected) Color(0xFF264443) else Color(0xFFEFF0F4)
                val textColor = if (isSelected) Color.White else Color.Black

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(color, shape = RoundedCornerShape(40.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (isSelected) Color(0xFFF0BF70) else Color.White,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Check",
                                tint = if (isSelected) Color.Black else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CustomAlarmsSelector(
    selectedTimes: List<LocalTime>,
    onTimesSelected: (List<LocalTime>) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = {
                // Implement Time Picker Dialog
                // Once time is selected, add it to selectedTimes
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Alarm")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedTimes.forEach { time ->
                AlarmChip(
                    time = time,
                    onDelete = {
                        // Remove the selected time
                        val updatedTimes = selectedTimes.toMutableList().apply { remove(time) }
                        onTimesSelected(updatedTimes)
                    }
                )
            }
        }
    }
}

@Composable
fun AlarmChip(
    time: LocalTime,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.wrapContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = time.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Delete Alarm", tint = Color.Red)
            }
        }
    }
}
