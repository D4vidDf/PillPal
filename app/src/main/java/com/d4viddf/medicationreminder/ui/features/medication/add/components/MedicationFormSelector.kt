package com.d4viddf.medicationreminder.ui.features.medication.add.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.MedicationForm
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MedicationFormSelector(
    modifier: Modifier = Modifier,
    selectedForm: MedicationForm?,
    onFormSelected: (MedicationForm) -> Unit,
    selectedColor: MedicationColor? = null
) {
    val windowSizeClass = calculateWindowSizeClass()
    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 3
        else -> 5
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.medication_type_selector_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            items(MedicationForm.values().size) { index ->
                val form = MedicationForm.values()[index]
                val cornerShape = when {
                    columns == 5 -> {
                        when (index) {
                            0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                            4 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 16.dp, bottomEnd = 8.dp)
                            5 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 16.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                            9 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 16.dp)
                            else -> RoundedCornerShape(8.dp)
                        }
                    }
                    else -> {
                        when (index) {
                            0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                            2 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 16.dp, bottomEnd = 8.dp)
                            MedicationForm.values().size - 3 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 16.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                            MedicationForm.values().size - 1 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 8.dp, bottomEnd = 16.dp)
                            else -> RoundedCornerShape(8.dp)
                        }
                    }
                }
                MedicationFormItem(
                    form = form,
                    isSelected = form == selectedForm,
                    onClick = { onFormSelected(form) },
                    selectedColor = selectedColor,
                    cornerRadius = cornerShape
                )
            }
        }
    }
}

@Composable
fun MedicationFormItem(
    form: MedicationForm,
    isSelected: Boolean,
    selectedColor: MedicationColor?,
    cornerRadius: RoundedCornerShape,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) selectedColor?.backgroundColor ?: MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) selectedColor?.textColor ?: MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                color = backgroundColor,
                shape = cornerRadius
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = form.imageUrl),
            contentDescription = stringResource(id = form.nameResId),
            modifier = Modifier
                .fillMaxSize(0.6f)
                .aspectRatio(1f),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = form.nameResId),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MedicationFormSelectorPreview() {
    AppTheme {
        MedicationFormSelector(
            selectedForm = MedicationForm.TABLET,
            onFormSelected = {},
            selectedColor = MedicationColor.LIGHT_PINK
        )
    }
}
