package com.monospace.app.core.data.repository

import com.google.gson.Gson
import com.monospace.app.core.database.dao.FocusProfileDao
import com.monospace.app.core.database.entity.FocusProfileEntity
import com.monospace.app.core.domain.model.FocusProfile
import com.monospace.app.core.domain.model.FocusSchedule
import com.monospace.app.core.domain.repository.FocusProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FocusProfileRepositoryImpl @Inject constructor(
    private val dao: FocusProfileDao,
    private val gson: Gson
) : FocusProfileRepository {

    override fun observeAll(): Flow<List<FocusProfile>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActive(): Flow<FocusProfile?> =
        dao.observeActive().map { 
            android.util.Log.d("BLOCK_DEBUG", "Repository.observeActive emission: ${it?.name} (isActive=${it?.isActive})")
            it?.toDomain() 
        }

    override suspend fun getById(id: String): FocusProfile? =
        dao.getById(id)?.toDomain()

    override suspend fun getAll(): List<FocusProfile> =
        dao.getAll().map { it.toDomain() }

    override suspend fun save(profile: FocusProfile) =
        dao.upsert(profile.toEntity())

    override suspend fun delete(id: String) =
        dao.delete(id)

    override suspend fun activate(id: String) {
        android.util.Log.d("BLOCK_DEBUG", "Repository.activate: $id")
        dao.deactivateAll()
        dao.activate(id)
    }

    override suspend fun deactivate() {
        android.util.Log.d("BLOCK_DEBUG", "Repository.deactivate")
        dao.deactivateAll()
    }

    // ---- Mappers ----

    private fun FocusProfileEntity.toDomain(): FocusProfile {
        val appIds = if (allowedAppIds.isBlank()) emptySet()
        else allowedAppIds.split(",").filter { it.isNotBlank() }.toSet()

        val schedule = scheduleJson?.let {
            runCatching {
                gson.fromJson(it, FocusScheduleDto::class.java)?.toDomain()
            }.getOrNull()
        }

        return FocusProfile(
            id = id,
            name = name,
            allowedAppIds = appIds,
            linkedListId = linkedListId,
            schedule = schedule,
            isActive = isActive
        )
    }

    private fun FocusProfile.toEntity(): FocusProfileEntity {
        val appIdsStr = allowedAppIds.joinToString(",")
        val scheduleStr = schedule?.let { gson.toJson(FocusScheduleDto.from(it)) }
        return FocusProfileEntity(
            id = id,
            name = name,
            allowedAppIds = appIdsStr,
            linkedListId = linkedListId,
            scheduleJson = scheduleStr,
            isActive = isActive
        )
    }

    // DTO for Gson serialization
    private data class FocusScheduleDto(
        val startHour: Int = 0,
        val startMinute: Int = 0,
        val endHour: Int = 0,
        val endMinute: Int = 0,
        val daysOfWeek: List<Int> = emptyList()
    ) {
        fun toDomain() = FocusSchedule(
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            daysOfWeek = daysOfWeek.toSet()
        )

        companion object {
            fun from(s: FocusSchedule) = FocusScheduleDto(
                startHour = s.startHour,
                startMinute = s.startMinute,
                endHour = s.endHour,
                endMinute = s.endMinute,
                daysOfWeek = s.daysOfWeek.toList()
            )
        }
    }
}
