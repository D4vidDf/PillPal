package com.d4viddf.medreminder.wear.tile

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
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

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): com.google.common.util.concurrent.ListenableFuture<Tile> {
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
                            tileLayout(this, nextReminder)
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

    private fun tileLayout(context: Context, nextReminder: MedicationReminder?): LayoutElementBuilders.LayoutElement {
        val launchAppClickable = Clickable.Builder()
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
            return PrimaryLayout.Builder(this)
                .setContent(
                    Text.Builder(this)
                        .setText("No upcoming doses.")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(argb(Color.White.hashCode()))
                        .build()
                )
                .setPrimaryChipContent(
                     Text.Builder(this)
                        .setText("Open App")
                         .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(argb(Color.Black.hashCode()))
                        .build()
                )
                .setPrimaryChipClickable(launchAppClickable)
                .build()
        }

        return PrimaryLayout.Builder(this)
            .setPrimaryLabelTextContent(
                Text.Builder(this)
                    .setText("Next Dose: ${formatTime(nextReminder.time)}")
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(argb(Color(0xFFB0BEC5).hashCode())) // Light Blue Grey
                    .build()
            )
            .setContent(
                Column.Builder()
                    .setModifiers(
                        LayoutElementBuilders.Modifiers.Builder()
                            .setPadding(
                                Padding.Builder()
                                    .setAll(DimensionBuilders.dp(8f))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(
                        Text.Builder(this)
                            .setText(nextReminder.name)
                            .setTypography(Typography.TYPOGRAPHY_TITLE3)
                            .setColor(argb(Color.White.hashCode()))
                            .build()
                    )
                    .addContent(
                        Text.Builder(this)
                            .setText(nextReminder.dosage)
                            .setTypography(Typography.TYPOGRAPHY_BODY1)
                            .setColor(argb(Color(0xFFCFD8DC).hashCode())) // Another shade of Blue Grey
                            .build()
                    )
                    .build()
            )
             .setPrimaryChipContent(
                     Text.Builder(this)
                        .setText("Open App")
                         .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(argb(Color.Black.hashCode()))
                        .build()
                )
            .setPrimaryChipClickable(launchAppClickable)
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
    // This is a simplified representation for Compose Preview
    // Actual tile rendering uses ProtoLayout, not Jetpack Compose directly for the Tile itself
    val context = androidx.compose.ui.platform.LocalContext.current
    val nextReminder = MedicationReminder("1", "Mestinon", "1 tablet", getTimestamp(9, 0))

    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        androidx.compose.material.Text("Next Dose: ${formatTime(nextReminder.time)}", fontSize = 14.sp, color = Color.LightGray)
        Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
        androidx.compose.material.Text(nextReminder.name, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White)
        androidx.compose.material.Text(nextReminder.dosage, fontSize = 14.sp, color = Color.Gray)
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
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        androidx.compose.material.Text("No upcoming doses.", fontSize = 14.sp, color = Color.White)
    }
}
