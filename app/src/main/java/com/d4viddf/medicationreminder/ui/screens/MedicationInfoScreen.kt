package com.d4viddf.medicationreminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Subject // For Prospecto
import androidx.compose.material.icons.filled.DocumentScanner // For Ficha Técnica
import androidx.compose.material.icons.filled.Image // Placeholder for medication image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
// Removed duplicate Button and ButtonDefaults imports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // Added import for TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.d4viddf.medicationreminder.R // Moved import to top
import com.d4viddf.medicationreminder.data.CimaMedicationDetail // Changed to use CimaMedicationDetail
import com.d4viddf.medicationreminder.data.CimaFormaFarmaceutica // For preview
import com.d4viddf.medicationreminder.data.CimaViaAdministracion // For preview
import com.d4viddf.medicationreminder.data.CimaEstado // For preview
import com.d4viddf.medicationreminder.data.CimaDocumento // For preview
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme exists
import com.d4viddf.medicationreminder.viewmodel.MedicationInfoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Removed MedicationFullInfo data class as we now use CimaMedicationDetail from ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationInfoScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit,
    viewModel: MedicationInfoViewModel? = hiltViewModel() // Made nullable for preview
) {
    val cimaMedicationDetail by viewModel?.medicationInfo?.collectAsState() ?: remember { mutableStateOf(null) }
    val isLoading by viewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val error by viewModel?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

    // Sample data for preview when viewModel is null
    val sampleInfoForPreview = if (viewModel == null) {
        remember {
            CimaMedicationDetail(
                nregistro = "12345",
                nombre = "Medicamento Ejemplo (Preview)",
                labtitular = "Laboratorios Ficticios S.A.",
                pactivos = "Principio Activo Ejemplo, Otro Principio",
                formaFarmaceutica = CimaFormaFarmaceutica(id = 1, nombre = "Comprimido"),
                viasAdministracion = listOf(CimaViaAdministracion(id = 1, nombre = "Oral")),
                condPresc = "Con receta médica",
                estado = CimaEstado(aut = System.currentTimeMillis()),
                docs = listOf(
                    CimaDocumento(tipo = 1, urlHtml = "https://example.com/ficha", url = "https://example.com/ficha.pdf"),
                    CimaDocumento(tipo = 2, urlHtml = "https://example.com/prospecto", url = "https://example.com/prospecto.pdf")
                ),
                fotos = null,
                comerc = true,
                conduc = false,
                triangulo = true,
                huerfano = false,
                biosimilar = false
            )
        }
    } else {
        cimaMedicationDetail
    }


    LaunchedEffect(medicationId, viewModel) {
        viewModel?.loadMedicationInfo(medicationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sampleInfoForPreview?.nombre ?: stringResource(id = R.string.loading)) }, // Changed to R.string.loading
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.med_info_navigate_back_cd)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                }
            }
            sampleInfoForPreview != null -> {
                val info = sampleInfoForPreview
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 8.dp)
                ) {
                    // Image Display
                    item {
                        val imageUrl = if (!info.nregistro.isNullOrBlank()) {
                            "https://cima.aemps.es/cima/rest/medicamento/${info.nregistro}/foto/materialAcondicionamientoPrimario"
                        } else {
                            null // Or a default placeholder URL if you have one
                        }
                        AsyncImage(
                            model = imageUrl,
                            placeholder = painterResource(id = R.drawable.ic_pill_placeholder), // Replace with your placeholder
                            error = painterResource(id = R.drawable.ic_pill_placeholder),       // Replace with your error drawable
                            contentDescription = stringResource(id = R.string.med_info_image_placeholder_cd),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                ),
                            alignment = Alignment.Center
                        )
                    }

                    // Information Fields
                    item { InfoSectionCard(info) } // Pass CimaMedicationDetail

                    // Links
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        val uriHandler = LocalUriHandler.current
                        val prospecto = info.docs?.find { it.tipo == 2 } // Prospecto
                        val fichaTecnica = info.docs?.find { it.tipo == 1 } // Ficha técnica

                        LinkButton(
                            text = stringResource(id = R.string.med_info_button_prospecto),
                            url = prospecto?.urlHtml ?: prospecto?.url,
                            icon = Icons.AutoMirrored.Filled.Subject,
                            onClick = { url -> uriHandler.openUri(url) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinkButton(
                            text = stringResource(id = R.string.med_info_button_ficha_tecnica),
                            url = fichaTecnica?.urlHtml ?: fichaTecnica?.url,
                            icon = Icons.Filled.DocumentScanner,
                            onClick = { url -> uriHandler.openUri(url) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            else -> { // Fallback for when info is null and not loading and no error
                 Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.med_info_not_available_generic))
                }
            }
        }
    }
}

