package com.monospace.app.core.domain.repository

import com.monospace.app.core.domain.model.DetoxStats
import kotlinx.coroutines.flow.Flow

interface FocusSessionRepository {
    suspend fun recordSession(durationMinutes: Int, profileId: String?)
    fun observeStats(): Flow<DetoxStats>
}
