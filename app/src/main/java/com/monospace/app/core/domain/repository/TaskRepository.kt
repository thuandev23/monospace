package com.monospace.app.core.domain.repository


import com.monospace.app.core.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    // Lấy danh sách task theo list, trả về Flow để UI tự update
    fun observeTasks(listId: String): Flow<List<Task>>

    // Thêm hoặc cập nhật task
    suspend fun saveTask(task: Task)

    // Đánh dấu hoàn thành task
    suspend fun markTaskCompleted(taskId: String, isCompleted: Boolean)

    // Xóa task (thực chất là đánh dấu pending_delete)
    suspend fun deleteTask(taskId: String)
}