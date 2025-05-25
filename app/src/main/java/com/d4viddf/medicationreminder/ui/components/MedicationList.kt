package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// Removed PullRefreshIndicator import
import androidx.compose.material.pullrefresh.pullRefresh // Kept for gesture
import androidx.compose.material.pullrefresh.rememberPullRefreshState // Kept for gesture
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi // Added
import androidx.compose.material3.expressive.LoadingIndicator // Added
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.d4viddf.medicationreminder.data.Medication

@Composable
fun MedicationList(
    medications: List<Medication>,
    onItemClick: (Medication) -> Unit,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullRefreshState(refreshing = isLoading, onRefresh = onRefresh)

    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(medications) { medication ->
                MedicationCard(
                    medication = medication,
                    onClick = { onItemClick(medication) }
                )
            }
        }

        if (isLoading) {
            LoadingIndicator(
                modifier = Modifier.align(Alignment.TopCenter)
                // No progress parameter for indeterminate
                // Color can be specified if needed, e.g., color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
