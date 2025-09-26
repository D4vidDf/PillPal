package com.d4viddf.medicationreminder.ui.features.todayschedules.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.MedicationColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorFilterBottomSheet(
    selectedColorName: String?,
    onDismiss: () -> Unit,
    onColorSelected: (String?) -> Unit
) {
    val colors = listOf<String?>(null) + MedicationColor.values().map { it.name }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.filter_by_color),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(colors) { colorName ->
                    ColorItem(
                        colorName = colorName,
                        isSelected = colorName == selectedColorName,
                        onClick = { onColorSelected(colorName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.confirm))
            }
        }
    }
}

@Composable
private fun ColorItem(
    colorName: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cornerRadius by animateDpAsState(targetValue = if (isSelected) 50.dp else 16.dp, label = "cornerRadius")
    val shape = RoundedCornerShape(cornerRadius)

    val color = colorName?.let { MedicationColor.valueOf(it).backgroundColor } ?: MaterialTheme.colorScheme.surfaceVariant
    val contentColor = colorName?.let { MedicationColor.valueOf(it).onBackgroundColor } ?: MaterialTheme.colorScheme.onSurfaceVariant
    val context = androidx.compose.ui.platform.LocalContext.current
    val colorResId = if (colorName != null) {
        val resId = context.resources.getIdentifier("color_${colorName.lowercase()}", "string", context.packageName)
        if (resId != 0) resId else R.string.color_no_color
    } else {
        R.string.color_no_color
    }


    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(color)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = context.getString(colorResId)
            },
        contentAlignment = Alignment.Center
    ) {
        if (colorName == null) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null, // Description is on the box
                tint = contentColor
            )
        }
    }
}