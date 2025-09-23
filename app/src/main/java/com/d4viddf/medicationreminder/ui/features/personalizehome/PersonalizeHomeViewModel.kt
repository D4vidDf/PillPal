package com.d4viddf.medicationreminder.ui.features.personalizehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.repository.UserPreferencesRepository
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeItem
import com.d4viddf.medicationreminder.ui.features.personalizehome.model.HomeSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonalizeHomeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _sections = MutableStateFlow<List<HomeSection>>(emptyList())
    val sections = _sections.asStateFlow()

    init {
        loadSections()
    }

    private fun loadSections() {
        viewModelScope.launch {
            val savedLayout = userPreferencesRepository.homeLayoutFlow.first()
            val defaultLayout = getDefaultSections()

            // If no layout has ever been saved, create the default one and save it immediately.
            if (savedLayout.isEmpty()) {
                _sections.value = defaultLayout
                saveSections() // Save the default layout so it's no longer empty
            } else {
                // --- NEW: Reconcile saved layout with the default layout ---
                val reconciledLayout = reconcileLayouts(savedLayout, defaultLayout)
                _sections.value = reconciledLayout
                // If changes were made, save the updated layout back to the repository.
                if (reconciledLayout != savedLayout) {
                    saveSections()
                }
            }
        }
    }
    private fun reconcileLayouts(saved: List<HomeSection>, default: List<HomeSection>): List<HomeSection> {
        val defaultSectionsMap = default.associateBy { it.id }
        val savedSectionsMap = saved.associateBy { it.id }

        // Start with the user's saved order
        val reconciled = saved.toMutableList()

        // Add new sections from default that are missing in saved
        default.forEach { defaultSection ->
            if (!savedSectionsMap.containsKey(defaultSection.id)) {
                reconciled.add(defaultSection)
            }
        }

        // For each section, add new items that are missing
        return reconciled.map { section ->
            val defaultItemsMap = defaultSectionsMap[section.id]?.items?.associateBy { it.id } ?: emptyMap()
            val savedItemsMap = section.items.associateBy { it.id }
            val reconciledItems = section.items.toMutableList()

            defaultItemsMap.values.forEach { defaultItem ->
                if (!savedItemsMap.containsKey(defaultItem.id)) {
                    reconciledItems.add(defaultItem)
                }
            }
            section.copy(items = reconciledItems)
        }
    }
    fun onMoveSectionUp(sectionId: String) {
        val currentList = _sections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == sectionId }
        if (index > 0) {
            val item = currentList.removeAt(index)
            currentList.add(index - 1, item)
            _sections.value = currentList
            saveSections()
        }
    }

    fun onMoveSectionDown(sectionId: String) {
        val currentList = _sections.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == sectionId }
        if (index != -1 && index < currentList.size - 1) {
            val item = currentList.removeAt(index)
            currentList.add(index + 1, item)
            _sections.value = currentList
            saveSections()
        }
    }

    fun onToggleItemVisibility(itemId: String, isVisible: Boolean) {
        val currentSections = _sections.value.map { section ->
            section.copy(items = section.items.map { item ->
                if (item.id == itemId) item.copy(isVisible = isVisible) else item
            })
        }
        _sections.value = currentSections
        saveSections()
    }

    fun onUpdateNextDoseUnit(unit: String) {
        val currentSections = _sections.value.map { section ->
            section.copy(items = section.items.map { item ->
                if (item.id == "next_dose") item.copy(displayUnit = unit) else item
            })
        }
        _sections.value = currentSections
        saveSections()
    }

    private fun saveSections() {
        viewModelScope.launch {
            userPreferencesRepository.saveHomeLayout(_sections.value)
        }
    }

    private fun getDefaultSections(): List<HomeSection> {
        // This is the default layout. All items are visible.
        return listOf(
            HomeSection(
                id = "progress",
                nameRes = R.string.home_section_progress,
                items = listOf(
                    HomeItem(
                        id = "today_progress",
                        nameRes = R.string.home_item_today_progress,
                        isVisible = true
                    ),
                    HomeItem(
                        id = "next_dose",
                        nameRes = R.string.home_item_next_dose,
                        isVisible = true,
                        displayUnit = "minutes"
                    ),
                    HomeItem(
                        id = "missed_reminders",
                        nameRes = R.string.home_item_missed_reminders,
                        isVisible = true
                    )
                )
            ),
            HomeSection(
                id = "nutrition",
                nameRes = R.string.home_section_nutrition,
                items = listOf(
                    HomeItem(
                        id = "water",
                        nameRes = R.string.home_item_water_intake,
                        isVisible = true
                    )
                )
            ),
            HomeSection(
                id = "health",
                nameRes = R.string.home_section_health,
                items = listOf(
                    HomeItem(
                        id = "heart_rate",
                        nameRes = R.string.home_item_heart_rate,
                        isVisible = true
                    ),
                    HomeItem(id = "weight", nameRes = R.string.home_item_weight, isVisible = true),
                    HomeItem(
                        id = "temperature",
                        nameRes = R.string.home_item_temperature,
                        isVisible = true
                    )

                )
            )
        )
    }
}