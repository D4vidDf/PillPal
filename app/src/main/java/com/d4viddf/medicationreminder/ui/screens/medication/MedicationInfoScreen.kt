package com.d4viddf.medicationreminder.ui.screens.medication

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.CimaMedicationDetail
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.ui.colors.MedicationColor
import com.d4viddf.medicationreminder.ui.components.HtmlText
import com.d4viddf.medicationreminder.ui.theme.AppTheme
import com.d4viddf.medicationreminder.ui.theme.MedicationSpecificTheme
import com.d4viddf.medicationreminder.utils.observeWithLifecycle
import com.d4viddf.medicationreminder.viewmodel.MedicationInfoViewModel
import com.d4viddf.medicationreminder.viewmodel.MedicationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationInfoScreen(
    medicationId: Int,
    colorName: String?,
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel(),
    medicationInfoViewModel: MedicationInfoViewModel = hiltViewModel()
) {
    var medicationState by remember { mutableStateOf<Medication?>(null) }
    val medicationInfoState by medicationInfoViewModel.cimaMedicationDetail.collectAsState(null)
    val isLoading by medicationInfoViewModel.isLoading.collectAsState()
    val error by medicationInfoViewModel.error.collectAsState()

    val medicationColor = remember(colorName) {
        try {
            colorName?.let { MedicationColor.valueOf(it) } ?: MedicationColor.LIGHT_BLUE
        } catch (e: IllegalArgumentException) {
            MedicationColor.LIGHT_BLUE // Default color
        }
    }

    LaunchedEffect(medicationId) {
        viewModel.getMedicationById(medicationId)?.let {
            medicationState = it
            it.nregistro?.let { nRegistro ->
                if (nRegistro.isNotBlank()) {
                    medicationInfoViewModel.fetchCimaMedicationInfo(nRegistro)
                } else {
                    medicationInfoViewModel.setNoNRegistroAvailableError()
                }
            } ?: medicationInfoViewModel.setNoNRegistroAvailableError()
        }
    }

    medicationInfoViewModel.toastMessage.observeWithLifecycle { message ->
        // TODO: Show toast message (Need context or a snackbar host)
        Log.d("MedicationInfoScreen", "Toast: $message")
    }

    MedicationSpecificTheme(medicationColor = medicationColor) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            medicationState?.name ?: stringResource(R.string.medication_info_screen_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = medicationColor.onBackgroundColor // Or MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    navigationIcon = {
                        Box(modifier = Modifier.padding(start = 10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    .clickable { onNavigateBack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = stringResource(id = R.string.back),
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = medicationColor.backgroundColor,
                        navigationIconContentColor = Color.White, // Ensure contrast if background is light
                        titleContentColor = medicationColor.onBackgroundColor // Ensure contrast
                    )
                )
            },
            containerColor = medicationColor.backgroundColor.copy(alpha = 0.8f)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (error != null) {
                    Text(
                        text = error ?: stringResource(id = R.string.unknown_error),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = medicationColor.onBackgroundColor, // Or MaterialTheme.colorScheme.error
                        style = MaterialTheme.typography.titleMedium
                    )
                } else if (medicationInfoState != null) {
                    MedicationInfoDetailContent(medicationInfoState!!, medicationColor)
                } else {
                    Text(
                        text = stringResource(R.string.medication_info_not_available_short),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = medicationColor.onBackgroundColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}


@Composable
fun InfoRow(label: String, value: String?, showDivider: Boolean = true) {
    if (value != null && value.isNotBlank()) { // Also check for isNotBlank
        Column(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
            Text(
                text = label.uppercase(), // Uppercase label for distinction
                style = MaterialTheme.typography.labelMedium, // Smaller label
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold // Slightly less bold
            )
            Spacer(modifier = Modifier.height(2.dp)) // Reduced spacer
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 0.dp) // No indent needed if label is styled
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp), // Keep padding for divider
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) // Softer divider
            )
        }
    }
}

@Composable
fun GeneralInfoCard(info: CimaMedicationDetail) {
    fun formatDate(timestamp: Long?): String? {
        if (timestamp == null || timestamp == -1L) return null
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp * 1000))
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
}

