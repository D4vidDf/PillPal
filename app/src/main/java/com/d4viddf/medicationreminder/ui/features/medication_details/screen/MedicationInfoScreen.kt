package com.d4viddf.medicationreminder.ui.features.medication_details.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.CimaDocumento
import com.d4viddf.medicationreminder.data.CimaEstado
import com.d4viddf.medicationreminder.data.CimaFormaFarmaceutica
import com.d4viddf.medicationreminder.data.CimaMedicationDetail
import com.d4viddf.medicationreminder.data.CimaViaAdministracion
import com.d4viddf.medicationreminder.ui.common.theme.AppTheme
import com.d4viddf.medicationreminder.ui.common.theme.MedicationColor
import com.d4viddf.medicationreminder.ui.common.theme.MedicationSpecificTheme
import com.d4viddf.medicationreminder.viewmodel.MedicationInfoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// A helper function to format the date, now at the top level for reusability.
private fun formatDate(timestamp: Long?): String? {
    // Timestamps from CIMA are in seconds, so they need to be converted to milliseconds.
    if (timestamp == null || timestamp == -1L) return null
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(timestamp ))
    } catch (e: Exception) {
        null
    }
}

enum class CardType {
    GENERAL_INFO,
    ADDITIONAL_INFO
}

@Composable
private fun InfoCardContent(
    info: CimaMedicationDetail,
    cardType: CardType,
    yesText: String,
    noText: String
) {
    Column(modifier = Modifier.padding(16.dp)) {
        when (cardType) {
            CardType.GENERAL_INFO -> {
                InfoRow(label = stringResource(id = R.string.med_info_label_nombre_comercial), value = info.nombre)
                InfoRow(label = stringResource(id = R.string.med_info_label_principio_activo), value = info.pactivos)
                InfoRow(label = stringResource(id = R.string.med_info_label_forma_farmaceutica), value = info.formaFarmaceutica?.nombre)
                InfoRow(label = stringResource(id = R.string.med_info_label_vias_administracion), value = info.viasAdministracion?.joinToString { it.nombre ?: "" })
                InfoRow(label = stringResource(id = R.string.med_info_label_laboratorio_titular), value = info.labtitular)
                InfoRow(label = stringResource(id = R.string.med_info_label_estado_autorizacion_date), value = formatDate(info.estado?.aut))
                InfoRow(label = stringResource(id = R.string.med_info_label_condiciones_prescripcion), value = info.condPresc, showDivider = false)
            }
            CardType.ADDITIONAL_INFO -> {
                InfoRow(label = stringResource(id = R.string.med_info_label_comercializado), value = info.comerc?.let { if (it) yesText else noText })
                InfoRow(label = stringResource(id = R.string.med_info_label_conduccion), value = info.conduc?.let { if (it) yesText else noText })
                InfoRow(label = stringResource(id = R.string.med_info_label_triangulo_negro), value = info.triangulo?.let { if (it) yesText else noText })
                InfoRow(label = stringResource(id = R.string.med_info_label_huerfano), value = info.huerfano?.let { if (it) yesText else noText })
                InfoRow(label = stringResource(id = R.string.med_info_label_biosimilar), value = info.biosimilar?.let { if (it) yesText else noText }, showDivider = false)
            }
        }
    }
}

@Composable
fun InfoSectionCard(info: CimaMedicationDetail) {
    val yesText = stringResource(id = R.string.text_yes)
    val noText = stringResource(id = R.string.text_no)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        InfoCardContent(info, CardType.GENERAL_INFO, yesText, noText)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        InfoCardContent(info, CardType.ADDITIONAL_INFO, yesText, noText)
    }
}

@Composable
fun SingleInfoSectionCard(
    info: CimaMedicationDetail,
    cardType: CardType,
    modifier: Modifier = Modifier
) {
    val yesText = stringResource(id = R.string.text_yes)
    val noText = stringResource(id = R.string.text_no)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        InfoCardContent(info, cardType, yesText, noText)
    }
}

