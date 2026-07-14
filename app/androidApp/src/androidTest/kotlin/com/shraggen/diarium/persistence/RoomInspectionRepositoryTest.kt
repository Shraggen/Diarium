package com.shraggen.diarium.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shraggen.diarium.beekeeping.InspectionDraft
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomInspectionRepositoryTest {

    private lateinit var database: DiariumDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            DiariumDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun writesAndReadsOrderedPersistentRecords() = runTest {
        var clock = 1_000L
        val repository = RoomInspectionRepository(
            inspectionDao = database.inspectionDao(),
            currentTimeMillis = { clock++ },
        )

        repository.record(InspectionDraft(hiveId = " 4 ", queenSeen = true))
        repository.record(InspectionDraft(hiveId = "7", queenSeen = false))

        val newest = RoomInspectionRepository(database.inspectionDao()).recent(limit = 2)

        assertEquals(listOf("7", "4"), newest.map { it.hiveId })
        assertFalse(newest.first().queenSeen)
        assertTrue(newest.last().queenSeen)
        assertEquals(listOf(2L, 1L), newest.map { it.id })
    }
}
