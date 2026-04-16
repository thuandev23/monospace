package com.monospace.app.core.data.mapper

import com.monospace.app.core.database.entity.TaskEntity
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.ReminderUnit
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.RepeatUnit
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

class TaskMapperTest {

    // ─── helpers ──────────────────────────────────────────────────────────────

    private fun minimalEntity(
        taskStatus: String = "NOT_DONE",
        priority: Int = 0,
        syncStatus: String = "synced",
    ) = TaskEntity(
        id = "id-1",
        title = "Test Task",
        listId = "list-1",
        taskStatus = taskStatus,
        priority = priority,
        syncStatus = syncStatus,
    )

    private fun minimalTask(
        status: TaskStatus = TaskStatus.NOT_DONE,
        priority: Priority = Priority.NONE,
        syncStatus: SyncStatus = SyncStatus.SYNCED,
    ) = Task(
        id = "id-1",
        title = "Test Task",
        listId = "list-1",
        status = status,
        priority = priority,
        syncStatus = syncStatus,
        startDateTime = null,
        endDateTime = null,
    )

    // ─── toDomain: TaskStatus ──────────────────────────────────────────────────

    @Test
    fun `toDomain maps NOT_DONE correctly`() =
        assertEquals(TaskStatus.NOT_DONE, minimalEntity("NOT_DONE").toDomain().status)

    @Test
    fun `toDomain maps IN_PROGRESS correctly`() =
        assertEquals(TaskStatus.IN_PROGRESS, minimalEntity("IN_PROGRESS").toDomain().status)

    @Test
    fun `toDomain maps CANCELLED correctly`() =
        assertEquals(TaskStatus.CANCELLED, minimalEntity("CANCELLED").toDomain().status)

    @Test
    fun `toDomain maps DONE correctly`() =
        assertEquals(TaskStatus.DONE, minimalEntity("DONE").toDomain().status)

    @Test
    fun `toDomain defaults unknown task_status to NOT_DONE`() =
        assertEquals(TaskStatus.NOT_DONE, minimalEntity("GARBAGE").toDomain().status)

    // ─── toDomain: Priority ────────────────────────────────────────────────────

    @Test
    fun `toDomain maps all priority int values correctly`() {
        assertEquals(Priority.NONE,   minimalEntity(priority = 0).toDomain().priority)
        assertEquals(Priority.LOW,    minimalEntity(priority = 1).toDomain().priority)
        assertEquals(Priority.MEDIUM, minimalEntity(priority = 2).toDomain().priority)
        assertEquals(Priority.HIGH,   minimalEntity(priority = 3).toDomain().priority)
    }

    @Test
    fun `toDomain defaults unknown priority to NONE`() =
        assertEquals(Priority.NONE, minimalEntity(priority = 99).toDomain().priority)

    // ─── toDomain: SyncStatus ──────────────────────────────────────────────────

    @Test
    fun `toDomain maps all syncStatus strings correctly`() {
        assertEquals(SyncStatus.SYNCED,         minimalEntity(syncStatus = "synced").toDomain().syncStatus)
        assertEquals(SyncStatus.PENDING_CREATE,  minimalEntity(syncStatus = "pending_create").toDomain().syncStatus)
        assertEquals(SyncStatus.PENDING_UPDATE,  minimalEntity(syncStatus = "pending_update").toDomain().syncStatus)
        assertEquals(SyncStatus.PENDING_DELETE,  minimalEntity(syncStatus = "pending_delete").toDomain().syncStatus)
    }

    @Test
    fun `toDomain defaults unknown syncStatus to SYNCED`() =
        assertEquals(SyncStatus.SYNCED, minimalEntity(syncStatus = "unknown").toDomain().syncStatus)

    // ─── toDomain: DateTime ────────────────────────────────────────────────────

    @Test
    fun `toDomain maps startDateTime and endDateTime millis to Instant`() {
        val startMs = 1_700_000_000_000L
        val endMs   = 1_700_003_600_000L
        val entity  = minimalEntity().copy(startDateTime = startMs, endDateTime = endMs)
        val task    = entity.toDomain()
        assertEquals(Instant.ofEpochMilli(startMs), task.startDateTime)
        assertEquals(Instant.ofEpochMilli(endMs),   task.endDateTime)
    }

    @Test
    fun `toDomain maps null startDateTime to null`() =
        assertNull(minimalEntity().toDomain().startDateTime)

    // ─── toDomain: ReminderConfig ──────────────────────────────────────────────

    @Test
    fun `toDomain maps null reminder fields to null reminder`() =
        assertNull(minimalEntity().toDomain().reminder)

