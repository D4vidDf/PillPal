package com.d4viddf.medicationreminder.ui.features.personalizehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            // If no layout has ever been saved, create the default one and save it immediately.
            if (savedLayout.isEmpty()) {
                val defaultLayout = getDefaultSections()
                _sections.value = defaultLayout
                saveSections() // Save the default layout so it's no longer empty
            } else {
                _sections.value = savedLayout
            }
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
                name = "Progress",
                items = listOf(
                    HomeItem("today_progress", "Today Progress", isVisible = true),
                    HomeItem("next_dose", "Next Dose", isVisible = true, displayUnit = "minutes"),
                    HomeItem("missed_reminders", "Missed Reminders",  isVisible = true)
                )
            ),
            HomeSection(
                id = "nutrition",
                name = "Nutrition",
                items = listOf(
                    HomeItem("water", "Water Intake",  isVisible = true)
                )
            ),
            HomeSection(
                id = "health",
                name = "Health",
                items = listOf(
                    HomeItem("heart_rate", "Heart Rate",  isVisible = true),
                    HomeItem("weight", "Weight",  isVisible = true),
                    HomeItem("temperature", "Temperature", isVisible = true)

                )
            )
        )
    }
}