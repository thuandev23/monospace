package com.monospace.app.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.monospace.app.core.database.FocusDatabase
import com.monospace.app.core.database.entity.SyncQueueEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncQueueDaoTest {

    private lateinit var db: FocusDatabase
    private lateinit var dao: SyncQueueDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FocusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.syncQueueDao()
    }

    @After
    fun tearDown() { db.close() }

    private fun entity(
        id: String = "sq-1",
        taskId: String = "task-1",
        operation: String = "CREATE",
        payload: String = "task-1",
        retryCount: Int = 0,
        nextRetryAt: Long = 0L,
    ) = SyncQueueEntity(
        id          = id,
        taskId      = taskId,
        operation   = operation,
        payload     = payload,
        retryCount  = retryCount,
        nextRetryAt = nextRetryAt,
    )

    // ─── insert ───────────────────────────────────────────────────────────────

    @Test
    fun insert_persistsEntityRetrievableViaGetReadyItems() = runTest {
        dao.insert(entity(id = "sq-1", nextRetryAt = 0L))

        val items = dao.getReadyItems(now = System.currentTimeMillis(), maxRetryCount = 5)
        assertEquals(1, items.size)
        assertEquals("sq-1", items[0].id)
    }

    @Test
    fun insert_replacesDuplicateId() = runTest {
        dao.insert(entity(id = "sq-1", operation = "CREATE"))
        dao.insert(entity(id = "sq-1", operation = "UPDATE"))

        val items = dao.getReadyItems(now = System.currentTimeMillis(), maxRetryCount = 5)
        assertEquals(1, items.size)
        assertEquals("UPDATE", items[0].operation)
    }

    // ─── getReadyItems ────────────────────────────────────────────────────────

    @Test
    fun getReadyItems_excludesItemsWhoseNextRetryAtIsInFuture() = runTest {
        val now    = System.currentTimeMillis()
        val future = now + 60_000L

        dao.insert(entity(id = "ready",   nextRetryAt = now - 1_000L))
        dao.insert(entity(id = "pending", nextRetryAt = future))

        val items = dao.getReadyItems(now = now, maxRetryCount = 5).map { it.id }
        assertTrue("ready"   in items)
        assertTrue("pending" !in items)
    }

    @Test
    fun getReadyItems_excludesItemsAtOrAboveMaxRetryCount() = runTest {
        val now = System.currentTimeMillis()

        dao.insert(entity(id = "ok",      retryCount = 2, nextRetryAt = 0L))
        dao.insert(entity(id = "maxed",   retryCount = 5, nextRetryAt = 0L))
        dao.insert(entity(id = "over",    retryCount = 9, nextRetryAt = 0L))

        val items = dao.getReadyItems(now = now, maxRetryCount = 5).map { it.id }
        assertTrue("ok"    in items)
        assertTrue("maxed" !in items)
        assertTrue("over"  !in items)
    }

    @Test
    fun getReadyItems_orderedByCreatedAtAscending() = runTest {
        val now = System.currentTimeMillis()

        // Insert in reverse order; createdAt defaults to System.currentTimeMillis() on insert.
        // To have deterministic ordering use explicit createdAt via copy.
        dao.insert(entity(id = "first").copy(createdAt = now - 2_000L, nextRetryAt = 0L))
        dao.insert(entity(id = "third").copy(createdAt = now,          nextRetryAt = 0L))
        dao.insert(entity(id = "second").copy(createdAt = now - 1_000L, nextRetryAt = 0L))

        val ids = dao.getReadyItems(now = now + 1_000L, maxRetryCount = 5).map { it.id }
        assertEquals(listOf("first", "second", "third"), ids)
    }

    @Test
    fun getReadyItems_returnsEmptyListWhenNothingIsReady() = runTest {
        dao.insert(entity(id = "sq-1", nextRetryAt = System.currentTimeMillis() + 60_000L))

        val items = dao.getReadyItems(now = System.currentTimeMillis(), maxRetryCount = 5)
        assertTrue(items.isEmpty())
    }

    // ─── updateForRetry ───────────────────────────────────────────────────────

    @Test
    fun updateForRetry_incrementsRetryCountAndUpdatesNextRetryAt() = runTest {
        dao.insert(entity(id = "sq-1", retryCount = 0, nextRetryAt = 0L))

        val futureRetry = System.currentTimeMillis() + 30_000L
        dao.updateForRetry(id = "sq-1", retryCount = 1, nextRetryAt = futureRetry, error = "timeout")

        // After update the item is no longer ready (nextRetryAt is in the future)
        val ready = dao.getReadyItems(now = System.currentTimeMillis(), maxRetryCount = 5)
        assertTrue(ready.none { it.id == "sq-1" })
    }

    @Test
    fun updateForRetry_savesErrorMessage() = runTest {
        dao.insert(entity(id = "sq-1", nextRetryAt = 0L))

        val futureRetry = System.currentTimeMillis() + 30_000L
        dao.updateForRetry(id = "sq-1", retryCount = 1, nextRetryAt = futureRetry, error = "network error")

        // Bring it back to ready by passing a now far in the future
        val items = dao.getReadyItems(now = futureRetry + 1_000L, maxRetryCount = 5)
        assertEquals("network error", items.first { it.id == "sq-1" }.errorMessage)
    }

    @Test
    fun updateForRetry_withNullError_clearsErrorMessage() = runTest {
        dao.insert(entity(id = "sq-1", nextRetryAt = 0L))
        dao.updateForRetry(id = "sq-1", retryCount = 1, nextRetryAt = 0L, error = null)

        val items = dao.getReadyItems(now = System.currentTimeMillis(), maxRetryCount = 5)
        assertNull(items.first { it.id == "sq-1" }.errorMessage)
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    fun delete_removesEntityFromQueue() = runTest {
        dao.insert(entity(id = "sq-1", nextRetryAt = 0L))
        dao.delete("sq-1")

        val items = dao.getReadyItems(now = System.currentTimeMillis(), maxRetryCount = 5)
        assertTrue(items.none { it.id == "sq-1" })
    }

    @Test
    fun delete_doesNotAffectOtherEntities() = runTest {
        dao.insert(entity(id = "sq-1", nextRetryAt = 0L))
        dao.insert(entity(id = "sq-2", nextRetryAt = 0L))
        dao.delete("sq-1")

        val items = dao.getReadyItems(now = System.currentTimeMillis(), maxRetryCount = 5)
        assertEquals(1, items.size)
        assertEquals("sq-2", items[0].id)
    }

    @Test
    fun delete_onNonExistentId_doesNothing() = runTest {
        dao.insert(entity(id = "sq-1", nextRetryAt = 0L))
        dao.delete("nonexistent")

        val items = dao.getReadyItems(now = System.currentTimeMillis(), maxRetryCount = 5)
        assertEquals(1, items.size)
    }
}
