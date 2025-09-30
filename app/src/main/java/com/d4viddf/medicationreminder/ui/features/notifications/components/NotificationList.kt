package com.d4viddf.medicationreminder.ui.features.notifications.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.data.model.Notification
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.rememberDismissState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.R

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotificationList(
    notifications: List<Notification>,
    onNotificationSwiped: (Notification) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(notifications, key = { it.id }) { notification ->
            val dismissState = rememberDismissState(
                confirmValueChange = {
                    if (it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart) {
                        onNotificationSwiped(notification)
                        true
                    } else {
                        false
                    }
                }
            )

            SwipeToDismiss(
                state = dismissState,
                directions = setOf(DismissDirection.EndToStart),
                background = {
                    val color by animateColorAsState(
                        targetValue = if (dismissState.targetValue == DismissValue.Default) Color.Transparent else MaterialTheme.colorScheme.errorContainer,
                        label = "background color"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f,
                        label = "icon scale"
                    )

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete),
                            modifier = Modifier.scale(scale)
                        )
                    }
                },
                dismissContent = {
                    NotificationItem(notification = notification)
                }
            )
            HorizontalDivider()
        }
    }
}