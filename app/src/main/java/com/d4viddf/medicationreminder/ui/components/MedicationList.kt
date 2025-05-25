package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// Old Material imports removed:
// import androidx.compose.material.pullrefresh.pullRefresh
// import androidx.compose.material.pullrefresh.rememberPullRefreshState
// Material3ExpressiveApi imports removed (were added in previous step):
// import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
// import androidx.compose.material3.expressive.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api // For new M3 pull-to-refresh
import androidx.compose.material3.pulltorefresh.PullToRefreshBox // Added
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults // Added
import androidx.compose.material3.pulltorefresh.PullToRefreshState // Added
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState // Added
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.d4viddf.medicationreminder.data.Medication

@OptIn(ExperimentalMaterial3Api::class) // Ensure this OptIn is present
@Composable
fun MedicationList(
    medications: List<Medication>,
    onItemClick: (Medication) -> Unit,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberPullToRefreshState() // New M3 state

    PullToRefreshBox(
        modifier = modifier, // Apply the main modifier here
        state = state,
        onRefresh = onRefresh,
        isRefreshing = isLoading,
        indicator = {
            // Pass state and isRefreshing to the default indicator
            PullToRefreshDefaults.Indicator(state = state, isRefreshing = isLoading) // Corrected to Indicator
        }
    ) { // Content of the PullToRefreshBox
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(medications) { medication ->
                MedicationCard(
                    medication = medication,
                    onClick = { onItemClick(medication) }
                )
            }
        }
    }
}
