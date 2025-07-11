package com.d4viddf.medicationreminder.wear.tile

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import com.d4viddf.medicationreminder.wear.presentation.WearActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
// Jetpack Compose imports for Previews
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column as ComposeColumn
import androidx.compose.foundation.layout.Spacer as ComposeSpacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight as ComposeFontWeight
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.wear.compose.material.Text as ComposeText

private const val RESOURCES_VERSION = "1"
private const val TILE_ID = "medication_tile"

// --- Placeholder Data and Helpers ---
data class MedicationReminder(
    val id: String,
    val name: String,
    val dosage: String,
    val time: Long,
    val isTaken: Boolean = false
)

fun getTimestamp(hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun formatTime(timeInMillis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(timeInMillis)
}

private val tileSampleReminders = listOf(
    MedicationReminder("1", "Mestinon", "1 tablet", getTimestamp(9, 0)),
    MedicationReminder("2", "Valsartan", "1 tablet", getTimestamp(9, 0)),
    MedicationReminder("3", "Mestinon", "1 tablet", getTimestamp(15, 0), isTaken = true)
)

class MedicationTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val now = System.currentTimeMillis()
        val nextReminder = tileSampleReminders
            .filter { it.time >= now && !it.isTaken }
            .minByOrNull { it.time }

        return Futures.immediateFuture(
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .build()
        )
    }

    private fun tileLayout(
        context: Context,
        nextReminder: MedicationReminder?,
        deviceParameters: DeviceParametersBuilders.DeviceParameters
    ): LayoutElementBuilders.LayoutElement {

        val launchAppClickable = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setClassName(WearActivity::class.java.name)
                    .setPackageName(context.packageName)
                    .build()
            ).build()

        if (nextReminder == null) {
            return PrimaryLayout.Builder(deviceParameters)
                .setContent(
                    Text.Builder(context, "No upcoming doses.")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(Color.White.toArgb()))
                        .build()
                )
                .setPrimaryChipContent(
                    CompactChip.Builder(
                        context,
                        "Open App",
                        ModifiersBuilders.Clickable.Builder().setOnClick(launchAppClickable).build(),
                        deviceParameters
                    ).build()
                )
                .build()
        }

        return PrimaryLayout.Builder(deviceParameters)
            .setPrimaryLabelTextContent(
                Text.Builder(context, "Next: ${formatTime(nextReminder.time)}")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(ColorBuilders.argb(Color(0xFFB0BEC5).toArgb()))
                    .build()
            )
            .setContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(
                        Text.Builder(context, nextReminder.name)
                            .setTypography(Typography.TYPOGRAPHY_TITLE3)
                            .setColor(ColorBuilders.argb(Color.White.toArgb()))
                            .build()
                    )
                    .addContent(
                        Text.Builder(context, nextReminder.dosage)
                            .setTypography(Typography.TYPOGRAPHY_BODY1)
                            .setColor(ColorBuilders.argb(Color(0xFFCFD8DC).toArgb()))
                            .build()
                    )
                    .build()
            )
            .setPrimaryChipContent(
                CompactChip.Builder(
                    context,
                    "Open App",
                    ModifiersBuilders.Clickable.Builder().setOnClick(launchAppClickable).build(),
                    deviceParameters
                ).build()
            )
            .build()
    }
}


// --- Previews for Tile ---
@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun TilePreview() {
    val nextReminder = MedicationReminder("1", "Mestinon", "1 tablet", getTimestamp(9, 0))

    MaterialTheme {
        ComposeColumn(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ComposeText("Next: ${formatTime(nextReminder.time)}", fontSize = 14.sp, color = Color.LightGray)
            ComposeSpacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
            ComposeText(nextReminder.name, fontSize = 20.sp, fontWeight = ComposeFontWeight.Bold, color = Color.White)
            ComposeText(nextReminder.dosage, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Preview(
    device = WearDevices.SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun NoDoseTilePreview() {
    MaterialTheme {
        ComposeColumn(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ComposeText("No upcoming doses.", fontSize = 14.sp, color = Color.White)
        }
    }
}
