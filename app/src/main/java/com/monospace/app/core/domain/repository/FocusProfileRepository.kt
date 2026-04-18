package com.monospace.app.core.domain.repository

import com.monospace.app.core.domain.model.FocusProfile
import kotlinx.coroutines.flow.Flow

interface FocusProfileRepository {
    fun observeAll(): Flow<List<FocusProfile>>
    fun observeActive(): Flow<FocusProfile?>
    suspend fun getById(id: String): FocusProfile?
    suspend fun getAll(): List<FocusProfile>
    suspend fun save(profile: FocusProfile)
    suspend fun delete(id: String)
    suspend fun activate(id: String)
    suspend fun deactivate()
}
