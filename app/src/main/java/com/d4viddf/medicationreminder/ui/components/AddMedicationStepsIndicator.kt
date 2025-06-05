package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.medicationreminder.R // Assuming R class is in this package
import com.d4viddf.medicationreminder.ui.theme.AppTheme

data class StepDetails(
    val number: Int,
    val title: String,
    val iconResId: Int? = null // Optional icon for each step
)

@Composable
fun AddMedicationStepsIndicator(
    currentStep: Int, // 0-indexed
    totalSteps: Int,
    stepDetails: List<StepDetails>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight()) {
        stepDetails.forEachIndexed { index, detail ->
            val isCurrent = index == currentStep
            val isCompleted = index < currentStep

            StepItem(
                stepNumber = detail.number,
                title = detail.title,
                iconResId = detail.iconResId,
                isCurrent = isCurrent,
                isCompleted = isCompleted,
                isLastStep = index == totalSteps - 1
            )
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: Int,
    title: String,
    iconResId: Int?,
    isCurrent: Boolean,
    isCompleted: Boolean,
    isLastStep: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.wrapContentWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isCompleted -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (iconResId != null && isCompleted) {
                    Icon(
                        painterResource(id = R.drawable.ic_check), // Generic check icon for completed
                        contentDescription = stringResource(R.string.step_completed), // TODO: Add string
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                } else if (iconResId != null && isCurrent) {
                     Icon(
                        painterResource(id = iconResId),
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimary,
                         modifier = Modifier.size(18.dp)
                    )
                }
                else {
                    Text(
                        text = "$stepNumber",
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.onPrimary
                            isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!isLastStep) {
                Divider(
                    modifier = Modifier
                        .height(24.dp)
                        .width(2.dp),
                    color = if (isCompleted || isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isCurrent || isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddMedicationStepsIndicatorPreview() {
    val steps = listOf(
        StepDetails(1, "Medication Name", R.drawable.ic_pill_placeholder), // Assuming ic_pill_placeholder exists
        StepDetails(2, "Type & Color", R.drawable.ic_search), // Assuming ic_search exists
        StepDetails(3, "Dosage & Package"),
        StepDetails(4, "Frequency"),
        StepDetails(5, "Summary")
    )
    AppTheme {
        AddMedicationStepsIndicator(currentStep = 0, totalSteps = 5, stepDetails = steps, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun AddMedicationStepsIndicatorPreviewStep3() {
     val steps = listOf(
        StepDetails(1, "Medication Name", R.drawable.ic_pill_placeholder),
        StepDetails(2, "Type & Color", R.drawable.ic_search),
        StepDetails(3, "Dosage & Package"),
        StepDetails(4, "Frequency"),
        StepDetails(5, "Summary")
    )
    AppTheme {
        AddMedicationStepsIndicator(currentStep = 2, totalSteps = 5, stepDetails = steps, modifier = Modifier.padding(16.dp))
    }
}
