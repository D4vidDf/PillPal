package com.d4viddf.medicationreminder.ui.features.todayschedules.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onConfirm: (String?) -> Unit
) {
    val colors = listOf<String?>(null) + MedicationColor.values().map { it.name }
    var tempSelectedColorName by remember { mutableStateOf(selectedColorName) }

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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(colors) { colorName ->
                    ColorItem(
                        colorName = colorName,
                        isSelected = colorName == tempSelectedColorName,
                        onClick = { tempSelectedColorName = colorName }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onConfirm(tempSelectedColorName) },
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

    val medicationColor = colorName?.let { MedicationColor.valueOf(it) }
    val color = medicationColor?.backgroundColor ?: MaterialTheme.colorScheme.surfaceVariant
    val contentColor = medicationColor?.onBackgroundColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    val tickColor = medicationColor?.textColor ?: contentColor

    val context = androidx.compose.ui.platform.LocalContext.current
    val colorResId = if (colorName != null) {
        val resId = context.resources.getIdentifier("color_${colorName.lowercase()}", "string", context.packageName)
        if (resId != 0) resId else R.string.color_no_color
    } else {
        R.string.color_no_color
    }

    val borderModifier = if (isSelected) {
        Modifier.border(BorderStroke(4.dp, MaterialTheme.colorScheme.primary), shape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .then(borderModifier)
            .clip(shape)
            .background(color)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = context.getString(colorResId)
            },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = tickColor
            )
        } else if (colorName == null) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null, // Description is on the box
                tint = contentColor
            )
        }
    }
}