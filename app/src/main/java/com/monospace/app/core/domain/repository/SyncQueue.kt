package com.monospace.app.core.domain.repository


interface SyncQueue {
    suspend fun enqueue(
        taskId: String,
        operation: SyncOperationType,
        payload: String
    )
}

enum class SyncOperationType {
    CREATE, UPDATE, DELETE
}