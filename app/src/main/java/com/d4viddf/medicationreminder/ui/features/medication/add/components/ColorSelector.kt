package com.d4viddf.medicationreminder.ui.features.medication.add.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.theme.medicationColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSelector(
    selectedColor: MedicationColor?,
    onColorSelected: (MedicationColor) -> Unit,
    showBottomSheet: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedColorName = selectedColor?.let { stringResource(id = it.colorNameResId) } ?: stringResource(id = R.string.color_orange)
    val selectedColorAccText = stringResource(id = R.string.color_selector_selected_color_acc, selectedColorName)
    val expandAccText = stringResource(id = R.string.color_selector_expand_acc)
    val colorSelectorTitleText = stringResource(id = R.string.color_selector_title)

    // This Row is just for display purposes inside the card.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.label_color), modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(selectedColor?.backgroundColor ?: MedicationColor.ORANGE.backgroundColor, CircleShape)
                    .semantics {
                        contentDescription = selectedColorAccText
                    }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(selectedColorName)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = expandAccText)
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(colorSelectorTitleText, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(medicationColors.size) { index ->
                        val color = medicationColors[index]
                        val isSelected = color == selectedColor
                        val itemColorName = stringResource(id = color.colorNameResId)
                        val itemColorAccText = stringResource(id = R.string.color_selector_selected_color_acc, itemColorName)
                        val itemSelectedAccText = stringResource(id = R.string.color_selector_selected_acc)

                        val cornerShape = when (index) {
                            0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                            2 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 16.dp, bottomEnd = 8.dp)
                            9 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 16.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                            11 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 16.dp)
                            else -> RoundedCornerShape(8.dp)
                        }
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .background(color.backgroundColor, cornerShape)
                                .clickable {
                                    onColorSelected(color)
                                    onDismiss()
                                }
                                .semantics {
                                    contentDescription = itemColorAccText
                                },
                            contentAlignment = Alignment.Center,

                            ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = itemSelectedAccText,
                                    modifier = Modifier
                                        .background(Color.White, CircleShape)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Light Mode", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ColorSelectorPreview() {
    AppTheme(dynamicColor = false) {
        var selectedColor by remember { mutableStateOf<MedicationColor?>(medicationColors[0]) }
        var showSheet by remember { mutableStateOf(false) }

        Column {
            Button(onClick = { showSheet = true }) {
                Text("Show Color Selector")
            }
            ColorSelector(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it },
                showBottomSheet = showSheet,
                onDismiss = { showSheet = false }
            )
        }
    }
}