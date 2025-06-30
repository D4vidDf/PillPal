package com.d4viddf.medicationreminder.ui.features.medicationvault.components // Updated package

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
// Import MedicationCard from its new potential location
import com.d4viddf.medicationreminder.ui.features.medicationvault.components.MedicationCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun MedicationList(
    medications: List<Medication>,
    onItemClick: (medication: Medication, index: Int) -> Unit,
    isLoading: Boolean,
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
        isRefreshing = isLoading,
        contentAlignment = Alignment.TopCenter,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isLoading,
            )
        }
    ) {
        if (medications.isEmpty() && !isLoading) {
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
                itemsIndexed(medications, key = { _, medication -> medication.id }) { index, medication ->
                    MedicationCard( // This will be imported from the same package
                        medication = medication,
                        onClick = { onItemClick(medication, index) },
                        enableTransition = enableCardTransitions,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
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
            onItemClick = { _, _ -> },
            isLoading = false,
            onRefresh = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
            bottomContentPadding = 0.dp,
            listState = rememberLazyListState(),
            enableCardTransitions = true
        )
    }
}
