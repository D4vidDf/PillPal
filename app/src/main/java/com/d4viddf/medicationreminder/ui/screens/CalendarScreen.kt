package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.viewmodel.CalendarDay
import com.d4viddf.medicationreminder.viewmodel.CalendarViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationScheduleItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMedicationDetail: (Int) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePickerDialog by remember { mutableStateOf(false) }

    if (showDatePickerDialog) {
        ShowDatePicker(
            initialSelectedDate = uiState.selectedDate,
            onDateSelected = { newDate ->
                viewModel.setSelectedDate(newDate)
                showDatePickerDialog = false
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    Scaffold(
        topBar = {
            CalendarTopAppBar(
                currentMonth = uiState.currentMonth,
                onPreviousMonthClicked = { viewModel.onPreviousMonth() },
                onNextMonthClicked = { viewModel.onNextMonth() },
                onDateSelectorClicked = { showDatePickerDialog = true }, // Show dialog
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            MonthCalendarView(
                days = uiState.daysInMonth,
                selectedDate = uiState.selectedDate,
                onDateSelected = { date -> viewModel.setSelectedDate(date) }
            )
            Divider()
            MedicationScheduleListView(
                medicationSchedules = uiState.medicationSchedules,
                currentMonth = uiState.currentMonth,
                onMedicationClicked = { medicationId -> onNavigateToMedicationDetail(medicationId) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDatePicker(
    initialSelectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTopAppBar(
    currentMonth: YearMonth,
    onPreviousMonthClicked: () -> Unit,
    onNextMonthClicked: () -> Unit,
    onDateSelectorClicked: () -> Unit,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onPreviousMonthClicked) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.previous_month))
                }
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium // Or another appropriate style
                )
                IconButton(onClick = onNextMonthClicked) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.next_month))
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = onDateSelectorClicked) { // Ensure this is being called
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = stringResource(R.string.select_date)
                )
            }
        }
    )
}

