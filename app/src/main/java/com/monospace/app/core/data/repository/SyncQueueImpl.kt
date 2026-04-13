package com.monospace.app.core.data.repository

import com.monospace.app.core.database.dao.SyncQueueDao
import com.monospace.app.core.database.entity.SyncQueueEntity
import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.sync.SyncScheduler
import javax.inject.Inject

class SyncQueueImpl @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val syncScheduler: SyncScheduler
) : SyncQueue {

    override suspend fun enqueue(taskId: String, operation: SyncOperationType, payload: String) {
        syncQueueDao.insert(
            SyncQueueEntity(
                taskId = taskId,
                operation = operation.name.lowercase(),
                payload = payload
            )
        )
        // Trigger WorkManager chạy sync ngay khi có mạng
        syncScheduler.scheduleSync()
    }
}