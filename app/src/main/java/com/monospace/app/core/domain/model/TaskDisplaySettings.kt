package com.monospace.app.core.domain.model

enum class TaskAlignment { LEADING, CENTER, TRAILING }

data class TaskDisplaySettings(
    val showStatusCircle: Boolean = true,
    val lowercase: Boolean = false,
    val fontSize: Int = 17,
    val alignment: TaskAlignment = TaskAlignment.LEADING,
    val secondStatus: SecondStatus = SecondStatus.CANCELLED
)
