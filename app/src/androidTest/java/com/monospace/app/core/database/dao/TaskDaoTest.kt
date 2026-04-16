package com.monospace.app.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.monospace.app.core.database.FocusDatabase
import com.monospace.app.core.database.entity.TaskEntity
import com.monospace.app.core.database.entity.TaskListEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var db: FocusDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var taskListDao: TaskListDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FocusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao    = db.taskDao()
        taskListDao = db.taskListDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private suspend fun insertList(id: String = "list-1"): TaskListEntity {
        val list = TaskListEntity(id = id, name = "Test List")
        taskListDao.upsert(list)
        return list
    }

    private fun entity(
        id: String = UUID.randomUUID().toString(),
        listId: String = "list-1",
        taskStatus: String = "NOT_DONE",
        syncStatus: String = "pending_create",
        startDateTime: Long? = null,
        reminderValue: Int? = null,
    ) = TaskEntity(
        id            = id,
        title         = "Task $id",
        listId        = listId,
        taskStatus    = taskStatus,
        syncStatus    = syncStatus,
        startDateTime = startDateTime,
        reminderUnit  = if (reminderValue != null) "DAY" else null,
        reminderValue = reminderValue,
        reminderTime  = if (reminderValue != null) "09:00" else null,
    )

    private fun todayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ─── upsert / observeTasksByList ──────────────────────────────────────────

    @Test
    fun observeTasksByList_returnsInsertedTask() = runTest {
        insertList()
        val task = entity()
        taskDao.upsert(task)

        taskDao.observeTasksByList("list-1").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(task.id, items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTasksByList_excludesPendingDeleteTasks() = runTest {
        insertList()
        taskDao.upsert(entity(id = "visible",  syncStatus = "pending_create"))
        taskDao.upsert(entity(id = "deleted",  syncStatus = "pending_delete"))

        taskDao.observeTasksByList("list-1").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("visible", items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTasksByList_onlyReturnsTasksFromRequestedList() = runTest {
        insertList("list-1")
        insertList("list-2")
        taskDao.upsert(entity(id = "t1", listId = "list-1"))
        taskDao.upsert(entity(id = "t2", listId = "list-2"))

        taskDao.observeTasksByList("list-1").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("t1", items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTasksByList_emitsUpdateAfterUpsert() = runTest {
        insertList()
        taskDao.upsert(entity(id = "t1"))

        taskDao.observeTasksByList("list-1").test {
            assertEquals(1, awaitItem().size)
            taskDao.upsert(entity(id = "t2"))
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── markAsDeleted / hardDelete ───────────────────────────────────────────

    @Test
    fun markAsDeleted_setsSyncStatusToPendingDelete() = runTest {
        insertList()
        val task = entity(id = "t1")
        taskDao.upsert(task)
        taskDao.markAsDeleted("t1")

        val found = taskDao.getTaskById("t1")
        assertNotNull(found)
        assertEquals("pending_delete", found!!.syncStatus)
    }

    @Test
    fun hardDelete_removesTaskCompletely() = runTest {
        insertList()
        taskDao.upsert(entity(id = "t1"))
        taskDao.hardDelete("t1")
        assertNull(taskDao.getTaskById("t1"))
    }

    // ─── updateTaskStatus ─────────────────────────────────────────────────────

    @Test
    fun updateTaskStatus_changesStatusAndSetsSyncStatusToPendingUpdate() = runTest {
        insertList()
        taskDao.upsert(entity(id = "t1", taskStatus = "NOT_DONE", syncStatus = "synced"))
        taskDao.updateTaskStatus("t1", "DONE")

        val updated = taskDao.getTaskById("t1")!!
        assertEquals("DONE", updated.taskStatus)
        assertEquals("pending_update", updated.syncStatus)
    }

    @Test
    fun updateTaskStatus_triggersObserveTasksByListEmission() = runTest {
        insertList()
        taskDao.upsert(entity(id = "t1", taskStatus = "NOT_DONE"))

        taskDao.observeTasksByList("list-1").test {
            assertEquals("NOT_DONE", awaitItem()[0].taskStatus)
            taskDao.updateTaskStatus("t1", "DONE")
            assertEquals("DONE", awaitItem()[0].taskStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── markAsSynced ─────────────────────────────────────────────────────────

    @Test
    fun markAsSynced_setsSyncStatusToSynced() = runTest {
        insertList()
        taskDao.upsert(entity(id = "t1", syncStatus = "pending_create"))
        taskDao.markAsSynced("t1")
        assertEquals("synced", taskDao.getTaskById("t1")!!.syncStatus)
    }

    // ─── getPendingTasks ──────────────────────────────────────────────────────

    @Test
    fun getPendingTasks_returnsOnlyNonSyncedTasks() = runTest {
        insertList()
        taskDao.upsert(entity(id = "synced",  syncStatus = "synced"))
        taskDao.upsert(entity(id = "create",  syncStatus = "pending_create"))
        taskDao.upsert(entity(id = "update",  syncStatus = "pending_update"))
        taskDao.upsert(entity(id = "delete",  syncStatus = "pending_delete"))

        val pending = taskDao.getPendingTasks().map { it.id }
        assertTrue("create" in pending)
        assertTrue("update" in pending)
        assertTrue("delete" in pending)
        assertTrue("synced" !in pending)
    }

    // ─── observeAllActiveTaskCount ────────────────────────────────────────────

    @Test
    fun observeAllActiveTaskCount_excludesDoneAndCancelledTasks() = runTest {
        insertList()
        taskDao.upsert(entity(id = "not_done",   taskStatus = "NOT_DONE"))
        taskDao.upsert(entity(id = "in_progress", taskStatus = "IN_PROGRESS"))
        taskDao.upsert(entity(id = "done",        taskStatus = "DONE"))
        taskDao.upsert(entity(id = "cancelled",   taskStatus = "CANCELLED"))

        taskDao.observeAllActiveTaskCount().test {
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllActiveTaskCount_excludesPendingDeleteTasks() = runTest {
        insertList()
        taskDao.upsert(entity(id = "active",  taskStatus = "NOT_DONE", syncStatus = "synced"))
        taskDao.upsert(entity(id = "deleted", taskStatus = "NOT_DONE", syncStatus = "pending_delete"))

        taskDao.observeAllActiveTaskCount().test {
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── observeTodayTaskCount ────────────────────────────────────────────────

    @Test
    fun observeTodayTaskCount_countsOnlyTasksScheduledForToday() = runTest {
        insertList()
        val dayStart = todayMillis()
        val dayEnd   = dayStart + 86_400_000L

        taskDao.upsert(entity(id = "today",     startDateTime = dayStart + 3_600_000L))
        taskDao.upsert(entity(id = "yesterday", startDateTime = dayStart - 1_000L))
        taskDao.upsert(entity(id = "tomorrow",  startDateTime = dayEnd + 1_000L))
        taskDao.upsert(entity(id = "no_date"))

        taskDao.observeTodayTaskCount(dayStart, dayEnd).test {
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTodayTaskCount_excludesDoneTasks() = runTest {
        insertList()
        val dayStart = todayMillis()
        val dayEnd   = dayStart + 86_400_000L

        taskDao.upsert(entity(id = "active", taskStatus = "NOT_DONE", startDateTime = dayStart + 1_000L))
        taskDao.upsert(entity(id = "done",   taskStatus = "DONE",     startDateTime = dayStart + 2_000L))

        taskDao.observeTodayTaskCount(dayStart, dayEnd).test {
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── getTasksWithFutureReminders ──────────────────────────────────────────

    @Test
    fun getTasksWithFutureReminders_returnsOnlyFutureTasksWithReminders() = runTest {
        insertList()
        val nowMs  = System.currentTimeMillis()
        val future = nowMs + 86_400_000L
        val past   = nowMs - 86_400_000L

        taskDao.upsert(entity(id = "future_with_reminder", startDateTime = future, reminderValue = 1))
        taskDao.upsert(entity(id = "past_with_reminder",   startDateTime = past,   reminderValue = 1))
        taskDao.upsert(entity(id = "future_no_reminder",   startDateTime = future))

        val result = taskDao.getTasksWithFutureReminders(nowMs).map { it.id }
        assertTrue("future_with_reminder" in result)
        assertTrue("past_with_reminder"  !in result)
        assertTrue("future_no_reminder"  !in result)
    }

    @Test
    fun getTasksWithFutureReminders_excludesDoneAndCancelledTasks() = runTest {
        insertList()
        val future = System.currentTimeMillis() + 86_400_000L

        taskDao.upsert(entity(id = "not_done",    taskStatus = "NOT_DONE",    startDateTime = future, reminderValue = 1))
        taskDao.upsert(entity(id = "done",        taskStatus = "DONE",        startDateTime = future, reminderValue = 1))
        taskDao.upsert(entity(id = "cancelled",   taskStatus = "CANCELLED",   startDateTime = future, reminderValue = 1))
        taskDao.upsert(entity(id = "in_progress", taskStatus = "IN_PROGRESS", startDateTime = future, reminderValue = 1))

        val result = taskDao.getTasksWithFutureReminders(System.currentTimeMillis()).map { it.id }
        assertTrue("not_done"    in result)
        assertTrue("in_progress" in result)
        assertTrue("done"       !in result)
        assertTrue("cancelled"  !in result)
    }

    // ─── observeAllTasksSortedByDate ──────────────────────────────────────────

    @Test
    fun observeAllTasksSortedByDate_putsNullDateTasksLast() = runTest {
        insertList()
        val nowMs = System.currentTimeMillis()

        taskDao.upsert(entity(id = "no_date",  startDateTime = null))
        taskDao.upsert(entity(id = "has_date", startDateTime = nowMs))

        taskDao.observeAllTasksSortedByDate().test {
            val ids = awaitItem().map { it.id }
            assertEquals("has_date", ids.first())
            assertEquals("no_date",  ids.last())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllTasksSortedByDate_putsDoneTasksAfterActiveTasks() = runTest {
        insertList()
        val nowMs = System.currentTimeMillis()

        taskDao.upsert(entity(id = "done",     taskStatus = "DONE",     startDateTime = nowMs))
        taskDao.upsert(entity(id = "not_done", taskStatus = "NOT_DONE", startDateTime = nowMs))

        taskDao.observeAllTasksSortedByDate().test {
            val ids = awaitItem().map { it.id }
            assertEquals("not_done", ids.first())
            assertEquals("done",     ids.last())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
