package com.monospace.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.data.preferences.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val sidebarItemOrder: StateFlow<List<String>> = settingsDataStore.sidebarItemOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sidebarHiddenItems: StateFlow<Set<String>> = settingsDataStore.sidebarHiddenItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun saveItemOrder(orderedIds: List<String>) {
        viewModelScope.launch {
            settingsDataStore.setSidebarItemOrder(orderedIds)
        }
    }

    fun saveHiddenItems(hiddenIds: Set<String>) {
        viewModelScope.launch {
            settingsDataStore.setSidebarHiddenItems(hiddenIds)
        }
    }

    fun toggleItemVisibility(id: String, currentHidden: Set<String>) {
        val updated = if (currentHidden.contains(id)) currentHidden - id else currentHidden + id
        saveHiddenItems(updated)
    }
}
