package com.monospace.app.core.database.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monospace.app.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Flow giúp UI tự động cập nhật khi data trong database thay đổi
    @Query("SELECT * FROM tasks WHERE list_id = :listId AND sync_status != 'pending_delete' ORDER BY is_completed ASC, priority DESC, start_date_time ASC")
    fun observeTasksByList(listId: String): Flow<List<TaskEntity>>

    // Lấy các task đang chờ đồng bộ lên server
    @Query("SELECT * FROM tasks WHERE sync_status != 'synced'")
    suspend fun getPendingTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    // Soft delete: Chỉ đánh dấu là pending_delete để chờ đồng bộ
    @Query("UPDATE tasks SET sync_status = 'pending_delete', updated_at = :now WHERE id = :id")
    suspend fun markAsDeleted(id: String, now: Long = System.currentTimeMillis())

    // Hard delete: Xóa thật sau khi server đã confirm
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("UPDATE tasks SET is_completed = :isCompleted, sync_status = 'pending_update', updated_at = :now WHERE id = :id")
    suspend fun updateCompletionStatus(
        id: String,
        isCompleted: Boolean,
        now: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("UPDATE tasks SET sync_status = 'synced' WHERE id = :id")
    suspend fun markAsSynced(id: String)
}