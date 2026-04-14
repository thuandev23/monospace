package com.monospace.app.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.monospace.app.core.database.dao.SyncQueueDao
import com.monospace.app.core.database.dao.TaskDao
import com.monospace.app.core.network.api.TaskApiService
import com.monospace.app.core.network.dto.toDto
import com.monospace.app.core.domain.usecase.PullTasksUseCase
import com.monospace.app.core.database.dao.TaskListDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncQueueDao: SyncQueueDao,
    private val taskDao: TaskDao,
    private val taskListDao: TaskListDao,
    private val taskApiService: TaskApiService,
    private val pullTasksUseCase: PullTasksUseCase
) : CoroutineWorker(context, params) {

    companion object {
        private const val MAX_RETRY_COUNT = 5
        // Exponential backoff: 30s, 60s, 120s, 240s, 480s
        private fun nextRetryDelay(retryCount: Int): Long {
            return 30_000L * (1L shl retryCount)
        }
    }

    override suspend fun doWork(): Result {
        // Chưa có backend → không retry, return success để WorkManager không tốn tài nguyên
        if (!com.monospace.app.BuildConfig.BACKEND_ENABLED) return Result.success()

        val pendingItems = syncQueueDao.getReadyItems(
            now = System.currentTimeMillis(),
            maxRetryCount = MAX_RETRY_COUNT
        )

        if (pendingItems.isEmpty()) return Result.success()

        var hasFailure = false

        for (item in pendingItems) {
            try {
                val success = when (item.operation) {
                    "create" -> handleCreate(item.taskId)
                    "update" -> handleUpdate(item.taskId)
                    "delete" -> handleDelete(item.taskId)
                    else -> true // operation không xác định → bỏ qua
                }

                if (success) {
                    syncQueueDao.delete(item.id)
                } else {
                    hasFailure = true
                    scheduleRetry(item.id, item.retryCount)
                }
            } catch (e: Exception) {
                hasFailure = true
                scheduleRetry(item.id, item.retryCount, e.message)
            }
        }

        if (!hasFailure) {
            // Push xong → pull về để cập nhật data từ server (incremental sync)
            pullFromServer()
        }

        return if (hasFailure) Result.retry() else Result.success()
    }

    private suspend fun pullFromServer() {
        try {
            val lists = taskListDao.getAllListIds()
            for (listId in lists) {
                pullTasksUseCase(listId)
            }
        } catch (_: Exception) {
            // Pull thất bại không ảnh hưởng kết quả push
        }
    }

    private suspend fun handleCreate(taskId: String): Boolean {
        val entity = taskDao.getTaskById(taskId) ?: return true // task bị xóa local → bỏ qua
        val response = taskApiService.createTask(entity.toDto())
        if (response.isSuccessful) {
            taskDao.markAsSynced(taskId)
            return true
        }
        return false
    }

    private suspend fun handleUpdate(taskId: String): Boolean {
        val entity = taskDao.getTaskById(taskId) ?: return true
        val response = taskApiService.updateTask(taskId, entity.toDto())
        if (response.isSuccessful) {
            taskDao.markAsSynced(taskId)
            return true
        }
        return false
    }

    private suspend fun handleDelete(taskId: String): Boolean {
        val response = taskApiService.deleteTask(taskId)
        if (response.isSuccessful) {
            taskDao.hardDelete(taskId)
            return true
        }
        return false
    }

    private suspend fun scheduleRetry(id: String, currentRetryCount: Int, error: String? = null) {
        val newRetryCount = currentRetryCount + 1
        val nextRetryAt = System.currentTimeMillis() + nextRetryDelay(newRetryCount)
        syncQueueDao.updateForRetry(
            id = id,
            retryCount = newRetryCount,
            nextRetryAt = nextRetryAt,
            error = error
        )
    }
}
