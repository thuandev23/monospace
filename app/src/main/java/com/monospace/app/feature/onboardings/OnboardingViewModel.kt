package com.monospace.app.feature.onboardings

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
class OnboardingViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val onboardingCompleted: StateFlow<Boolean?> = settingsDataStore.onboardingCompleted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null  // null = still loading
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted()
        }
    }
}
