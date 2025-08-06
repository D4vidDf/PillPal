package com.d4viddf.medicationreminder.ui.features.personalizehome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeItem
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeSection
import com.d4viddf.medicationreminder.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizeHomeScreen(
    onNavigateBack: () -> Unit,
    viewModel: PersonalizeHomeViewModel = hiltViewModel()
) {
    val sections by viewModel.sections.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.personalize_home_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(sections, key = { _, section -> section.id }) { index, section ->
                SectionItem(
                    section = section,
                    onMoveUp = { viewModel.onMoveSectionUp(section.id) },
                    canMoveUp = index > 0,
                    onMoveDown = { viewModel.onMoveSectionDown(section.id) },
                    canMoveDown = index < sections.size - 1,
                    onToggleItemVisibility = viewModel::onToggleItemVisibility,
                    onUpdateNextDoseUnit = viewModel::onUpdateNextDoseUnit
                )
            }
        }
    }
}

@Composable
private fun SectionItem(
    section: HomeSection,
    canMoveUp: Boolean,
    onMoveUp: () -> Unit,
    canMoveDown: Boolean,
    onMoveDown: () -> Unit,
    onToggleItemVisibility: (itemId: String, isVisible: Boolean) -> Unit,
    onUpdateNextDoseUnit: (unit: String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = section.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        section.items.forEach { item ->
            HomeItemCard(
                item = item,
                onToggleVisibility = { isVisible -> onToggleItemVisibility(item.id, isVisible) },
                onUpdateNextDoseUnit = onUpdateNextDoseUnit
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeItemCard(
    item: HomeItem,
    onToggleVisibility: (Boolean) -> Unit,
    onUpdateNextDoseUnit: (String) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))

            if (item.id == "next_dose") {
                Box {
                    TextButton(onClick = { isMenuExpanded = true }) {
                        Text(text = item.displayUnit ?: "minutes")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("minutes") }, onClick = {
                            onUpdateNextDoseUnit("minutes")
                            isMenuExpanded = false
                        })
                        DropdownMenuItem(text = { Text("seconds") }, onClick = {
                            onUpdateNextDoseUnit("seconds")
                            isMenuExpanded = false
                        })
                    }
                }
            }
            Switch(checked = item.isVisible, onCheckedChange = onToggleVisibility)
        }
    }
}
fun getDefaultSections(): List<HomeSection> {
    return listOf(
        HomeSection(
            id = "progress",
            name = "Progress",
            items = listOf(
                HomeItem("today_progress", "Today Progress"),
                HomeItem("next_dose", "Next Dose", displayUnit = "minutes")
            )
        ),
        HomeSection(
            id = "health",
            name = "Health",
            items = listOf(
                HomeItem("heart_rate", "Heart Rate"),
                HomeItem("weight", "Weight")
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PersonalizeHomeScreenPreview() {
    AppTheme {
        PersonalizeHomeScreen(onNavigateBack = {})
    }
}