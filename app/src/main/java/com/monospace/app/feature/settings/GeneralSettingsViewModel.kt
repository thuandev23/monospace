package com.monospace.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.GeneralSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings: StateFlow<GeneralSettings> = settingsDataStore.generalSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeneralSettings())

    fun update(settings: GeneralSettings) {
        viewModelScope.launch {
            settingsDataStore.setGeneralSettings(settings)
        }
    }
}
