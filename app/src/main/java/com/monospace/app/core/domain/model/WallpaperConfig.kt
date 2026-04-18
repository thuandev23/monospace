package com.monospace.app.core.domain.model

enum class WallpaperAlignment { TOP, CENTER, BOTTOM }

data class WallpaperConfig(
    val backgroundColorHex: String = "#111111",
    val textColorHex: String = "#EFEFEF",
    val showTime: Boolean = true,
    val showDate: Boolean = true,
    val showTasks: Boolean = true,
    val taskLimit: Int = 5,
    val contentAlignment: WallpaperAlignment = WallpaperAlignment.CENTER,
    val autoUpdate: Boolean = false,
    val showOnHome: Boolean = false
)
