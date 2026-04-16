package com.monospace.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.repository.TaskListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val taskListRepository: TaskListRepository
) : ViewModel() {

    val sidebarItemOrder: StateFlow<List<String>> = settingsDataStore.sidebarItemOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sidebarHiddenItems: StateFlow<Set<String>> = settingsDataStore.sidebarHiddenItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val folders: StateFlow<List<TaskList>> = taskListRepository.observeAllLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val maxOrder = folders.value.maxOfOrNull { it.sortOrder } ?: 0
            taskListRepository.saveList(
                TaskList(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    syncStatus = SyncStatus.PENDING_CREATE,
                    sortOrder = maxOrder + 1
                )
            )
        }
    }

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