@Composable
fun MonthCalendarView(
    days: List<CalendarDay>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { // Reduced vertical padding
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = DayOfWeek.values()
            for (dayOfWeek in daysOfWeek) {
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp // Smaller font for day names
                )
            }
        }

        val firstDayOfMonth = days.firstOrNull()?.date?.withDayOfMonth(1) ?: LocalDate.now().withDayOfMonth(1)
        val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0, Monday = 1 ... Saturday = 6 for US, adjust if locale needs Monday as 0

        val weeks = mutableListOf<List<CalendarDay?>>()
        var currentWeek = mutableListOf<CalendarDay?>()

        for (i in 0 until dayOfWeekOffset) { // Corrected loop for offset
            currentWeek.add(null)
        }

        days.forEach { day ->
            currentWeek.add(day)
            if (currentWeek.size == 7) {
                weeks.add(currentWeek.toList())
                currentWeek = mutableListOf()
            }
        }
        if (currentWeek.isNotEmpty()) {
            while (currentWeek.size < 7) {
                currentWeek.add(null)
            }
            weeks.add(currentWeek.toList())
        }

        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f).padding(1.dp)) { // Add small padding around each cell
                        if (day != null) {
                            DayCell(
                                day = day,
                                onDateSelected = { onDateSelected(day.date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    onDateSelected: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .background(
                color = if (day.isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(4.dp) // Consistent shape
            )
            .then(
                if (day.isToday && !day.isSelected) Modifier.border( // Border only if today and not selected
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .clickable(onClick = onDateSelected)

    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (day.isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                      else if (day.isToday) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@Composable
fun MedicationScheduleListView(
    medicationSchedules: List<MedicationScheduleItem>,
    currentMonth: YearMonth,
    onMedicationClicked: (Int) -> Unit
) {
    if (medicationSchedules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_medications_scheduled_for_this_month))
        }
        return
    }

    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp)) {
        items(medicationSchedules, key = { it.medication.id }) { scheduleItem ->
            MedicationScheduleRow(
                scheduleItem = scheduleItem,
                currentMonth = currentMonth,
                onClicked = { onMedicationClicked(scheduleItem.medication.id) }
            )
            Divider(thickness = 0.5.dp) // Thinner divider
        }
    }
}

@Composable
fun MedicationScheduleRow(
    scheduleItem: MedicationScheduleItem,
    currentMonth: YearMonth,
    onClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClicked)
            .padding(vertical = 6.dp, horizontal = 4.dp), // Adjusted padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            scheduleItem.medication.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.4f).padding(end = 4.dp), // Added padding to prevent text touching the bar
            maxLines = 2 // Allow medication name to wrap if too long
        )

        Box(
            modifier = Modifier
                .weight(0.6f)
                .height(36.dp) // Slightly reduced height
                .background(
                    color = Color(scheduleItem.medication.color ?: 0xFFCCCCCC).copy(alpha = 0.2f), // Softer alpha
                    shape = RoundedCornerShape(6.dp) // Slightly more rounded
                )
                .padding(horizontal = 6.dp, vertical = 4.dp), // Adjusted padding
            contentAlignment = Alignment.CenterStart
        ) {
            val scheduleText = mutableListOf<String>()
            scheduleItem.startDateText?.let { scheduleText.add(it) }
            scheduleItem.endDateText?.let { scheduleText.add(it) }

            if (scheduleText.isNotEmpty()) {
                Text(
                    text = scheduleText.joinToString(" - "),
                    fontSize = 9.sp, // Smaller font for details
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), // Use theme color
                    maxLines = 2
                )
            } else {
                 Text(
                    text = scheduleItem.medication.name,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewLight() {
    AppTheme(darkTheme = false) {
        // Provide a mock ViewModel or uiState for better preview
        val previewViewModel: CalendarViewModel = hiltViewModel() // This might not work well in previews without Hilt setup
        // For a more robust preview, consider creating a fake UiState
        val previewUiState = com.d4viddf.medicationreminder.viewmodel.CalendarUiState(
            selectedDate = LocalDate.now(),
            currentMonth = YearMonth.now(),
            daysInMonth = (1..YearMonth.now().lengthOfMonth()).map {
                val date = YearMonth.now().atDay(it)
                com.d4viddf.medicationreminder.viewmodel.CalendarDay(date, date.isEqual(LocalDate.now()), date.isEqual(LocalDate.now()))
            },
            medicationSchedules = listOf(
                MedicationScheduleItem(
                    medication = com.d4viddf.medicationreminder.data.Medication(id = 1, name = "Metformin", color = 0xFFE91E63),
                    schedule = com.d4viddf.medicationreminder.data.MedicationSchedule(medicationId = 1, scheduleType = com.d4viddf.medicationreminder.data.ScheduleType.DAILY, intervalHours = null, intervalMinutes = null, daysOfWeek = null, specificTimes = null, intervalStartTime = null, intervalEndTime = null),
                    startDateText = "Starts Jan 10",
                    endDateText = "Ends Jan 25"
                ),
                MedicationScheduleItem(
                    medication = com.d4viddf.medicationreminder.data.Medication(id = 2, name = "Lisinopril", color = 0xFF4CAF50),
                    schedule = com.d4viddf.medicationreminder.data.MedicationSchedule(medicationId = 2, scheduleType = com.d4viddf.medicationreminder.data.ScheduleType.DAILY, intervalHours = null, intervalMinutes = null, daysOfWeek = null, specificTimes = null, intervalStartTime = null, intervalEndTime = null)
                )
            )
        )
        val currentMonth = previewUiState.currentMonth
        Scaffold(
            topBar = {
                CalendarTopAppBar(currentMonth, {}, {}, {}, {})
            }
        ) {Paddings ->
            Column(Modifier.padding(Paddings)) {
                MonthCalendarView(previewUiState.daysInMonth, previewUiState.selectedDate, {})
                Divider()
                MedicationScheduleListView(previewUiState.medicationSchedules, currentMonth, {})
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun CalendarScreenPreviewDark() {
     AppTheme(darkTheme = true) {
        val previewUiState = com.d4viddf.medicationreminder.viewmodel.CalendarUiState(
            selectedDate = LocalDate.now(),
            currentMonth = YearMonth.now(),
            daysInMonth = (1..YearMonth.now().lengthOfMonth()).map {
                val date = YearMonth.now().atDay(it)
                com.d4viddf.medicationreminder.viewmodel.CalendarDay(date, date.isEqual(LocalDate.now()), date.isEqual(LocalDate.now()))
            },
            medicationSchedules = listOf(
                MedicationScheduleItem(
                    medication = com.d4viddf.medicationreminder.data.Medication(id = 1, name = "Aspirin", color = 0xFF03A9F4),
                    schedule = com.d4viddf.medicationreminder.data.MedicationSchedule(medicationId = 1, scheduleType = com.d4viddf.medicationreminder.data.ScheduleType.DAILY, intervalHours = null, intervalMinutes = null, daysOfWeek = null, specificTimes = null, intervalStartTime = null, intervalEndTime = null),
                    startDateText = "Ongoing"
                )
            )
        )
         val currentMonth = previewUiState.currentMonth
        Scaffold(
            topBar = {
                CalendarTopAppBar(currentMonth, {}, {}, {}, {})
            }
        ) {Paddings ->
            Column(Modifier.padding(Paddings)) {
                MonthCalendarView(previewUiState.daysInMonth, previewUiState.selectedDate, {})
                Divider()
                MedicationScheduleListView(previewUiState.medicationSchedules, currentMonth, {})
            }
        }
    }
}