@Composable
fun AdditionalFlagsCard(info: CimaMedicationDetail) {
    val yesText = stringResource(id = R.string.text_yes)
    val noText = stringResource(id = R.string.text_no)
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

/*
@Composable
fun InfoSectionCard(info: CimaMedicationDetail) {
    val yesText = stringResource(id = R.string.text_yes)
    val noText = stringResource(id = R.string.text_no)

    fun formatDate(timestamp: Long?): String? {
        if (timestamp == null || timestamp == -1L) return null
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp * 1000))
        } catch (e: Exception) {
            null
        }
    }

    // Card 1: General Details
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
            // ... other InfoRows from GeneralInfoCard
            InfoRow(label = stringResource(id = R.string.med_info_label_condiciones_prescripcion), value = info.condPresc, showDivider = false)
        }
    }

    Spacer(modifier = Modifier.height(8.dp)) // Spacer between cards

    // Card 2: Additional Flags
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(label = stringResource(id = R.string.med_info_label_comercializado), value = info.comerc?.let { if (it) yesText else noText })
            // ... other InfoRows from AdditionalFlagsCard
            InfoRow(label = stringResource(id = R.string.med_info_label_biosimilar), value = info.biosimilar?.let { if (it) yesText else noText }, showDivider = false)
        }
    }
}
*/


@Composable
fun MedicationInfoDetailContent(
    info: CimaMedicationDetail,
    medicationColor: MedicationColor // Keep for consistency, though not directly used in cards now
) {
    val uriHandler = LocalUriHandler.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Determine if a grid layout should be used based on screen width
    val useGrid = screenWidth > 600.dp // Example threshold for grid layout

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Main Title for the medication - remains at the top
        Text(
            text = info.nombre?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } ?: stringResource(id = R.string.medication_name_placeholder),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = medicationColor.onBackgroundColor, // Use medication specific color
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        if (useGrid) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp), // Adjust minSize as needed
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 72.dp), // For FAB or other bottom elements
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { GeneralInfoCard(info = info) }
                item { AdditionalFlagsCard(info = info) }

                // Documents section (can span multiple columns if needed or be a single column item)
                if (!info.docs.isNullOrEmpty()) {
                    item(span = { GridCells.Adaptive(minSize = 300.dp).maxLineSpan }) { // Full width for section title
                        Text(
                            text = stringResource(R.string.med_info_documents_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = medicationColor.onBackgroundColor,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(info.docs.size) { index ->
                        val doc = info.docs[index]
                        DocumentButton(doc = doc, uriHandler = uriHandler)
                    }
                }
                 if (!info.notas.isNullOrBlank()) {
                    item(span = { GridCells.Adaptive(minSize = 300.dp).maxLineSpan }) { NotesSection(notes = info.notas!!, medicationColor = medicationColor)}
                }

                if (!info.materialesInf.isNullOrEmpty()) {
                     item(span = { GridCells.Adaptive(minSize = 300.dp).maxLineSpan }) { InformationMaterialsSection(materials = info.materialesInf!!, uriHandler = uriHandler, medicationColor = medicationColor)}
                }
            }
        } else { // useGrid is false, use LazyColumn
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 72.dp), // For FAB or other bottom elements
                verticalArrangement = Arrangement.spacedBy(0.dp) // Let cards manage their own padding
            ) {
                item { GeneralInfoCard(info = info) }
                item { AdditionalFlagsCard(info = info) }

                if (!info.docs.isNullOrEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.med_info_documents_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = medicationColor.onBackgroundColor,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(info.docs.size) { index ->
                        val doc = info.docs[index]
                        DocumentButton(doc = doc, uriHandler = uriHandler)
                    }
                }
                 if (!info.notas.isNullOrBlank()) {
                    item { NotesSection(notes = info.notas!!, medicationColor = medicationColor) }
                }
                if (!info.materialesInf.isNullOrEmpty()) {
                    item { InformationMaterialsSection(materials = info.materialesInf!!, uriHandler = uriHandler, medicationColor = medicationColor) }
                }
            }
        }
    }
}


