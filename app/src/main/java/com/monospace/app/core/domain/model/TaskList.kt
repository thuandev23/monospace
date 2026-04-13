package com.monospace.app.core.domain.model

data class TaskList(
    val id: String,
    val name: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
