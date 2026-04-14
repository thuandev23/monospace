package com.monospace.app.core.domain.usecase

import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.ReminderUnit
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.RepeatUnit
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.network.api.TaskApiService
import com.monospace.app.core.network.dto.TaskDto
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

class PullTasksUseCase @Inject constructor(
    private val apiService: TaskApiService,
    private val taskRepository: TaskRepository
) {
    /**
     * Lấy tasks từ server theo listId, merge vào local DB.
     * @param listId danh sách cần pull
     * @param updatedAfter chỉ lấy tasks thay đổi sau mốc này (incremental sync)
     */
    suspend operator fun invoke(listId: String, updatedAfter: Long? = null): Result<Int> {
        return try {
            val response = apiService.getTasks(listId, updatedAfter)
            if (response.isSuccessful) {
                val remoteTasks = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                taskRepository.mergeRemoteTasks(remoteTasks)
                Result.success(remoteTasks.size)
            } else {
                Result.failure(Exception("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun TaskDto.toDomain(): Task = Task(
        id = id,
        title = title,
        notes = notes,
        isCompleted = isCompleted,
        listId = listId,
        syncStatus = SyncStatus.SYNCED,
        priority = Priority.entries.firstOrNull { it.value == priority } ?: Priority.NONE,
        startDateTime = startDateTime?.let { Instant.ofEpochMilli(it) },
        endDateTime = endDateTime?.let { Instant.ofEpochMilli(it) },
        isAllDay = isAllDay,
        reminder = if (reminderValue != null && reminderUnit != null && reminderTime != null) {
            ReminderConfig(
                value = reminderValue,
                unit = ReminderUnit.valueOf(reminderUnit),
                remindTime = LocalTime.parse(reminderTime)
            )
        } else null,
        repeat = if (repeatInterval != null && repeatUnit != null) {
            RepeatConfig(
                interval = repeatInterval,
                unit = RepeatUnit.valueOf(repeatUnit),
                daysOfWeek = repeatDaysOfWeek?.split(",")?.map { it.toInt() }?.toSet()
            )
        } else null
    )
}
