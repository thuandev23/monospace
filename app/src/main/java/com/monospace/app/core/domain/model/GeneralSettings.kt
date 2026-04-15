package com.monospace.app.core.domain.model

enum class AppTheme { MINIMALIST, REMINDERS }
enum class AddTaskPosition { BOTTOM, TOP }
enum class SecondStatus { CANCELLED, IN_PROGRESS }

data class GeneralSettings(
    val theme: AppTheme = AppTheme.MINIMALIST,
    val addTaskPosition: AddTaskPosition = AddTaskPosition.BOTTOM,
    val secondStatus: SecondStatus = SecondStatus.CANCELLED,
    val reverseScrollDirection: Boolean = false
)
