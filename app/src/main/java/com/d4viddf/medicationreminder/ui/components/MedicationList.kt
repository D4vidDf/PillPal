package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    externalNestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection? = null,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp // New parameter for bottom padding
) {
    val pullToRefreshState = rememberPullToRefreshState()

    // The modifier from HomeScreen (which includes padding for Scaffold's TopAppBar and potentially other things)
    // should be applied to the PullToRefreshBox.
    // The nestedScroll connection is also applied here.
    var pullToRefreshModifier = modifier 
    if (externalNestedScrollConnection != null) {
        pullToRefreshModifier = pullToRefreshModifier.nestedScroll(externalNestedScrollConnection)
    }

    PullToRefreshBox(
        modifier = pullToRefreshModifier, // Apply the combined modifier here
        state = pullToRefreshState,
        onRefresh = onRefresh,
        isRefreshing = isLoading,
        contentAlignment = Alignment.TopCenter, // Default, but good to be explicit
        indicator = {
            PullToRefreshDefaults.Indicator( // Changed to new M3 indicator
                state = pullToRefreshState,
                isRefreshing = isLoading,
                // Modifier.align(Alignment.TopCenter) is default for Indicator
            )
        }
    ) { // Content of PullToRefreshBox
        if (medications.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(), // Fills the content area of PullToRefreshBox
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(id = R.string.no_medications_yet))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), // LazyColumn fills the PullToRefreshBox's content area
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