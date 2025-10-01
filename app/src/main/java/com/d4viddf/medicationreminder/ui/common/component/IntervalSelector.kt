package com.d4viddf.medicationreminder.ui.common.component

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import java.time.LocalTime

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
@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun IntervalSelectorPreview() {
    AppTheme(dynamicColor = false) {
        IntervalSelector(
            onIntervalChanged = { _, _ -> }
        )
    }
}
