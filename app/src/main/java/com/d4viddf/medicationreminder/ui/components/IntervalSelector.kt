package com.d4viddf.medicationreminder.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.DpSize
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun IntervalSelector(
    onIntervalChanged: (Int, Int) -> Unit  // Hours and minutes
) {
    var selectedTime by remember { mutableStateOf(LocalTime.of(0, 0)) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .background(Color(0xFF264443), shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Wheel Time Picker for Hours and Minutes
        WheelTimePicker(
            modifier = Modifier.padding(vertical = 8.dp),
            startTime = selectedTime,
            minTime = LocalTime.of(0, 0),
            maxTime = LocalTime.of(23, 59),
            size = DpSize(300.dp, 180.dp),  // Adjust the size based on your design
            textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 28.sp),
            textColor = Color.White,
            onSnappedTime = { snappedTime ->
                selectedTime = snappedTime
                onIntervalChanged(snappedTime.hour, snappedTime.minute)
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@androidx.compose.ui.tooling.preview.Preview
@Composable
fun IntervalSelectorPreview() {
    AppTheme {
        IntervalSelector(
            onIntervalChanged = { _, _ -> }
        )
    }
}
