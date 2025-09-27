package com.d4viddf.medicationreminder.ui.features.home.components.skeletons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.ui.common.utill.shimmerLoadingAnimation
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import androidx.compose.material3.MaterialTheme

@Composable
fun SectionHeaderSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .height(24.dp)
                .width(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .shimmerLoadingAnimation()
        )
        Box(
            modifier = Modifier
                .height(20.dp)
                .width(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .shimmerLoadingAnimation()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderSkeletonPreview() {
    AppTheme {
        SectionHeaderSkeleton()
    }
}
