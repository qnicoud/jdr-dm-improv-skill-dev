package io.github.qnicoud.dmgym.data.history

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.data.history.db.DmGymDatabase
import io.github.qnicoud.dmgym.data.history.db.truncateWal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RoomSessionArchiveTest {
    private lateinit var database: DmGymDatabase
    private lateinit var repo: RoomHistoryRepository
    private var checkpoints = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<DmGymDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
        val ticks = MutableSharedFlow<Unit>(replay = 1).also { it.tryEmit(Unit) }
        repo =
            RoomHistoryRepository(
                database.sessionDao(),
                Clock.fixed(Instant.parse("2026-10-17T08:00:00Z"), ZoneOffset.UTC),
                ticks,
                checkpoint = { checkpoints++ },
            )
    }

    @After
    fun tearDown() = database.close()

    private suspend fun add(
        status: SessionStatus,
        startedAt: Long,
    ): Long {
        val id =
            repo.createSession(
                SessionRecord(
                    exerciseId = "crazy_eights",
                    status = status,
                    startedAtEpochMillis = startedAt,
                    zoneId = "UTC",
                    localEpochDay = 100,
                ),
            )
        repo.upsertDraws(id, listOf(DrawRecord("item", 0, "t", "e1", "Une clé")))
        repo.upsertAnswers(id, listOf(AnswerRecord("idea", 0, "texte")))
        repo.upsertRubric(id, listOf(RubricRecord("q", true)))
        return id
    }

    @Test
    fun `readAll returns every session with its children, oldest first, in progress included`() =
        runTest {
            add(SessionStatus.IN_PROGRESS, startedAt = 2_000)
            add(SessionStatus.COMPLETED, startedAt = 1_000)

            val all = repo.readAll()

            assertEquals(listOf(1_000L, 2_000L), all.map { it.session.startedAtEpochMillis })
            assertEquals(listOf("Une clé"), all.first().draws.map { it.textSnapshot })
            assertEquals(listOf("texte"), all.first().answers.map { it.text })
            assertEquals(listOf(true), all.first().rubric.map { it.value })
        }

    @Test
    fun `deleteAll removes sessions and their content`() =
        runTest {
            add(SessionStatus.COMPLETED, startedAt = 1_000)
            add(SessionStatus.IN_PROGRESS, startedAt = 2_000)

            repo.deleteAll()

            assertTrue(repo.readAll().isEmpty())
        }

    @Test
    fun `deleteAll checkpoints the WAL after the delete`() =
        runTest {
            add(SessionStatus.COMPLETED, startedAt = 1_000)

            repo.deleteAll()

            assertEquals(1, checkpoints)
        }

    @Test
    fun `truncateWal leaves the database usable`() =
        runTest {
            add(SessionStatus.COMPLETED, startedAt = 1_000)

            database.truncateWal()
            add(SessionStatus.COMPLETED, startedAt = 2_000)

            assertEquals(2, repo.readAll().size)
        }
}
