package com.d4viddf.medicationreminder.ui.screens.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // Added explicit import for Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch // Needed again for TodayScheduleItem
import androidx.compose.material3.Text
import androidx.compose.material3.LargeTopAppBar // Changed import
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState // Added import
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // Added import
import androidx.compose.ui.graphics.Color // Added import
import androidx.compose.ui.input.nestedscroll.nestedScroll // Added import
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.MedicationSchedule
import com.d4viddf.medicationreminder.data.TodayScheduleItem // Added
import com.d4viddf.medicationreminder.data.getFormattedSchedule
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
// import com.d4viddf.medicationreminder.ui.components.ThemedAppBarBackButton // Will be removed
import androidx.compose.material.icons.Icons // Added
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Added
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationSpecificTheme
import com.d4viddf.medicationreminder.viewmodel.AllSchedulesViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationReminderViewModel // Added
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun FullScheduleItem(
    itemData: Any,
    medicationName: String,
    medicationId: Int, // Added medicationId parameter
    onToggleTaken: ((itemId: String, isTaken: Boolean, medicationId: Int) -> Unit)? = null,
    isSwitchEnabled: Boolean = false,
    showSwitch: Boolean = false,
    medicationColor: MedicationColor // Added parameter
) {
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    Card( // WRAPPER CARD - Changed to Card
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Padding for the card itself
        colors = CardDefaults.cardColors(containerColor = medicationColor.cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Internal padding for content
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val itemName: String
                val itemTimeText: String

                when (itemData) {
                    is TodayScheduleItem -> {
                        itemName = itemData.medicationName
                        itemTimeText = itemData.time.format(timeFormatter)
                    }
                    is MedicationSchedule -> {
                        itemName = medicationName
                        itemTimeText = itemData.getFormattedSchedule()
                    }
                    else -> {
                        itemName = "Unknown Item"
                        itemTimeText = "N/A"
                    }
                }

                Text(
                    text = itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = medicationColor.onBackgroundColor // Set color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = itemTimeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = medicationColor.onBackgroundColor // Set color
                )
            }
            if (showSwitch && itemData is TodayScheduleItem && onToggleTaken != null) {
                Switch(
                    checked = itemData.isTaken,
                    onCheckedChange = { newTakenStatus ->
                        onToggleTaken(itemData.id, newTakenStatus, medicationId)
                    },
                    enabled = isSwitchEnabled
                    // Switch colors will be themed by default
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSchedulesScreen(
    medicationId: Int,
    showToday: Boolean, // Added parameter
    colorName: String,
    onNavigateBack: () -> Unit,
    allSchedulesViewModel: AllSchedulesViewModel = hiltViewModel(),
    medicationViewModel: MedicationViewModel = hiltViewModel(),
    medicationReminderViewModel: MedicationReminderViewModel = hiltViewModel() // Added
) {
    val medicationColor = remember(colorName) {
        try {
            MedicationColor.valueOf(colorName)
        } catch (e: IllegalArgumentException) {
            MedicationColor.LIGHT_ORANGE // Fallback
        }
    }
    val schedules by allSchedulesViewModel.getSchedules(medicationId).collectAsState(initial = emptyList())
    val todayScheduleItems by medicationReminderViewModel.todayScheduleItems.collectAsState(initial = emptyList())
    var medicationName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(medicationId, showToday) {
        val med = medicationViewModel.getMedicationById(medicationId)
        medicationName = med?.name
        if (showToday) {
            medicationReminderViewModel.loadTodaySchedule(medicationId)
        }
        // For !showToday, schedules are already collected via allSchedulesViewModel
    }

    val title = if (showToday) {
        stringResource(R.string.medication_detail_today_title) // Using "Today" as fallback for "Today's Schedule"
    } else {
        stringResource(R.string.all_schedules_title)
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState()) // Added scroll behavior

    MedicationSpecificTheme(medicationColor = medicationColor) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), // Added modifier
            topBar = {
                LargeTopAppBar( // Changed from TopAppBar
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back_button_cd)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior, // Passed scrollBehavior
                    colors = TopAppBarDefaults.largeTopAppBarColors( // Changed to largeTopAppBarColors
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent, // Or MaterialTheme.colorScheme.surface if a fill is desired on scroll
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface // If any actions were present
                    )
                )
            }
        ) { paddingValues ->
            val isLoading =
                medicationName == null && (if (showToday) todayScheduleItems.isEmpty() else schedules.isEmpty())

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    val itemsToDisplay: List<Any> = if (showToday) todayScheduleItems else schedules

                    if (itemsToDisplay.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.all_schedules_no_schedules_found), // Could be more specific for "today"
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(itemsToDisplay, key = { item ->
                            when (item) {
                                is TodayScheduleItem -> item.id
                                is MedicationSchedule -> item.id.toString() // Ensure unique string key
                                else -> item.hashCode().toString()
                            }
                        }) { itemData ->
                            FullScheduleItem(
                                itemData = itemData,
                                medicationName = medicationName ?: "Medication", // Fallback
                                medicationId = medicationId, // Pass screen's medicationId
                                medicationColor = medicationColor, // Pass medicationColor
                                showSwitch = showToday,
                                onToggleTaken = if (showToday) {
                                    { itemId, newState, mId -> // mId here is the one passed to FullScheduleItem
                                        medicationReminderViewModel.updateReminderStatus(
                                            itemId,
                                            newState,
                                            mId
                                        )
                                    }
                                } else null,
                                isSwitchEnabled = if (showToday && itemData is TodayScheduleItem) {
                                    itemData.isPast || itemData.isTaken
                                } else false
                            )
                        }
                    }
                }
            }
        }
    }
} // Closing MedicationSpecificTheme

