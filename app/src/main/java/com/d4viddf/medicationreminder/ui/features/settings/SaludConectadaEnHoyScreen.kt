package com.d4viddf.medicationreminder.ui.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaludConectadaEnHoyScreen(
    navController: NavController,
    viewModel: SaludConectadaEnHoyViewModel = hiltViewModel()
) {
    val showHealthConnectData by viewModel.showHealthConnectData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.health_connect_today_title)) },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimensions.PaddingScreen)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.salud_conectada_en_hoy_description),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.PaddingSmall)
                        .clickable { viewModel.onShowHealthConnectDataChange(false) },
                    shape = RoundedCornerShape(topStart = Dimensions.PaddingMedium, topEnd = Dimensions.PaddingMedium)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimensions.PaddingMedium)
                    ) {

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = Dimensions.PaddingMedium)
                        ) {
                            Text(text = stringResource(id = R.string.mostrar_solo_datos_de_fitbit_title),fontWeight = FontWeight.Bold)
                            Text(
                                text = stringResource(id = R.string.mostrar_solo_datos_de_fitbit_description),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        RadioButton(
                            selected = !showHealthConnectData,
                            onClick = { viewModel.onShowHealthConnectDataChange(false) }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.PaddingSmall)
                        .clickable { viewModel.onShowHealthConnectDataChange(true) },
                    shape = RoundedCornerShape(bottomStart = Dimensions.PaddingMedium, bottomEnd = Dimensions.PaddingMedium)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimensions.PaddingMedium)
                    ) {

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = Dimensions.PaddingMedium)
                        ) {
                            Text(text = stringResource(id = R.string.mostrar_datos_de_salud_conectada_title), fontWeight = FontWeight.Bold)
                            Text(
                                text = stringResource(id = R.string.mostrar_datos_de_salud_conectada_description),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        RadioButton(
                                selected = showHealthConnectData,
                        onClick = { viewModel.onShowHealthConnectDataChange(true) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
                Text(
                    text = stringResource(id = R.string.que_datos_se_mostraran_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Dimensions.PaddingLarge))
                Text(
                    text = stringResource(id = R.string.que_datos_se_mostraran_description_1),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(Dimensions.PaddingSmall))
                Text(text = "• ${stringResource(id = R.string.dato_frecuencia_cardiaca)}")
                Text(text = "• ${stringResource(id = R.string.dato_ingesta_de_agua)}")
                Text(text = "• ${stringResource(id = R.string.dato_peso)}")
                Text(text = "• ${stringResource(id = R.string.dato_temperatura_corporal)}")
                Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
                Text(
                    text = stringResource(id = R.string.que_datos_se_mostraran_description_2),
                    style = MaterialTheme.typography.bodyLarge
                )
                 Spacer(modifier = Modifier.height(Dimensions.PaddingMedium))
                Text(
                    text = stringResource(id = R.string.que_datos_se_mostraran_description_3),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
