package com.d4viddf.medicationreminder.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.d4viddf.medicationreminder.ui.colors.MedicationColor

@Composable
fun MedicationDetailHeader(
    medicationName: String?,
    medicationDosage: String?,
    medicationImageUrl: String?,
    colorScheme: MedicationColor,
    modifier: Modifier = Modifier
) {
    val displayName = remember(medicationName) {
        val words = medicationName?.split(" ")
        if (words != null && words.size > 3) {
            words.take(3).joinToString(" ")
        } else {
            medicationName ?: stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_header_loading)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(), // El modifier se aplica al Row principal
        verticalAlignment = Alignment.CenterVertically // Alineación vertical de los elementos del Row
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.textColor,
                lineHeight = 40.sp, // Ajustar según sea necesario
                maxLines = 2, // Permitir hasta 2 líneas para el nombre
                overflow = TextOverflow.Ellipsis // Añadir elipsis si el texto es muy largo
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = medicationDosage?.takeIf { it.isNotBlank() } ?: stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_header_no_dosage),
                fontSize = 20.sp,
                color = colorScheme.textColor
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = rememberAsyncImagePainter(model = medicationImageUrl ?: "https://placehold.co/100x100.png"),
            contentDescription = stringResource(id = com.d4viddf.medicationreminder.R.string.medication_detail_header_image_acc),
            modifier = Modifier.size(64.dp)
        )
    }
}