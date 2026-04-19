package com.monospace.app.feature.wallpaper

import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LiveWallpaperEntryPoint {
    fun taskRepository(): TaskRepository
    fun settingsDataStore(): SettingsDataStore
}
