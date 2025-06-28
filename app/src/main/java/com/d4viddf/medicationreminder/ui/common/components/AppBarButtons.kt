package com.d4viddf.medicationreminder.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R

@Composable
fun ThemedAppBarBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp) // Consistent with MedicationDetailScreen's back button
            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.rounded_arrow_back_ios_24),
            contentDescription = stringResource(id = R.string.back_button_content_description), // Use a general back description
            modifier = Modifier.size(28.dp),
            tint = Color.White
        )
    }
}
