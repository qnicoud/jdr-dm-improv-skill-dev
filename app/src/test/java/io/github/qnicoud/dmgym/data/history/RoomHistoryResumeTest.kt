package io.github.qnicoud.dmgym.data.history

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.data.history.db.DmGymDatabase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private class FixedClock(
    private val now: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = now
}

/** Requêtes du lot L9 : session à reprendre et aperçus de l'historique. */
@RunWith(RobolectricTestRunner::class)
class RoomHistoryResumeTest {
    private lateinit var database: DmGymDatabase
    private lateinit var repo: RoomHistoryRepository
    private val start = Instant.parse("2026-10-16T12:00:00Z")
    private val hour = 3_600_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<DmGymDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
        val ticks = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
        repo = RoomHistoryRepository(database.sessionDao(), FixedClock(start), ticks)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun create(
        status: SessionStatus = SessionStatus.IN_PROGRESS,
        agoHours: Long = 1,
        texts: List<AnswerRecord> = listOf(AnswerRecord("f", 0, "idée")),
    ): Long {
        val at = start.toEpochMilli() - agoHours * hour
        val id =
            repo.createSession(
                SessionRecord(
                    exerciseId = "crazy8",
                    status = status,
                    startedAtEpochMillis = at,
                    zoneId = "UTC",
                    localEpochDay = 100,
                ),
            )
        repo.upsertAnswers(id, texts)
        return id
    }

    @Test
    fun resumableIsTheMostRecentFreshInProgressWithContent() =
        runTest {
            create(agoHours = 5)
            val latest = create(agoHours = 2)
            create(agoHours = 1, texts = listOf(AnswerRecord("f", 0, "  ")))
            create(status = SessionStatus.COMPLETED, agoHours = 1)

            assertEquals(
                latest,
                repo
                    .observeResumable()
                    .first()
                    ?.session
                    ?.id,
            )
        }

    @Test
    fun resumableIgnoresSessionsIdleForMoreThan24Hours() =
        runTest {
            create(agoHours = 25)

            assertNull(repo.observeResumable().first())
        }

    @Test
    fun resumableCarriesItsAnswers() =
        runTest {
            create(texts = listOf(AnswerRecord("f", 0, "bonjour")))

            assertEquals(
                listOf("bonjour"),
                repo
                    .observeResumable()
                    .first()
                    ?.answers
                    ?.map { it.text },
            )
        }

    @Test
    fun previewIsTheFirstNonBlankAnswerTruncated() =
        runTest {
            val long = "x".repeat(500)
            val id =
                create(
                    texts =
                        listOf(
                            AnswerRecord("a", 0, " "),
                            AnswerRecord("b", 0, long),
                            AnswerRecord("c", 0, "autre"),
                        ),
                )
            val blank = create(texts = listOf(AnswerRecord("a", 0, "")))

            val previews = repo.observePreviews().first()

            assertEquals(240, previews.getValue(id).length)
            assertNull(previews[blank])
        }
}
