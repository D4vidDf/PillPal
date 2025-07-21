package com.d4viddf.medicationreminder.wear.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import com.d4viddf.medicationreminder.wear.R
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun PhoneAppNotInstalledScreen(onOpenPlayStore: () -> Unit, onRetry: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        contentPadding = rememberResponsiveColumnPadding(
            first = ColumnItemType.ListHeader,
            last = ColumnItemType.IconButton
        ),
        edgeButton = {
            EdgeButton(
                onClick = onRetry,
                buttonSize = EdgeButtonSize.Medium
            ) {
                Text(stringResource(R.string.retry_sync), textAlign = TextAlign.Center)
            }
        },
        timeText = { TimeText() }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
        ) {
            item(key = "app_not_installed_title") {
                ListHeader(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(
                        text = stringResource(R.string.phone_app_not_installed_title)
                    )
                }
            }
            item(key = "app_not_installed_body") {
                Text(
                    text = stringResource(R.string.phone_app_not_installed_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
            item(key = "open_play_store_button") {
                Button(
                    onClick = onOpenPlayStore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.open_play_store))
                }
            }
        }
    }
}