    @Test
    fun `toDomain maps reminder config correctly`() {
        val entity   = minimalEntity().copy(reminderValue = 2, reminderUnit = "DAY", reminderTime = "09:00")
        val reminder = entity.toDomain().reminder!!
        assertEquals(2, reminder.value)
        assertEquals(ReminderUnit.DAY, reminder.unit)
        assertEquals(LocalTime.of(9, 0), reminder.remindTime)
    }

    @Test
    fun `toDomain maps all ReminderUnit values`() {
        ReminderUnit.entries.forEach { unit ->
            val entity = minimalEntity().copy(reminderValue = 1, reminderUnit = unit.name, reminderTime = "08:00")
            assertEquals(unit, entity.toDomain().reminder!!.unit)
        }
    }

    // ─── toDomain: RepeatConfig ────────────────────────────────────────────────

    @Test
    fun `toDomain maps null repeat fields to null repeat`() =
        assertNull(minimalEntity().toDomain().repeat)

    @Test
    fun `toDomain maps repeat config without daysOfWeek`() {
        val entity = minimalEntity().copy(repeatInterval = 3, repeatUnit = "WEEK")
        val repeat = entity.toDomain().repeat!!
        assertEquals(3, repeat.interval)
        assertEquals(RepeatUnit.WEEK, repeat.unit)
        assertNull(repeat.daysOfWeek)
    }

    @Test
    fun `toDomain maps repeat daysOfWeek csv to Set of Int`() {
        val entity = minimalEntity().copy(repeatInterval = 1, repeatUnit = "WEEK", repeatDaysOfWeek = "1,3,5")
        assertEquals(setOf(1, 3, 5), entity.toDomain().repeat!!.daysOfWeek)
    }

    @Test
    fun `toDomain maps all RepeatUnit values`() {
        RepeatUnit.entries.forEach { unit ->
            val entity = minimalEntity().copy(repeatInterval = 1, repeatUnit = unit.name)
            assertEquals(unit, entity.toDomain().repeat!!.unit)
        }
    }

    // ─── toEntity ─────────────────────────────────────────────────────────────

    @Test
    fun `toEntity stores taskStatus as enum name string`() {
        TaskStatus.entries.forEach { status ->
            assertEquals(status.name, minimalTask(status = status).toEntity().taskStatus)
        }
    }

    @Test
    fun `toEntity stores priority as int value`() {
        Priority.entries.forEach { priority ->
            assertEquals(priority.value, minimalTask(priority = priority).toEntity().priority)
        }
    }

    @Test
    fun `toEntity stores syncStatus as correct string`() {
        val expected = mapOf(
            SyncStatus.SYNCED         to "synced",
            SyncStatus.PENDING_CREATE to "pending_create",
            SyncStatus.PENDING_UPDATE to "pending_update",
            SyncStatus.PENDING_DELETE to "pending_delete",
        )
        expected.forEach { (status, str) ->
            assertEquals(str, minimalTask(syncStatus = status).toEntity().syncStatus)
        }
    }

    @Test
    fun `toEntity stores daysOfWeek as parseable comma-separated string`() {
        val task   = minimalTask().copy(repeat = RepeatConfig(1, RepeatUnit.WEEK, setOf(1, 3, 5)))
        val stored = task.toEntity().repeatDaysOfWeek!!.split(",").map { it.toInt() }.toSet()
        assertEquals(setOf(1, 3, 5), stored)
    }

    // ─── Roundtrip ────────────────────────────────────────────────────────────

    @Test
    fun `toEntity then toDomain roundtrip preserves all fields`() {
        val original = Task(
            id            = "task-rt",
            title         = "Roundtrip",
            notes         = "Some notes",
            status        = TaskStatus.IN_PROGRESS,
            priority      = Priority.HIGH,
            listId        = "list-1",
            syncStatus    = SyncStatus.PENDING_UPDATE,
            startDateTime = Instant.ofEpochMilli(1_700_000_000_000L),
            endDateTime   = Instant.ofEpochMilli(1_700_003_600_000L),
            isAllDay      = false,
            reminder      = ReminderConfig(1, ReminderUnit.DAY, LocalTime.of(8, 0)),
            repeat        = RepeatConfig(1, RepeatUnit.WEEK, setOf(1, 3, 5)),
        )
        val restored = original.toEntity().toDomain()

        assertEquals(original.id,            restored.id)
        assertEquals(original.title,         restored.title)
        assertEquals(original.notes,         restored.notes)
        assertEquals(original.status,        restored.status)
        assertEquals(original.priority,      restored.priority)
        assertEquals(original.syncStatus,    restored.syncStatus)
        assertEquals(original.startDateTime, restored.startDateTime)
        assertEquals(original.endDateTime,   restored.endDateTime)
        assertEquals(original.isAllDay,      restored.isAllDay)
        assertEquals(original.reminder,      restored.reminder)
        assertEquals(original.repeat,        restored.repeat)
    }
}
