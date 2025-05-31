package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp // Added for dp unit
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.data.FrequencyType // Added for FrequencyType enum
import java.time.LocalDate // Added for LocalDate
import java.time.format.DateTimeFormatter // Added for DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class) // ExperimentalMaterial3ExpressiveApi is not needed for PullToRefreshBox
@Composable
fun MedicationList(
    medications: List<Medication>,
    onItemClick: (Medication) -> Unit,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier, // This modifier is passed from HomeScreen
    bottomContentPadding: Dp
) {
    val pullToRefreshState = rememberPullToRefreshState()


    PullToRefreshBox(
        state = pullToRefreshState,
        onRefresh = onRefresh,
        isRefreshing = isLoading,
        contentAlignment = Alignment.TopCenter,
        indicator = {
            // Use the standard Material 3 Indicator
            // It will be centered horizontally at the top of the PullToRefreshBox.
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isLoading,
                // modifier = Modifier.align(Alignment.TopCenter) // This is the default behavior
            )
        }
    ) {
        if (medications.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(), // This Box fills the PullToRefreshBox content area
                contentAlignment = Alignment.Center
            ) {
                // Make sure R.string.no_medications_yet is defined in your strings.xml
                Text(stringResource(id = R.string.no_medications_yet))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomContentPadding) // Apply bottom padding
            ) {
                items(medications, key = { medication -> medication.id }) { medication ->
                    MedicationCard(
                        medication = medication,
                        onClick = { onItemClick(medication) }
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Light Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@androidx.compose.ui.tooling.preview.Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationListPreview() {
    com.d4viddf.medicationreminder.ui.theme.AppTheme(dynamicColor = false) {
        val todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val sampleMedications = listOf(
            Medication(
                id = 1, name = "Amoxicillin", dosage = "250mg", color = "LIGHT_BLUE", reminderTime = "10:00 AM",
                typeId = 1,
                packageSize = 0, remainingDoses = 0, startDate = todayDate, endDate = todayDate
            ),
            Medication(
                id = 2, name = "Ibuprofen", dosage = "200mg", color = "LIGHT_RED", reminderTime = "06:00 PM",
                typeId = 1,
                packageSize = 0, remainingDoses = 0, startDate = todayDate, endDate = todayDate
            ),
            Medication(
                id = 3, name = "Vitamin C", dosage = "500mg", color = "LIGHT_ORANGE", reminderTime = "08:00 AM",
                typeId = 1,
                packageSize = 0, remainingDoses = 0, startDate = todayDate, endDate = todayDate
            )
        )
        MedicationList(
            medications = sampleMedications,
            onItemClick = {},
            isLoading = false,
            onRefresh = {},
            bottomContentPadding = 0.dp // Changed to 0.dp
        )
    }
}