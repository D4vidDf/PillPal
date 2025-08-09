package com.d4viddf.medicationreminder.ui.features.home.components.skeletons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.ui.common.utill.shimmerLoadingAnimation
import com.d4viddf.medicationreminder.ui.theme.AppTheme

@Composable
fun NextDoseCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Gray.copy(alpha = 0.3f))
            .padding(16.dp)
            .shimmerLoadingAnimation(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Gray.copy(alpha = 0.5f)),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.5f))
            )

            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NextDoseCardSkeletonPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 180.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            NextDoseCardSkeleton(modifier = Modifier.fillMaxSize())
        }
    }
}
