package com.monospace.app.core.data.repository

import com.monospace.app.core.database.dao.TaskListDao
import com.monospace.app.core.database.entity.TaskListEntity
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.repository.TaskListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskListRepositoryImpl @Inject constructor(
    private val taskListDao: TaskListDao
) : TaskListRepository {

    override fun observeAllLists(): Flow<List<TaskList>> {
        return taskListDao.observeAllLists().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveList(list: TaskList) {
        taskListDao.upsert(list.toEntity())
    }

    override suspend fun deleteList(id: String) {
        taskListDao.markAsDeleted(id)
    }
}

private fun TaskListEntity.toDomain(): TaskList {
    return TaskList(
        id = this.id,
        name = this.name,
        syncStatus = when (this.syncStatus) {
            "synced" -> SyncStatus.SYNCED
            "pending_create" -> SyncStatus.PENDING_CREATE
            "pending_update" -> SyncStatus.PENDING_UPDATE
            "pending_delete" -> SyncStatus.PENDING_DELETE
            else -> SyncStatus.SYNCED
        }
    )
}

private fun TaskList.toEntity(): TaskListEntity {
    return TaskListEntity(
        id = this.id,
        name = this.name,
        syncStatus = when (this.syncStatus) {
            SyncStatus.SYNCED -> "synced"
            SyncStatus.PENDING_CREATE -> "pending_create"
            SyncStatus.PENDING_UPDATE -> "pending_update"
            SyncStatus.PENDING_DELETE -> "pending_delete"
        }
    )
}
