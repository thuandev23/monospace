package com.monospace.app.core.domain.repository

import com.monospace.app.core.domain.model.AppInfo


interface AppRepository {
    fun getInstalledApps(): List<AppInfo>
    fun launchApp(packageName: String)
}