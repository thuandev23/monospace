package com.monospace.app.feature.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.AppShortcut
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsDataStore
) : ViewModel() {

    val shortcuts: StateFlow<List<AppShortcut>> = settings.launcherShortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppShortcut>>(emptyList())
    val installedApps: StateFlow<List<AppShortcut>> = _installedApps.asStateFlow()

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun exitEditMode() {
        _isEditMode.value = false
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved: List<ResolveInfo> = context.packageManager
                .queryIntentActivities(intent, PackageManager.GET_META_DATA)

            val apps = resolved
                .map { info ->
                    AppShortcut(
                        packageName = info.activityInfo.packageName,
                        label = info.loadLabel(context.packageManager).toString()
                    )
                }
                .filter { it.packageName != context.packageName }
                .sortedBy { it.label.lowercase() }

            _installedApps.value = apps
        }
    }

    fun addShortcut(app: AppShortcut) {
        viewModelScope.launch {
            val current = shortcuts.value.toMutableList()
            if (current.none { it.packageName == app.packageName }) {
                current.add(app.copy(sortOrder = current.size))
                settings.setLauncherShortcuts(current)
            }
        }
    }

    fun removeShortcut(packageName: String) {
        viewModelScope.launch {
            val updated = shortcuts.value
                .filter { it.packageName != packageName }
                .mapIndexed { i, s -> s.copy(sortOrder = i) }
            settings.setLauncherShortcuts(updated)
        }
    }

    fun launchApp(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
