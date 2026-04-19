package com.monospace.app.core.domain.model

data class ViewSettings(
    val showOverdue: Boolean = true,
    val showInProgress: Boolean = true,
    val showCompleted: Boolean = true,
    val showTime: Boolean = true,
    val showFolder: Boolean = true,
    val showPriority: Boolean = false,
    val sortBy: SortOption = SortOption.MANUAL,
    val groupBy: GroupOption = GroupOption.NONE
)

enum class SortOption { MANUAL, NAME, DATE, FOLDER }

enum class GroupOption { NONE, DEFAULT, FOLDER }
