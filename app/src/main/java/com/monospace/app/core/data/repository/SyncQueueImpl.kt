package com.monospace.app.core.data.repository


import com.monospace.app.core.database.dao.SyncQueueDao
import com.monospace.app.core.database.entity.SyncQueueEntity
import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import javax.inject.Inject

class SyncQueueImpl @Inject constructor(
    private val syncQueueDao: SyncQueueDao
) : SyncQueue {

    override suspend fun enqueue(taskId: String, operation: SyncOperationType, payload: String) {
        val entity = SyncQueueEntity(
            taskId = taskId,
            operation = operation.name.lowercase(),
            payload = payload
        )
        syncQueueDao.insert(entity)

        // TODO: Trigger WorkManager để bắt đầu đồng bộ ngầm (Chúng ta sẽ làm ở Sprint sau)
    }
}