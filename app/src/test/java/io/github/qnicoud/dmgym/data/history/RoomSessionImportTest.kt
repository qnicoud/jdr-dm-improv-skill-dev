package io.github.qnicoud.dmgym.data.history

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.core.backup.BACKUP_NOW
import io.github.qnicoud.dmgym.core.backup.BackupDecoder
import io.github.qnicoud.dmgym.core.backup.BackupEncoder
import io.github.qnicoud.dmgym.core.backup.DecodeResult
import io.github.qnicoud.dmgym.core.backup.KNOWN_EXERCISES
import io.github.qnicoud.dmgym.core.backup.START_2026_10_17
import io.github.qnicoud.dmgym.core.backup.fullSession
import io.github.qnicoud.dmgym.core.backup.withSession
import io.github.qnicoud.dmgym.core.backup.withoutIds
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.identity
import io.github.qnicoud.dmgym.data.history.db.DmGymDatabase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RoomSessionImportTest {
    private lateinit var source: DmGymDatabase
    private lateinit var target: DmGymDatabase
    private lateinit var sourceRepo: RoomHistoryRepository
    private lateinit var targetRepo: RoomHistoryRepository

    private val a = fullSession(START_2026_10_17)
    private val b = fullSession(START_2026_10_17 + 60_000, "constraint_box")

    private fun open(): Pair<DmGymDatabase, RoomHistoryRepository> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder<DmGymDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
        val ticks = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }
        val clock = Clock.fixed(Instant.parse("2026-10-18T08:00:00Z"), ZoneOffset.UTC)
        return db to RoomHistoryRepository(db.sessionDao(), clock, ticks)
    }

    @Before
    fun setUp() {
        open().let { (db, repo) ->
            source = db
            sourceRepo = repo
        }
        open().let { (db, repo) ->
            target = db
            targetRepo = repo
        }
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun `export then import into an empty database gives an identical database`() =
        runTest {
            sourceRepo.importSessions(listOf(a, b))
            val before = sourceRepo.readAll()
            val text = BackupEncoder.encode(before, BACKUP_NOW, "1.1.0")
            val decoded = BackupDecoder.decode(text, KNOWN_EXERCISES, BACKUP_NOW) as DecodeResult.Success

            val result = targetRepo.importSessions(decoded.sessions)

            assertEquals(2, result.insertedIds.size)
            assertEquals(before.withoutIds(), targetRepo.readAll().withoutIds())
            assertEquals(before.withoutIds(), listOf(a, b).withoutIds())
        }

    @Test
    fun `imported sessions get new local ids`() =
        runTest {
            targetRepo.importSessions(listOf(a.withSession { it.copy(startedAtEpochMillis = 5, id = 77) }))

            val ids = targetRepo.readAll().map { it.session.id }

            assertTrue(77L !in ids)
            assertEquals(1, ids.size)
        }

    @Test
    fun `importing the same file twice adds nothing the second time`() =
        runTest {
            targetRepo.importSessions(listOf(a, b))

            val second = targetRepo.importSessions(listOf(a, b))

            assertTrue(second.insertedIds.isEmpty())
            assertEquals(2, second.alreadyPresent)
            assertEquals(2, targetRepo.readAll().size)
        }

    @Test
    fun `the session already present wins`() =
        runTest {
            targetRepo.importSessions(listOf(a))

            targetRepo.importSessions(listOf(a.withSession { it.copy(selfRating = 1, isGem = false) }))

            assertEquals(
                3,
                targetRepo
                    .readAll()
                    .single()
                    .session.selfRating,
            )
        }

    @Test
    fun `a duplicate inside the batch is inserted once`() =
        runTest {
            val result = targetRepo.importSessions(listOf(a, a))

            assertEquals(1, result.insertedIds.size)
            assertEquals(1, result.alreadyPresent)
        }

    @Test
    fun `a session created after the preview is not duplicated`() =
        runTest {
            val preview = targetRepo.identities()
            assertTrue(preview.isEmpty())
            targetRepo.importSessions(listOf(a))

            val result = targetRepo.importSessions(listOf(a, b))

            assertEquals(1, result.insertedIds.size)
            assertEquals(1, result.alreadyPresent)
            assertEquals(2, targetRepo.readAll().size)
        }

    @Test
    fun `an error in the middle leaves the database unchanged`() =
        runTest {
            targetRepo.importSessions(listOf(a))
            val before = targetRepo.readAll()
            val broken = b.copy(draws = b.draws + b.draws.first())

            assertThrows(Exception::class.java) {
                kotlinx.coroutines.runBlocking { targetRepo.importSessions(listOf(fullSession(1_000), broken)) }
            }

            assertEquals(before, targetRepo.readAll())
        }

    @Test
    fun `undo removes exactly the imported sessions and their content`() =
        runTest {
            targetRepo.importSessions(listOf(a))
            val imported = targetRepo.importSessions(listOf(a, b))

            targetRepo.deleteSessions(imported.insertedIds)

            assertEquals(listOf(a.session.identity), targetRepo.readAll().map { it.session.identity })
            assertEquals(listOf(a).withoutIds(), targetRepo.readAll().withoutIds())
        }

    @Test
    fun `undo of a large import works in chunks`() =
        runTest {
            val many: List<SessionDetail> = List(1_200) { fullSession(START_2026_10_17 + 10_000L + it) }
            val ids = targetRepo.importSessions(many).insertedIds

            targetRepo.deleteSessions(ids)

            assertTrue(targetRepo.readAll().isEmpty())
        }

    @Test
    fun `identities lists exercise and start of every session`() =
        runTest {
            targetRepo.importSessions(listOf(a, b))

            assertEquals(setOf(a.session.identity, b.session.identity), targetRepo.identities())
        }
}
