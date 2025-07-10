package com.d4viddf.medreminder.wear.tile

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Explicitly use androidx.wear.tiles.* for all tile related builders
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.DeviceParametersBuilders
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.material.Typography
import androidx.wear.tiles.material.layouts.PrimaryLayout
import androidx.wear.tiles.TileService
// Jetpack Compose imports for Previews
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text as ComposeText
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme as ComposeMaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column as ComposeColumn
import androidx.compose.foundation.layout.Spacer as ComposeSpacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight as ComposeFontWeight
import androidx.compose.ui.platform.LocalContext
import com.d4viddf.medreminder.wear.ui.MedicationReminder
import com.d4viddf.medreminder.wear.ui.WearActivity
import com.d4viddf.medreminder.wear.ui.formatTime
import com.d4viddf.medreminder.wear.ui.getTimestamp
import com.google.common.util.concurrent.Futures
import java.util.Calendar

private const val RESOURCES_VERSION = "1"
// Placeholder for actual data fetching logic
private val tileSampleReminders = listOf(
    MedicationReminder("1", "Mestinon", "1 tablet", getTimestamp(9, 0)),
    MedicationReminder("2", "Valsartan", "1 tablet", getTimestamp(9, 0)),
    MedicationReminder("3", "Mestinon", "1 tablet", getTimestamp(15, 0))
)

class MedicationTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): com.google.common.util.concurrent.ListenableFuture<androidx.wear.tiles.TileBuilders.Tile> {
        // For this example, we'll use the sample data.
        // In a real app, you would fetch this data from a repository or data source.
        val now = System.currentTimeMillis()
        val nextReminder = tileSampleReminders
            .filter { it.time >= now && !it.isTaken }
            .minByOrNull { it.time }

        val tile = Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(
                TimelineBuilders.Timeline.Builder().addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder().setLayout(
                        LayoutElementBuilders.Layout.Builder().setRoot(
                            tileLayout(this, nextReminder, requestParams) // Pass requestParams
                        ).build()
                    ).build()
                ).build()
            ).build()
        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): com.google.common.util.concurrent.ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
        )
    }

    private fun tileLayout(context: Context, nextReminder: MedicationReminder?, requestParams: RequestBuilders.TileRequest): TilesLayoutElementBuilders.LayoutElement {
        val deviceParameters = requestParams.deviceParameters
        val launchAppClickable = ModifiersBuilders.Clickable.Builder()
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setClassName(WearActivity::class.java.name)
                            .setPackageName(context.packageName)
                            .build()
                    )
                    .build()
            )
            .build()

        if (nextReminder == null) {
            return PrimaryLayout.Builder(deviceParameters)
                .setContent(
                    LayoutElementBuilders.Text.Builder(context, "No upcoming doses.")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(Color.White.hashCode()))
                        .build()
                )
                .setPrimaryChipContent(
                     LayoutElementBuilders.Text.Builder(context, "Open App")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(Color.Black.hashCode()))
                        .build()
                )
                .setClickable(launchAppClickable)
                .build()
        }

        return PrimaryLayout.Builder(deviceParameters)
            .setPrimaryLabelTextContent(
                LayoutElementBuilders.Text.Builder(context, "Next Dose: ${formatTime(nextReminder.time)}")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(ColorBuilders.argb(Color(0xFFB0BEC5).hashCode())) // Light Blue Grey
                    .build()
            )
            .setContent(
                LayoutElementBuilders.Column.Builder()
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setPadding(
                                ModifiersBuilders.Padding.Builder()
                                    .setAll(DimensionBuilders.dp(8f))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Text.Builder(context, nextReminder.name)
                            .setTypography(Typography.TYPOGRAPHY_TITLE3)
                            .setColor(ColorBuilders.argb(Color.White.hashCode()))
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Text.Builder(context, nextReminder.dosage)
                            .setTypography(Typography.TYPOGRAPHY_BODY1)
                            .setColor(ColorBuilders.argb(Color(0xFFCFD8DC).hashCode())) // Another shade of Blue Grey
                            .build()
                    )
                    .build()
            )
             .setPrimaryChipContent(
                  LayoutElementBuilders.Text.Builder(context, "Open App")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(ColorBuilders.argb(Color.Black.hashCode()))
                        .build()
                )
            .setClickable(launchAppClickable)
            .build()
    }
}

// --- Previews for Tile ---
@Preview(
    device = Devices.WEAR_OS_SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun TilePreview() {
    val context = LocalContext.current // Use LocalContext for preview
    val nextReminder = MedicationReminder("1", "Mestinon", "1 tablet", getTimestamp(9, 0))

    ComposeMaterialTheme { // Wrap with a Compose Theme
        ComposeColumn( // Use aliased ComposeColumn
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(Color.Black) // Set background for preview
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ComposeText("Next Dose: ${formatTime(nextReminder.time)}", fontSize = 14.sp, color = Color.LightGray)
            ComposeSpacer(modifier = androidx.compose.ui.Modifier.height(8.dp)) // Use aliased ComposeSpacer
            ComposeText(nextReminder.name, fontSize = 18.sp, fontWeight = ComposeFontWeight.Bold, color = Color.White)
            ComposeText(nextReminder.dosage, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Preview(
    device = Devices.WEAR_OS_SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun NoDoseTilePreview() {
    ComposeMaterialTheme { // Wrap with a Compose Theme
        ComposeColumn( // Use aliased ComposeColumn
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(Color.Black) // Set background for preview
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ComposeText("No upcoming doses.", fontSize = 14.sp, color = Color.White)
        }
    }
}
