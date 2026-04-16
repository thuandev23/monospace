package com.monospace.app.core.domain.usecase

import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.RepeatUnit
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.sync.ReminderScheduler
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

class ExpandRepeatTaskUseCaseTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private val syncQueue: SyncQueue = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)

    private val useCase = ExpandRepeatTaskUseCase(repository, syncQueue, reminderScheduler)

    // 2024-03-06 09:00 UTC = Wednesday, dayOfWeek ISO value = 3
    private val wednesday: Instant =
        ZonedDateTime.of(2024, 3, 6, 9, 0, 0, 0, ZoneId.of("UTC")).toInstant()

    // 2024-03-08 09:00 UTC = Friday, dayOfWeek ISO value = 5
    private val friday: Instant =
        ZonedDateTime.of(2024, 3, 8, 9, 0, 0, 0, ZoneId.of("UTC")).toInstant()

    @Before
    fun setUp() {
        // Pin timezone to UTC so ZoneId.systemDefault() inside the use case is deterministic
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    private fun taskWith(
        startDateTime: Instant? = wednesday,
        endDateTime: Instant? = null,
        repeat: RepeatConfig? = RepeatConfig(1, RepeatUnit.DAY),
    ) = Task(
        id            = "task-original",
        title         = "Test",
        listId        = "list-1",
        status        = TaskStatus.DONE,
        startDateTime = startDateTime,
        endDateTime   = endDateTime,
        repeat        = repeat,
    )

    // ─── Guard clauses ────────────────────────────────────────────────────────

    @Test
    fun `does nothing when repeat is null`() = runTest {
        useCase(taskWith(repeat = null))
        coVerify(exactly = 0) { repository.saveTask(any()) }
    }

    @Test
    fun `does nothing when startDateTime is null`() = runTest {
        useCase(taskWith(startDateTime = null))
        coVerify(exactly = 0) { repository.saveTask(any()) }
    }

    // ─── DAY repeat ───────────────────────────────────────────────────────────

    @Test
    fun `DAY repeat interval 1 schedules next task 1 day later`() = runTest {
        useCase(taskWith(repeat = RepeatConfig(1, RepeatUnit.DAY)))
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(wednesday.atZone(ZoneId.of("UTC")).plusDays(1).toInstant(), slot.captured.startDateTime)
    }

    @Test
    fun `DAY repeat interval 3 schedules next task 3 days later`() = runTest {
        useCase(taskWith(repeat = RepeatConfig(3, RepeatUnit.DAY)))
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(wednesday.atZone(ZoneId.of("UTC")).plusDays(3).toInstant(), slot.captured.startDateTime)
    }

    // ─── MONTH repeat ─────────────────────────────────────────────────────────

    @Test
    fun `MONTH repeat schedules next task 1 month later`() = runTest {
        useCase(taskWith(repeat = RepeatConfig(1, RepeatUnit.MONTH)))
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(wednesday.atZone(ZoneId.of("UTC")).plusMonths(1).toInstant(), slot.captured.startDateTime)
    }

    // ─── YEAR repeat ──────────────────────────────────────────────────────────

    @Test
    fun `YEAR repeat schedules next task 1 year later`() = runTest {
        useCase(taskWith(repeat = RepeatConfig(1, RepeatUnit.YEAR)))
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(wednesday.atZone(ZoneId.of("UTC")).plusYears(1).toInstant(), slot.captured.startDateTime)
    }

    // ─── WEEK repeat — no daysOfWeek ──────────────────────────────────────────

    @Test
    fun `WEEK repeat without daysOfWeek schedules next task 1 week later`() = runTest {
        useCase(taskWith(repeat = RepeatConfig(1, RepeatUnit.WEEK, null)))
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(wednesday.atZone(ZoneId.of("UTC")).plusWeeks(1).toInstant(), slot.captured.startDateTime)
    }

    @Test
    fun `WEEK repeat interval 2 without daysOfWeek schedules next task 2 weeks later`() = runTest {
        useCase(taskWith(repeat = RepeatConfig(2, RepeatUnit.WEEK, null)))
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(wednesday.atZone(ZoneId.of("UTC")).plusWeeks(2).toInstant(), slot.captured.startDateTime)
    }

    // ─── WEEK repeat — with daysOfWeek ────────────────────────────────────────

    @Test
    fun `WEEK repeat with daysOfWeek picks next selected day in the same week`() = runTest {
        // Wednesday (3) → next selected day = Friday (5), +2 days
        useCase(taskWith(repeat = RepeatConfig(1, RepeatUnit.WEEK, setOf(1, 3, 5))))
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        val expected = wednesday.atZone(ZoneId.of("UTC")).plusDays(2).toInstant() // Friday
        assertEquals(expected, slot.captured.startDateTime)
    }

    @Test
    fun `WEEK repeat with daysOfWeek wraps to Monday of next week when Friday is last selected day`() = runTest {
        // Friday (5) → no selected day after 5 → next week Monday (1)
        // Friday + 7 days - 4 days = +3 days = Monday
        useCase(taskWith(startDateTime = friday, repeat = RepeatConfig(1, RepeatUnit.WEEK, setOf(1, 3, 5))))
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        val expected = friday.atZone(ZoneId.of("UTC")).plusDays(3).toInstant() // Monday
        assertEquals(expected, slot.captured.startDateTime)
    }

    // ─── New task properties ──────────────────────────────────────────────────

    @Test
    fun `new task gets a UUID different from the completed task`() = runTest {
        useCase(taskWith())
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertNotEquals("task-original", slot.captured.id)
    }

    @Test
    fun `new task status is NOT_DONE`() = runTest {
        useCase(taskWith())
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(TaskStatus.NOT_DONE, slot.captured.status)
    }

    @Test
    fun `new task syncStatus is PENDING_CREATE`() = runTest {
        useCase(taskWith())
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(SyncStatus.PENDING_CREATE, slot.captured.syncStatus)
    }

    // ─── Duration preservation ────────────────────────────────────────────────

    @Test
    fun `duration between start and end is preserved in next task`() = runTest {
        val end          = wednesday.plusSeconds(3_600) // 1-hour duration
        val originalMs   = end.toEpochMilli() - wednesday.toEpochMilli()
        useCase(taskWith(endDateTime = end, repeat = RepeatConfig(1, RepeatUnit.DAY)))

        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        val newMs = slot.captured.endDateTime!!.toEpochMilli() - slot.captured.startDateTime!!.toEpochMilli()
        assertEquals(originalMs, newMs)
    }

    @Test
    fun `endDateTime of new task is null when original had no endDateTime`() = runTest {
        useCase(taskWith(endDateTime = null, repeat = RepeatConfig(1, RepeatUnit.DAY)))
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertNull(slot.captured.endDateTime)
    }

    // ─── Side effects ─────────────────────────────────────────────────────────

    @Test
    fun `syncQueue is called with CREATE for the new task id`() = runTest {
        useCase(taskWith())
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        coVerify { syncQueue.enqueue(slot.captured.id, SyncOperationType.CREATE, slot.captured.id) }
    }
}
