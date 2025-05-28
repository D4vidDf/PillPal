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
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.Medication

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class) // ExperimentalMaterial3ExpressiveApi is not needed for PullToRefreshBox
@Composable
fun MedicationList(
    medications: List<Medication>,
    onItemClick: (Medication) -> Unit,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier, // This modifier is passed from HomeScreen
    externalNestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection? = null
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val finalModifier = if (externalNestedScrollConnection != null) {
        modifier.nestedScroll(externalNestedScrollConnection)
    } else {
        modifier
    }

    PullToRefreshBox(
        modifier = finalModifier, // Apply the correctly composed modifier
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
                modifier = Modifier.fillMaxSize()
                // The LazyColumn is the scrollable content. It will be scrollable
                // even when isRefreshing is true and the indicator is visible.
                // The indicator is drawn in a separate layer or space managed by PullToRefreshBox.
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