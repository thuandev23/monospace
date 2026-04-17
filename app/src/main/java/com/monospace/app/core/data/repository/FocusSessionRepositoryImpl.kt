package com.monospace.app.core.data.repository

import com.monospace.app.core.database.dao.FocusSessionDao
import com.monospace.app.core.database.entity.FocusSessionEntity
import com.monospace.app.core.domain.model.DetoxStats
import com.monospace.app.core.domain.model.computeDetoxStats
import com.monospace.app.core.domain.repository.FocusSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class FocusSessionRepositoryImpl @Inject constructor(
    private val dao: FocusSessionDao
) : FocusSessionRepository {

    override suspend fun recordSession(durationMinutes: Int, profileId: String?) {
        dao.insert(
            FocusSessionEntity(
                id = UUID.randomUUID().toString(),
                completedAt = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                profileId = profileId
            )
        )
    }

    override fun observeStats(): Flow<DetoxStats> =
        dao.observeAll().map { sessions ->
            val dates = sessions.map { session ->
                Instant.ofEpochMilli(session.completedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            computeDetoxStats(dates, sessions.size)
        }
}
