package com.d4viddf.medicationreminder.ui.features.home.components.cards

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.d4viddf.medicationreminder.R

@Composable
fun SectionHeader(
    title: String,
    onEditClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onEditClick) {
            Icon(
                painterResource(R.drawable.rounded_edit_24),
                contentDescription = stringResource(R.string.edit_section)
            )
        }
    }
}