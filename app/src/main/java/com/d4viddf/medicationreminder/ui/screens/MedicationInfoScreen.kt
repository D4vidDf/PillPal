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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.hilt.navigation.compose.hiltViewModel // For later
import com.d4viddf.medicationreminder.ui.theme.AppTheme // Assuming AppTheme exists

// Placeholder data structure for medication info
data class MedicationFullInfo(
    val nombreComercial: String = "Medicamento Ejemplo",
    val principioActivo: String = "Principio Activo Ejemplo",
    val dosis: String = "10 mg",
    val formaFarmaceutica: String = "Comprimido",
    val viasDeAdministracion: List<String> = listOf("Oral"),
    val laboratorioTitular: String = "Laboratorios Ficticios S.A.",
    val estadoRegistro: String = "Autorizado",
    val condicionesPrescripcion: String = "Con receta médica",
    val prospectoUrl: String? = "https://example.com/prospecto",
    val fichaTecnicaUrl: String? = "https://example.com/ficha_tecnica",
    val imageUrl: String? = null // Placeholder for image
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationInfoScreen(
    medicationId: Int,
    onNavigateBack: () -> Unit
    // viewModel: MedicationInfoViewModel = hiltViewModel() // Placeholder
) {
    // In a real app, fetch this based on medicationId
    var medicationInfo by remember { mutableStateOf<MedicationFullInfo?>(null) }

    LaunchedEffect(medicationId) {
        // Simulate data fetching
        kotlinx.coroutines.delay(100) // Simulate network delay
        medicationInfo = MedicationFullInfo(nombreComercial = "Medicamento ID: $medicationId")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(medicationInfo?.nombreComercial ?: "Cargando...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back" // Hardcoded
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (medicationInfo == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                // TODO: Replace with a proper CircularProgressIndicator later
                Text("Cargando información del medicamento...")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp) // Add some horizontal padding for items
            ) {
                // Image Placeholder
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Medication Image Placeholder",
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        // Text("Image Placeholder", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Information Fields
                item { InfoSectionCard(medicationInfo!!) }

                // Links
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinkButton(
                        text = "Ver Prospecto (PDF/Web)",
                        url = medicationInfo?.prospectoUrl,
                        icon = Icons.AutoMirrored.Filled.Subject
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinkButton(
                        text = "Ver Ficha Técnica (PDF/Web)",
                        url = medicationInfo?.fichaTecnicaUrl,
                        icon = Icons.Filled.DocumentScanner
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoSectionCard(info: MedicationFullInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(label = "Nombre comercial", value = info.nombreComercial)
            InfoRow(label = "Principio activo", value = info.principioActivo)
            InfoRow(label = "Dosis", value = info.dosis)
            InfoRow(label = "Forma farmacéutica", value = info.formaFarmaceutica)
            InfoRow(label = "Vías de administración", value = info.viasDeAdministracion.joinToString())
            InfoRow(label = "Laboratorio titular", value = info.laboratorioTitular)
            InfoRow(label = "Estado de registro", value = info.estadoRegistro, showDivider = false) // Last one, no divider
            // InfoRow(label = "Condiciones de prescripción", value = info.condicionesPrescripcion, showDivider = false)
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
             InfoRow(label = "Condiciones de prescripción", value = info.condicionesPrescripcion, showDivider = false)
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
            text = value ?: "No disponible",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp) // Slight indent for value
        )
    }
    if (showDivider) {
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun LinkButton(text: String, url: String?, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Button(
        onClick = { /* TODO: Handle opening URL */ },
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
        var medicationInfo by remember { mutableStateOf<MedicationFullInfo?>(null) }
        LaunchedEffect(Unit) {
             medicationInfo = MedicationFullInfo(
                nombreComercial = "Amoxicilina 500mg",
                principioActivo = "Amoxicilina Trihidrato",
                dosis = "500 mg",
                formaFarmaceutica = "Cápsula dura",
                viasDeAdministracion = listOf("Oral"),
                laboratorioTitular = "Genéricos Pharma S.L.",
                estadoRegistro = "Autorizado",
                condicionesPrescripcion = "Con receta médica",
                prospectoUrl = "https://example.com",
                fichaTecnicaUrl = "https://example.com"
            )
        }
         Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(medicationInfo?.nombreComercial ?: "Cargando...") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (medicationInfo == null) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("Cargando...")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 8.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = "Medication Image Placeholder",
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                    item { InfoSectionCard(medicationInfo!!) }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinkButton("Ver Prospecto", medicationInfo?.prospectoUrl, Icons.AutoMirrored.Filled.Subject)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinkButton("Ver Ficha Técnica", medicationInfo?.fichaTecnicaUrl, Icons.Filled.DocumentScanner)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
