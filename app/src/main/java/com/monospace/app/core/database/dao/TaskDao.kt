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
    @Query("SELECT * FROM tasks WHERE list_id = :listId AND sync_status != 'pending_delete' ORDER BY CASE WHEN task_status = 'DONE' THEN 1 ELSE 0 END ASC, priority DESC, start_date_time ASC")
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

    @Query("UPDATE tasks SET task_status = :taskStatus, sync_status = 'pending_update', updated_at = :now WHERE id = :id")
    suspend fun updateTaskStatus(
        id: String,
        taskStatus: String,
        now: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("UPDATE tasks SET sync_status = 'synced' WHERE id = :id")
    suspend fun markAsSynced(id: String)

    // Lấy tasks có reminder và chưa hoàn thành, dùng cho BootReceiver re-schedule
    @Query("""
        SELECT * FROM tasks
        WHERE reminder_value IS NOT NULL
          AND task_status NOT IN ('DONE', 'CANCELLED')
          AND sync_status != 'pending_delete'
          AND start_date_time > :nowMs
    """)
    suspend fun getTasksWithFutureReminders(nowMs: Long): List<TaskEntity>

    // Đếm active tasks theo list
    @Query("SELECT COUNT(*) FROM tasks WHERE list_id = :listId AND task_status != 'DONE' AND task_status != 'CANCELLED' AND sync_status != 'pending_delete'")
    fun observeActiveTaskCountForList(listId: String): Flow<Int>

    // Đếm tất cả active tasks
    @Query("SELECT COUNT(*) FROM tasks WHERE task_status != 'DONE' AND task_status != 'CANCELLED' AND sync_status != 'pending_delete'")
    fun observeAllActiveTaskCount(): Flow<Int>

    // Đếm tasks hôm nay (start_date_time trong khoảng dayStart..dayEnd)
    @Query("SELECT COUNT(*) FROM tasks WHERE task_status != 'DONE' AND task_status != 'CANCELLED' AND sync_status != 'pending_delete' AND start_date_time >= :dayStart AND start_date_time < :dayEnd")
    fun observeTodayTaskCount(dayStart: Long, dayEnd: Long): Flow<Int>

    // Tất cả tasks không bị xóa, sắp xếp theo ngày (null cuối), dùng cho Upcoming view
    @Query("""
        SELECT * FROM tasks
        WHERE sync_status != 'pending_delete'
        ORDER BY
            CASE WHEN start_date_time IS NULL THEN 1 ELSE 0 END ASC,
            start_date_time ASC,
            CASE WHEN task_status = 'DONE' THEN 1 ELSE 0 END ASC,
            priority DESC
    """)
    fun observeAllTasksSortedByDate(): Flow<List<TaskEntity>>

    // Tasks hôm nay (start_date trong dayStart..dayEnd) hoặc không có ngày, từ tất cả lists
    @Query("""
        SELECT * FROM tasks
        WHERE sync_status != 'pending_delete'
          AND (
            start_date_time IS NULL
            OR (start_date_time >= :dayStart AND start_date_time < :dayEnd)
          )
        ORDER BY
            CASE WHEN start_date_time IS NULL THEN 1 ELSE 0 END ASC,
            CASE WHEN task_status = 'DONE' THEN 1 ELSE 0 END ASC,
            priority DESC
    """)
    fun observeTodayTasks(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE sync_status != 'pending_delete'
          AND task_status NOT IN ('DONE', 'CANCELLED')
          AND (
            start_date_time IS NULL
            OR (start_date_time >= :dayStart AND start_date_time < :dayEnd)
          )
        ORDER BY
            CASE WHEN start_date_time IS NULL THEN 1 ELSE 0 END ASC,
            priority DESC
        LIMIT 20
    """)
    suspend fun getTodayTasksSnapshot(dayStart: Long, dayEnd: Long): List<TaskEntity>
}