package com.monospace.app.core.domain.model

data class AppShortcut(
    val packageName: String,
    val label: String,
    val sortOrder: Int = 0
)
