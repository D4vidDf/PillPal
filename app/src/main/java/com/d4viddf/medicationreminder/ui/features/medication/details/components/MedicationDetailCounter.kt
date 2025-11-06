package com.d4viddf.medicationreminder.ui.features.medication.details.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.common.components.AutoSizeText
import com.d4viddf.medicationreminder.ui.features.medication.add.CounterInfo
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationColor
import com.d4viddf.medicationreminder.utils.NumberUtils

@Composable
fun CounterItem(value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        AutoSizeText(
            text = value,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            color = valueColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
fun StatusCounter(
    iconResId: Int,
    labelResId: Int,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = stringResource(id = labelResId),
            tint = valueColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = labelResId),
            fontSize = 14.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WeeklyScheduleCounter(
    days: List<Boolean>,
    labelResId: Int,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            days.forEach { isActive ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isActive) valueColor else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = labelResId),
            fontSize = 14.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MedicationDetailCounters(
    colorScheme: MedicationColor,
    counters: List<CounterInfo>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = colorScheme.cardColor,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (counters.isEmpty()) {
            Text(
                stringResource(id = R.string.medication_detail_counter_no_details),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        } else {
            counters.forEachIndexed { index, counterInfo ->
                val itemModifier = Modifier.weight(1f)
                when (counterInfo) {
                    is CounterInfo.Dose -> {
                        val numericValue = counterInfo.value.toFloatOrNull()
                        if (numericValue != null) {
                            CounterItem(
                                value = NumberUtils.toFraction(numericValue),
                                label = counterInfo.unit,
                                valueColor = colorScheme.onBackgroundColor,
                                modifier = itemModifier
                            )
                        }
                    }
                    is CounterInfo.RemainingDoses -> CounterItem(
                        value = counterInfo.value,
                        label = stringResource(id = counterInfo.labelResId),
                        valueColor = colorScheme.onBackgroundColor,
                        modifier = itemModifier
                    )
                    is CounterInfo.Frequency -> CounterItem(
                        value = counterInfo.value,
                        label = counterInfo.unit,
                        valueColor = colorScheme.onBackgroundColor,
                        modifier = itemModifier
                    )
                    is CounterInfo.Weekly -> WeeklyScheduleCounter(
                        days = counterInfo.days,
                        labelResId = counterInfo.labelResId,
                        valueColor = colorScheme.onBackgroundColor,
                        modifier = itemModifier
                    )
                    is CounterInfo.Duration -> CounterItem(
                        value = counterInfo.value,
                        label = counterInfo.unit,
                        valueColor = colorScheme.onBackgroundColor,
                        modifier = itemModifier
                    )
                    is CounterInfo.Status -> StatusCounter(
                        iconResId = counterInfo.iconResId,
                        labelResId = counterInfo.labelResId,
                        valueColor = colorScheme.onBackgroundColor,
                        modifier = itemModifier
                    )
                }
                if (index < counters.size - 1) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = colorScheme.onBackgroundColor.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