@Preview(showBackground = true, name = "All Defined Schedules")
@Composable
fun AllSchedulesScreenDefinedPreview() {
    AppTheme {
        AllSchedulesScreen(
            medicationId = 1,
            showToday = false, // For defined schedules
            colorName = "GREEN",
            onNavigateBack = {}
            // ViewModels will use Hilt defaults or be null if signature is updated
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Today's Schedule Instances")
@Composable
fun AllSchedulesScreenTodayPreview() {
    // Sample data for TodayScheduleItem list
    val sampleTodayItems = listOf(
        TodayScheduleItem(
            "ts1",
            "Med A",
            LocalTime.of(8, 0),
            true,
            true,
            1L,
            1,
            LocalTime.of(8, 2).toString()
        ),
        TodayScheduleItem("ts2", "Med A", LocalTime.of(14, 0), false, false, 2L, 1, null),
        TodayScheduleItem(
            "ts3",
            "Med A",
            LocalTime.of(20, 0),
            false,
            true,
            3L,
            1,
            LocalTime.of(19, 55).toString()
        )
    )
    val medicationNameForPreview by remember { mutableStateOf("Sample Medication") }
    val previewMedicationId = 1
    val medicationColor = MedicationColor.BLUE

    AppTheme {
        MedicationSpecificTheme(medicationColor = medicationColor) {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState()) // Added scroll behavior for preview
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), // Added modifier for preview
                topBar = {
                    LargeTopAppBar( // Changed to LargeTopAppBar for preview
                        title = {
                            Text(
                                stringResource(
                                    R.string.todays_full_schedule_title_template,
                                    medicationNameForPreview
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { /* No-op for preview */ }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior, // Passed scrollBehavior for preview
                        colors = TopAppBarDefaults.largeTopAppBarColors( // Changed to largeTopAppBarColors for preview
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    items(sampleTodayItems, key = { it.id }) { item ->
                        FullScheduleItem(
                            itemData = item,
                            medicationName = item.medicationName,
                            medicationId = previewMedicationId, // Pass previewMedicationId
                            medicationColor = medicationColor, // Pass medicationColor for preview
                            showSwitch = true,
                            onToggleTaken = { _, _, _ -> }, // No-op for preview
                            isSwitchEnabled = item.isPast || item.isTaken
                        )
                    }
                }
            }
        }
    }
}// Close MedicationSpecificTheme
// Removed AllSchedulesScreenWithDataPreview as it's replaced by the two new previews