package com.monospace.app.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.monospace.app.core.database.FocusDatabase
import com.monospace.app.core.database.entity.FocusProfileEntity
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
class FocusProfileDaoTest {

    private lateinit var db: FocusDatabase
    private lateinit var dao: FocusProfileDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FocusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.focusProfileDao()
    }

    @After
    fun tearDown() { db.close() }

    private fun profile(
        id: String = "fp-1",
        name: String = "Work",
        isActive: Boolean = false,
        linkedListId: String? = null,
    ) = FocusProfileEntity(
        id            = id,
        name          = name,
        allowedAppIds = "",
        linkedListId  = linkedListId,
        isActive      = isActive,
    )

    // ─── upsert / observeAll ──────────────────────────────────────────────────

    @Test
    fun observeAll_returnsInsertedProfile() = runTest {
        dao.upsert(profile(id = "fp-1", name = "Work"))

        dao.observeAll().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("fp-1", items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAll_orderedByNameAscending() = runTest {
        dao.upsert(profile(id = "fp-c", name = "Chill"))
        dao.upsert(profile(id = "fp-a", name = "Art"))
        dao.upsert(profile(id = "fp-b", name = "Bike"))

        dao.observeAll().test {
            val names = awaitItem().map { it.name }
            assertEquals(listOf("Art", "Bike", "Chill"), names)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAll_emitsUpdateAfterUpsert() = runTest {
        dao.upsert(profile(id = "fp-1"))

        dao.observeAll().test {
            assertEquals(1, awaitItem().size)
            dao.upsert(profile(id = "fp-2", name = "Study"))
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsert_replacesExistingProfileWithSameId() = runTest {
        dao.upsert(profile(id = "fp-1", name = "Old"))
        dao.upsert(profile(id = "fp-1", name = "New"))

        val found = dao.getById("fp-1")
        assertEquals("New", found!!.name)
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    fun getById_returnsCorrectProfile() = runTest {
        dao.upsert(profile(id = "fp-1", name = "Focus"))
        val found = dao.getById("fp-1")
        assertNotNull(found)
        assertEquals("Focus", found!!.name)
    }

    @Test
    fun getById_returnsNullForUnknownId() = runTest {
        assertNull(dao.getById("nonexistent"))
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    fun delete_removesProfileCompletely() = runTest {
        dao.upsert(profile(id = "fp-1"))
        dao.delete("fp-1")
        assertNull(dao.getById("fp-1"))
    }

    @Test
    fun delete_triggersObserveAllEmission() = runTest {
        dao.upsert(profile(id = "fp-1"))

        dao.observeAll().test {
            assertEquals(1, awaitItem().size)
            dao.delete("fp-1")
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun delete_doesNotAffectOtherProfiles() = runTest {
        dao.upsert(profile(id = "fp-1", name = "Keep"))
        dao.upsert(profile(id = "fp-2", name = "Remove"))
        dao.delete("fp-2")

        val found = dao.getById("fp-1")
        assertNotNull(found)
    }

    // ─── deactivateAll / activate ─────────────────────────────────────────────

    @Test
    fun deactivateAll_setsAllProfilesToInactive() = runTest {
        dao.upsert(profile(id = "fp-1", isActive = true))
        dao.upsert(profile(id = "fp-2", isActive = true))
        dao.deactivateAll()

        assertNull(dao.getById("fp-1")?.isActive?.takeIf { it })
        assertNull(dao.getById("fp-2")?.isActive?.takeIf { it })
    }

    @Test
    fun activate_setsTargetProfileToActive() = runTest {
        dao.upsert(profile(id = "fp-1", isActive = false))
        dao.activate("fp-1")

        assertTrue(dao.getById("fp-1")!!.isActive)
    }

    @Test
    fun activate_afterDeactivateAll_makesOnlyOneProfileActive() = runTest {
        dao.upsert(profile(id = "fp-1", isActive = true))
        dao.upsert(profile(id = "fp-2", isActive = true))
        dao.deactivateAll()
        dao.activate("fp-1")

        assertTrue(dao.getById("fp-1")!!.isActive)
        assertTrue(!dao.getById("fp-2")!!.isActive)
    }

    // ─── observeActive ────────────────────────────────────────────────────────

    @Test
    fun observeActive_returnsActiveProfile() = runTest {
        dao.upsert(profile(id = "fp-1", isActive = false))
        dao.upsert(profile(id = "fp-2", isActive = true))

        dao.observeActive().test {
            val active = awaitItem()
            assertNotNull(active)
            assertEquals("fp-2", active!!.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeActive_returnsNullWhenNoActiveProfile() = runTest {
        dao.upsert(profile(id = "fp-1", isActive = false))

        dao.observeActive().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeActive_emitsUpdateWhenActiveProfileChanges() = runTest {
        dao.upsert(profile(id = "fp-1", isActive = false))
        dao.upsert(profile(id = "fp-2", isActive = false))

        dao.observeActive().test {
            assertNull(awaitItem())
            dao.activate("fp-1")
            assertEquals("fp-1", awaitItem()!!.id)
            dao.deactivateAll()
            dao.activate("fp-2")
            assertEquals("fp-2", awaitItem()!!.id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
