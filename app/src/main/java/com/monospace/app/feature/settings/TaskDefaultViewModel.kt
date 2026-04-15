package com.monospace.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.TaskDisplaySettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDefaultViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings: StateFlow<TaskDisplaySettings> = settingsDataStore.taskDisplaySettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskDisplaySettings())

    fun update(settings: TaskDisplaySettings) {
        viewModelScope.launch {
            settingsDataStore.setTaskDisplaySettings(settings)
        }
    }
}
