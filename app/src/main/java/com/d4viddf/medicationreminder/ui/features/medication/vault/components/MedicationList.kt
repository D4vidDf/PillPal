package com.d4viddf.medicationreminder.ui.features.medication.vault.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.Medication
import com.d4viddf.medicationreminder.ui.common.model.UiItemState
import com.d4viddf.medicationreminder.ui.features.medication.vault.components.skeletons.MedicationCardSkeleton
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun MedicationList(
    medicationState: List<UiItemState<Medication>>,
    isRefreshing: Boolean,
    onItemClick: (medication: Medication, index: Int) -> Unit,
    onRefresh: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp,
    listState: LazyListState,
    enableCardTransitions: Boolean
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullToRefreshState,
        onRefresh = onRefresh,
        isRefreshing = isRefreshing,
        contentAlignment = Alignment.TopCenter,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
            )
        }
    ) {
        if (medicationState.isEmpty() && !isRefreshing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(id = R.string.no_medications_yet))
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomContentPadding)
            ) {
                itemsIndexed(medicationState, key = { index, itemState ->
                    when (itemState) {
                        is UiItemState.Success -> itemState.data.id
                        else -> index // Use index as key for skeletons/errors
                    }
                }) { index, itemState ->
                    Crossfade(targetState = itemState, label = "MedicationItemCrossfade") { state ->
                        when (state) {
                            is UiItemState.Loading -> {
                                MedicationCardSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            is UiItemState.Success -> {
                                MedicationCard(
                                    medication = state.data,
                                    onClick = { onItemClick(state.data, index) },
                                    enableTransition = enableCardTransitions,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                            is UiItemState.Error -> {
                                // Optional: Show an error item
                                Text(
                                    "Error loading item.",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(86.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO, apiLevel = 33)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, apiLevel = 33)
@Composable
fun MedicationListPreview() {
    AppTheme(dynamicColor = false) {
        val todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val sampleMedications = listOf(
            UiItemState.Success(Medication(1, "Amoxicillin", "250mg", "LIGHT_BLUE", "10:00 AM", 1, 0, 0, todayDate, todayDate)),
            UiItemState.Loading,
            UiItemState.Success(Medication(3, "Vitamin C", "500mg", "LIGHT_ORANGE", "08:00 AM", 1, 0, 0, todayDate, todayDate))
        )
        MedicationList(
            medicationState = sampleMedications,
            isRefreshing = false,
            onItemClick = { _, _ -> },
            onRefresh = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            bottomContentPadding = 0.dp,
            listState = rememberLazyListState(),
            enableCardTransitions = true
        )
    }
}