@Composable
fun DocumentButton(doc: CimaMedicationDetail.Documento, uriHandler: UriHandler) {
    if (doc.url != null) {
        Button(
            onClick = {
                try {
                    uriHandler.openUri(doc.url)
                } catch (e: Exception) {
                    Log.e("DocumentButton", "Failed to open URI: ${doc.url}", e)
                    // Optionally, show a toast or snackbar to the user
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    painterResource(id = R.drawable.file_pdf_solid),
                    contentDescription = stringResource(R.string.med_info_doc_icon_desc),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = doc.tipo?.let { type ->
                            stringResource(
                                when (type) {
                                    1 -> R.string.med_info_doc_type_ft
                                    2 -> R.string.med_info_doc_type_prospecto
                                    3 -> R.string.med_info_doc_type_it // Assuming type 3 is Informe Técnico
                                    4 -> R.string.med_info_doc_type_epar // Assuming type 4 is EPAR
                                    else -> R.string.med_info_doc_type_other
                                }
                            )
                        } ?: stringResource(id = R.string.med_info_doc_type_other),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    doc.secc?.let { isSection ->
                        if(isSection) {
                             Text(stringResource(R.string.med_info_doc_section_available), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                     doc.fecha?.let { timestamp ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        try {
                            val dateString = sdf.format(Date(timestamp * 1000))
                            Text(stringResource(R.string.med_info_doc_date, dateString), style = MaterialTheme.typography.labelSmall)
                        } catch (e: Exception) { /* Do nothing if date is invalid */ }
                    }
                }
            }
        }
    }
}

@Composable
fun NotesSection(notes: String, medicationColor: MedicationColor) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.med_info_notes_title),
            style = MaterialTheme.typography.titleLarge,
            color = medicationColor.onBackgroundColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            HtmlText(
                html = notes,
                modifier = Modifier.padding(16.dp),
                defaultStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
fun InformationMaterialsSection(materials: List<CimaMedicationDetail.MaterialInfo>, uriHandler: UriHandler, medicationColor: MedicationColor) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.med_info_materiales_informativos_title),
            style = MaterialTheme.typography.titleLarge,
            color = medicationColor.onBackgroundColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        materials.forEach { material ->
            if (material.url != null) {
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri(material.url)
                        } catch (e: Exception) {
                            Log.e("InfoMaterials", "Failed to open URI: ${material.url}", e)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                     Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            painterResource(id = R.drawable.link_solid), // Generic link icon
                            contentDescription = stringResource(R.string.med_info_material_icon_desc),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = material.nombre ?: stringResource(id = R.string.med_info_material_generic_name),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            material.fecha?.let { timestamp ->
                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                try {
                                    val dateString = sdf.format(Date(timestamp * 1000))
                                    Text(stringResource(R.string.med_info_material_date, dateString), style = MaterialTheme.typography.labelSmall)
                                } catch (e: Exception) { /* Do nothing */ }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, widthDp = 360)
@Composable
fun MedicationInfoScreenNarrowPreview() {
    val mockMedication = CimaMedicationDetail(
        nombre = "Awesome Pill 500mg",
        pactivos = "Awesomeness",
        labtitular = "Pharma Innovations SL",
        estado = CimaMedicationDetail.Estado(aut = 1672531200L), // Example timestamp
        condPresc = "Receta médica",
        comerc = true,
        conduc = false,
        triangulo = true,
        huerfano = false,
        biosimilar = false,
        docs = listOf(
            CimaMedicationDetail.Documento(tipo = 1, url = "http://example.com/ft", secc = true, fecha = 1672531200L),
            CimaMedicationDetail.Documento(tipo = 2, url = "http://example.com/prospecto", secc = false, fecha = 1672531200L)
        ),
        notas = "Take with food. <br> Not suitable for children under 12.",
        materialesInf = listOf(CimaMedicationDetail.MaterialInfo(nombre = "Guía para pacientes", url = "http://example.com/guia", fecha = 1672531200L))
    )
    AppTheme {
        MedicationInfoScreen(
            medicationId = 1, // Dummy ID
            colorName = "LIGHT_GREEN",
            onNavigateBack = {},
            medicationInfoViewModel = previewMedicationInfoViewModel(mockMedication)
        )
    }
}

@Preview(showBackground = true, widthDp = 700)
@Composable
fun MedicationInfoScreenWidePreview() {
     val mockMedication = CimaMedicationDetail(
        nombre = "Awesome Pill 500mg",
        pactivos = "Awesomeness",
        labtitular = "Pharma Innovations SL",
        estado = CimaMedicationDetail.Estado(aut = 1672531200L), // Example timestamp
        condPresc = "Receta médica",
        comerc = true,
        conduc = false,
        triangulo = true,
        huerfano = false,
        biosimilar = false,
        docs = listOf(
            CimaMedicationDetail.Documento(tipo = 1, url = "http://example.com/ft", secc = true, fecha = 1672531200L),
            CimaMedicationDetail.Documento(tipo = 2, url = "http://example.com/prospecto", secc = false, fecha = 1672531200L)
        ),
        notas = "Take with food. <br> Not suitable for children under 12.",
        materialesInf = listOf(CimaMedicationDetail.MaterialInfo(nombre = "Guía para pacientes", url = "http://example.com/guia", fecha = 1672531200L))
    )
    AppTheme {
        MedicationInfoScreen(
            medicationId = 1, // Dummy ID
            colorName = "LIGHT_BLUE",
            onNavigateBack = {},
            medicationInfoViewModel = previewMedicationInfoViewModel(mockMedication)
        )
    }
}


// Helper for Previews
@Composable
fun previewMedicationInfoViewModel(mockData: CimaMedicationDetail? = null): MedicationInfoViewModel {
    val viewModel: MedicationInfoViewModel = hiltViewModel() // This might still try to fetch if not careful
    // Override necessary parts or use a mock instance if Hilt allows/testing framework provides
    // For simplicity, we'll assume direct state manipulation or a simple mock setup here
    // This is a simplified approach; proper mocking might require a testing framework or Hilt customizations
    if (mockData != null) {
        // This is tricky because Flow is involved. A real mock ViewModel would be better.
        // As a hack for preview, you might have a mutable state in ViewModel for preview.
        // Or, pass data directly to MedicationInfoDetailContent for preview.
    }
    return viewModel // In a real test, this would be a mock.
}
