package com.monospace.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monospace.app.core.database.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM lists WHERE sync_status != 'pending_delete' ORDER BY sort_order ASC, created_at ASC")
    fun observeAllLists(): Flow<List<TaskListEntity>>

    @Query("UPDATE lists SET sort_order = :order WHERE id = :id")
    suspend fun updateSortOrder(id: String, order: Int)

    @Query("SELECT id FROM lists WHERE sync_status != 'pending_delete'")
    suspend fun getAllListIds(): List<String>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun getListById(id: String): TaskListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: TaskListEntity)

    @Query("UPDATE lists SET sync_status = 'pending_delete' WHERE id = :id")
    suspend fun markAsDeleted(id: String)

    @Delete
    suspend fun delete(list: TaskListEntity)
}