@Composable
fun InfoRow(label: String, value: String?, showDivider: Boolean = true) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value ?: stringResource(id = R.string.med_info_value_not_available),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun LinkButton(
    text: String,
    url: String?,
    icon: Any,
    onClick: (String) -> Unit
) {
    Button(
        onClick = { url?.let(onClick) },
        enabled = !url.isNullOrBlank(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        when (icon) {
            is ImageVector -> Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            is androidx.compose.ui.graphics.painter.Painter -> Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        }
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text)
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationInfoScreen(
    medicationId: Int,
    colorName: String,
    onNavigateBack: () -> Unit,
    viewModel: MedicationInfoViewModel? = hiltViewModel()
) {
    val medicationColor = remember(colorName) {
        try {
            MedicationColor.valueOf(colorName)
        } catch (e: IllegalArgumentException) {
            MedicationColor.LIGHT_ORANGE // Fallback color
        }
    }
    val cimaMedicationDetail by viewModel?.medicationInfo?.collectAsState() ?: remember { mutableStateOf(null) }
    val isLoading by viewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val error by viewModel?.error?.collectAsState() ?: remember { mutableStateOf(null) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val useGrid = screenWidthDp >= 720.dp

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
                estado = CimaEstado(aut = System.currentTimeMillis() / 1000), // Use seconds for preview
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

    MedicationSpecificTheme(medicationColor = medicationColor) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_arrow_back_ios_24),
                                contentDescription = stringResource(id = R.string.back_button_cd)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
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
                    // **FIX:** Find the packaging image URL from the 'fotos' list.
                    val imageUrl = info.fotos?.find { it.tipo == "materialas" }?.url

                    val uriHandler = LocalUriHandler.current
                    val prospecto = info.docs?.find { it.tipo == 2 }
                    val fichaTecnica = info.docs?.find { it.tipo == 1 }

                    if (useGrid) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 340.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    placeholder = painterResource(id = R.drawable.ic_pill_placeholder),
                                    error = painterResource(id = R.drawable.ic_pill_placeholder),
                                    contentDescription = stringResource(id = R.string.med_info_image_placeholder_cd),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center
                                )
                            }
                            item {
                                SingleInfoSectionCard(info = info, cardType = CardType.GENERAL_INFO)
                            }
                            item {
                                SingleInfoSectionCard(info = info, cardType = CardType.ADDITIONAL_INFO)
                            }
                            item {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinkButton(
                                        text = stringResource(id = R.string.med_info_button_prospecto),
                                        url = prospecto?.urlHtml ?: prospecto?.url,
                                        icon = painterResource(id = R.drawable.rounded_subject_24),
                                        onClick = { url -> uriHandler.openUri(url) }
                                    )
                                    LinkButton(
                                        text = stringResource(id = R.string.med_info_button_ficha_tecnica),
                                        url = fichaTecnica?.urlHtml ?: fichaTecnica?.url,
                                        icon = painterResource(id = R.drawable.rounded_document_scanner_24),
                                        onClick = { url -> uriHandler.openUri(url) }
                                    )
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(horizontal = 8.dp)
                        ) {
                            item {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    placeholder = painterResource(id = R.drawable.ic_pill_placeholder),
                                    error = painterResource(id = R.drawable.ic_pill_placeholder),
                                    contentDescription = stringResource(id = R.string.med_info_image_placeholder_cd),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(vertical = 8.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center
                                )
                            }
                            item { InfoSectionCard(info) }
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                LinkButton(
                                    text = stringResource(id = R.string.med_info_button_prospecto),
                                    url = prospecto?.urlHtml ?: prospecto?.url,
                                    icon = painterResource(id = R.drawable.rounded_subject_24),
                                    onClick = { url -> uriHandler.openUri(url) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            item {
                                LinkButton(
                                    text = stringResource(id = R.string.med_info_button_ficha_tecnica),
                                    url = fichaTecnica?.urlHtml ?: fichaTecnica?.url,
                                    icon = painterResource(id = R.drawable.rounded_document_scanner_24),
                                    onClick = { url -> uriHandler.openUri(url) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Text(stringResource(id = R.string.med_info_not_available_generic))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Medication Info Screen")
@Composable
fun MedicationInfoScreenPreview() {
    AppTheme {
        MedicationInfoScreen(
            medicationId = 1,
            colorName = "LIGHT_RED",
            onNavigateBack = {},
            viewModel = null
        )
    }
}