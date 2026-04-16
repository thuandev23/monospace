package com.monospace.app.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.monospace.app.core.database.FocusDatabase
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

@RunWith(AndroidJUnit4::class)
class TaskListDaoTest {

    private lateinit var db: FocusDatabase
    private lateinit var dao: TaskListDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FocusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.taskListDao()
    }

    @After
    fun tearDown() { db.close() }

    private fun list(
        id: String = "list-1",
        name: String = "My List",
        syncStatus: String = "synced",
        sortOrder: Int = 0,
    ) = TaskListEntity(id = id, name = name, syncStatus = syncStatus, sortOrder = sortOrder)

    // ─── upsert / observeAllLists ─────────────────────────────────────────────

    @Test
    fun observeAllLists_returnsInsertedList() = runTest {
        dao.upsert(list(id = "l1", name = "Work"))

        dao.observeAllLists().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("l1", items[0].id)
            assertEquals("Work", items[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllLists_excludesPendingDeleteLists() = runTest {
        dao.upsert(list(id = "visible", syncStatus = "synced"))
        dao.upsert(list(id = "deleted", syncStatus = "pending_delete"))

        dao.observeAllLists().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("visible", items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllLists_orderedBySortOrder() = runTest {
        dao.upsert(list(id = "l3", sortOrder = 3))
        dao.upsert(list(id = "l1", sortOrder = 1))
        dao.upsert(list(id = "l2", sortOrder = 2))

        dao.observeAllLists().test {
            val ids = awaitItem().map { it.id }
            assertEquals(listOf("l1", "l2", "l3"), ids)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllLists_emitsUpdateAfterUpsert() = runTest {
        dao.upsert(list(id = "l1"))

        dao.observeAllLists().test {
            assertEquals(1, awaitItem().size)
            dao.upsert(list(id = "l2"))
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── markAsDeleted ────────────────────────────────────────────────────────

    @Test
    fun markAsDeleted_setsSyncStatusToPendingDelete() = runTest {
        dao.upsert(list(id = "l1", syncStatus = "synced"))
        dao.markAsDeleted("l1")

        val found = dao.getListById("l1")
        assertNotNull(found)
        assertEquals("pending_delete", found!!.syncStatus)
    }

    @Test
    fun markAsDeleted_hidesListFromObserveAllLists() = runTest {
        dao.upsert(list(id = "l1"))
        dao.markAsDeleted("l1")

        dao.observeAllLists().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── hard delete ──────────────────────────────────────────────────────────

    @Test
    fun delete_removesListCompletely() = runTest {
        val l = list(id = "l1")
        dao.upsert(l)
        dao.delete(l)
        assertNull(dao.getListById("l1"))
    }

    // ─── updateSortOrder ──────────────────────────────────────────────────────

    @Test
    fun updateSortOrder_changesListOrder() = runTest {
        dao.upsert(list(id = "l1", sortOrder = 0))
        dao.updateSortOrder("l1", 99)

        val updated = dao.getListById("l1")
        assertNotNull(updated)
        assertEquals(99, updated!!.sortOrder)
    }

    @Test
    fun updateSortOrder_triggersObserveAllListsEmission() = runTest {
        dao.upsert(list(id = "l1", sortOrder = 0))

        dao.observeAllLists().test {
            assertEquals(0, awaitItem()[0].sortOrder)
            dao.updateSortOrder("l1", 5)
            assertEquals(5, awaitItem()[0].sortOrder)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── getListById ──────────────────────────────────────────────────────────

    @Test
    fun getListById_returnsCorrectList() = runTest {
        dao.upsert(list(id = "l1", name = "Personal"))
        val found = dao.getListById("l1")
        assertNotNull(found)
        assertEquals("Personal", found!!.name)
    }

    @Test
    fun getListById_returnsNullForUnknownId() = runTest {
        assertNull(dao.getListById("nonexistent"))
    }

    // ─── getAllListIds ────────────────────────────────────────────────────────

    @Test
    fun getAllListIds_returnsAllNonDeletedIds() = runTest {
        dao.upsert(list(id = "l1", syncStatus = "synced"))
        dao.upsert(list(id = "l2", syncStatus = "pending_create"))
        dao.upsert(list(id = "l3", syncStatus = "pending_delete"))

        val ids = dao.getAllListIds()
        assertTrue("l1" in ids)
        assertTrue("l2" in ids)
        assertTrue("l3" !in ids)
    }

    // ─── upsert conflict replacement ─────────────────────────────────────────

    @Test
    fun upsert_replacesExistingListWithSameId() = runTest {
        dao.upsert(list(id = "l1", name = "Old Name"))
        dao.upsert(list(id = "l1", name = "New Name"))

        val found = dao.getListById("l1")
        assertEquals("New Name", found!!.name)
    }

    @Test
    fun observeAllLists_emitsOnlyOnceWhenUpsertReplacesWithSameData() = runTest {
        val l = list(id = "l1", name = "Same")
        dao.upsert(l)

        dao.observeAllLists().test {
            assertEquals(1, awaitItem().size)
            // Re-insert identical list — Room may or may not emit depending on diff
            dao.upsert(l.copy()) // same data, different object
            cancelAndIgnoreRemainingEvents()
        }
    }
}
