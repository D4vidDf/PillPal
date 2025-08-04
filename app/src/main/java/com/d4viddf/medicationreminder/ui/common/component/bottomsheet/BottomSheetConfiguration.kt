package com.d4viddf.medicationreminder.ui.common.component.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R

/**
 * Represents a single clickable item in the bottom sheet.
 * A unique ID has been added to solve the crash.
 */
data class BottomSheetItem(
    val id: String, // Unique ID for the key
    val icon: Painter,
    val text: String,
    val action: () -> Unit
)

/**
 * Represents a group of items in the bottom sheet, with a title.
 */
data class BottomSheetGroup(
    val title: String,
    val items: List<BottomSheetItem>
)

/**
 * Provides the configuration for the bottom sheet. This function defines
 * the content and actions for each item.
 */
@Composable
fun getBottomSheetData(navController: NavController): List<BottomSheetGroup> {
    return listOf(
        BottomSheetGroup(
            title = stringResource(R.string.bottom_sheet_add_reminders_title),
            items = listOf(
                BottomSheetItem(
                    id = "add_medication",
                    icon = painterResource(R.drawable.prescriptions),
                    text = stringResource(R.string.bottom_sheet_add_medication),
                    action = { navController.navigate("addMedication") }
                ),
                BottomSheetItem(
                    id = "add_water_reminder",
                    icon = painterResource(R.drawable.water_full),
                    text = stringResource(R.string.bottom_sheet_add_water_reminder),
                    action = { /* TODO: Implement navigation */ }
                )
            )
        ),
        BottomSheetGroup(
            title = stringResource(R.string.bottom_sheet_register_manually_title),
            items = listOf(
                BottomSheetItem(
                    id = "log_medication",
                    icon = painterResource(R.drawable.medication_filled),
                    text = stringResource(R.string.bottom_sheet_log_medication),
                    action = { /* TODO: Implement navigation */ }
                ),
                BottomSheetItem(
                    id = "log_water",
                    icon = painterResource(R.drawable.water_full),
                    text = stringResource(R.string.bottom_sheet_log_water),
                    action = { /* TODO: Implement navigation */ }
                ),
                BottomSheetItem(
                    id = "log_weight",
                    icon = painterResource(R.drawable.monitor_weight),
                    text = stringResource(R.string.bottom_sheet_log_weight),
                    action = { /* TODO: Implement navigation */ }
                )
            )
        )
    )
}