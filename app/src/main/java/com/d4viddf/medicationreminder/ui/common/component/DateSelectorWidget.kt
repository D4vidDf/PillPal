package com.d4viddf.medicationreminder.ui.common.component

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import java.time.LocalDate

@Composable
fun DateSelectorWidget(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val dates = remember { generateDatesList() } // Generate a list of dates, e.g., the upcoming week.

    LazyRow {
        items(dates) { date ->
            val isSelected = date == selectedDate
            DateItem(
                date = date,
                isSelected = isSelected,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateItem(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
            .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfWeek.name.take(3),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

fun generateDatesList(): List<LocalDate> {
    val today = LocalDate.now()
    return (0..6).map { today.plusDays(it.toLong()) } // Generate a week starting from today.
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun DateSelectorWidgetPreview() {
    AppTheme(dynamicColor = false) {
        DateSelectorWidget(
            selectedDate = LocalDate.now(),
            onDateSelected = {}
        )
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun DateItemPreview() {
    AppTheme(dynamicColor = false) {
        DateItem(
            date = LocalDate.now(),
            isSelected = true,
            onClick = {}
        )
    }
}