@Composable
private fun InfoSectionCard(info: CimaMedicationDetail) { // Changed parameter type
    val yesText = stringResource(id = R.string.text_yes)
    val noText = stringResource(id = R.string.text_no)

    // Helper to format timestamp
    fun formatDate(timestamp: Long?): String? {
        if (timestamp == null || timestamp == -1L) return null
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp * 1000)) // Assuming timestamp is in seconds
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(label = stringResource(id = R.string.med_info_label_nombre_comercial), value = info.nombre)
            InfoRow(label = stringResource(id = R.string.med_info_label_principio_activo), value = info.pactivos)
            InfoRow(label = stringResource(id = R.string.med_info_label_forma_farmaceutica), value = info.formaFarmaceutica?.nombre)
            InfoRow(label = stringResource(id = R.string.med_info_label_vias_administracion), value = info.viasAdministracion?.joinToString { it.nombre ?: "" })
            InfoRow(label = stringResource(id = R.string.med_info_label_laboratorio_titular), value = info.labtitular)
            InfoRow(label = stringResource(id = R.string.med_info_label_estado_autorizacion_date), value = formatDate(info.estado?.aut))
            InfoRow(label = stringResource(id = R.string.med_info_label_condiciones_prescripcion), value = info.condPresc, showDivider = false)
        }
    }
    // Card for boolean flags
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(label = stringResource(id = R.string.med_info_label_comercializado), value = info.comerc?.let { if (it) yesText else noText })
            InfoRow(label = stringResource(id = R.string.med_info_label_conduccion), value = info.conduc?.let { if (it) yesText else noText })
            InfoRow(label = stringResource(id = R.string.med_info_label_triangulo_negro), value = info.triangulo?.let { if (it) yesText else noText })
            InfoRow(label = stringResource(id = R.string.med_info_label_huerfano), value = info.huerfano?.let { if (it) yesText else noText })
            InfoRow(label = stringResource(id = R.string.med_info_label_biosimilar), value = info.biosimilar?.let { if (it) yesText else noText }, showDivider = false)
        }
     }
}


@Composable
private fun InfoRow(label: String, value: String?, showDivider: Boolean = true) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge, // More prominent label
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value ?: stringResource(id = R.string.med_info_value_not_available),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp) // Slight indent for value
        )
    }
    if (showDivider) {
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun LinkButton(
    text: String,
    url: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (String) -> Unit // Pass URL to click handler
) {
    Button(
        onClick = { url?.let(onClick) }, // Call onClick only if URL is not null
        enabled = !url.isNullOrBlank(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text)
    }
}

@Preview(showBackground = true, name = "Medication Info Screen")
@Composable
fun MedicationInfoScreenPreview() {
    AppTheme {
        MedicationInfoScreen(
            medicationId = 1, // Sample ID for preview
            onNavigateBack = {},
            viewModel = null // Ensures preview uses sample data
        )
    }
}

// Add a new string resource for generic "not available" if needed, e.g., for when info itself is null
// <string name="med_info_not_available_generic">Medication information not available.</string>
// And for Yes/No
// <string name="text_yes">Yes</string>
// <string name="text_no">No</string>
// <string name="med_info_label_comercializado">Comercializado</string>
// <string name="med_info_label_conduccion">Afecta Conducción</string>
// <string name="med_info_label_triangulo_negro">Triángulo Negro</string>
// <string name="med_info_label_huerfano">Medicamento Huérfano</string>
// <string name="med_info_label_biosimilar">Biosimilar</string>
// <string name="med_info_label_estado_autorizacion_date">Fecha Autorización</string>
