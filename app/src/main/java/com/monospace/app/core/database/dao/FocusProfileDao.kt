package com.monospace.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.monospace.app.core.database.entity.FocusProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusProfileDao {

    @Query("SELECT * FROM focus_profiles ORDER BY name ASC")
    fun observeAll(): Flow<List<FocusProfileEntity>>

    @Query("SELECT * FROM focus_profiles WHERE is_active = 1 LIMIT 1")
    fun observeActive(): Flow<FocusProfileEntity?>

    @Query("SELECT * FROM focus_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FocusProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: FocusProfileEntity)

    @Query("DELETE FROM focus_profiles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE focus_profiles SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE focus_profiles SET is_active = 1 WHERE id = :id")
    suspend fun activate(id: String)
}
