package com.monospace.app.core.database.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monospace.app.core.database.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncQueueEntity)

    // Lấy các job đã đến giờ chạy và chưa vượt quá số lần thử lại
    @Query("SELECT * FROM sync_queue WHERE next_retry_at <= :now AND retry_count < :maxRetryCount ORDER BY created_at ASC")
    suspend fun getReadyItems(now: Long, maxRetryCount: Int): List<SyncQueueEntity>

    // Cập nhật thời gian thử lại nếu lỗi mạng
    @Query("UPDATE sync_queue SET retry_count = :retryCount, next_retry_at = :nextRetryAt, error_message = :error WHERE id = :id")
    suspend fun updateForRetry(id: String, retryCount: Int, nextRetryAt: Long, error: String?)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: String)
}
