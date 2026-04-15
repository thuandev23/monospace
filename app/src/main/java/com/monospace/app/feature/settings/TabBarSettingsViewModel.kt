package com.monospace.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.data.preferences.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TabBarSettings(
    val showUpcoming: Boolean = true,
    val showSearch: Boolean = true
)

@HiltViewModel
class TabBarSettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings: StateFlow<TabBarSettings> = combine(
        settingsDataStore.tabBarShowUpcoming,
        settingsDataStore.tabBarShowSearch
    ) { upcoming, search ->
        TabBarSettings(showUpcoming = upcoming, showSearch = search)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TabBarSettings())

    fun setShowUpcoming(show: Boolean) {
        viewModelScope.launch { settingsDataStore.setTabBarShowUpcoming(show) }
    }

    fun setShowSearch(show: Boolean) {
        viewModelScope.launch { settingsDataStore.setTabBarShowSearch(show) }
    }
}
