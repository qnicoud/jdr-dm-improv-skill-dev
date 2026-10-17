package io.github.qnicoud.dmgym.data.history

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.ExerciseCountRecord
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionNotFoundException
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.data.history.db.DmGymDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private class MutableClock(
    var now: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = now
}

@RunWith(RobolectricTestRunner::class)
class RoomHistoryRepositoryTest {
    private lateinit var database: DmGymDatabase
    private lateinit var repo: RoomHistoryRepository
    private lateinit var clock: MutableClock
    private lateinit var ticks: MutableSharedFlow<Unit>
    private val start = Instant.parse("2026-10-16T12:00:00Z")
    private val hour = 3_600_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder<DmGymDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()
        clock = MutableClock(start)
        ticks = MutableSharedFlow(replay = 1)
        ticks.tryEmit(Unit)
        repo = RoomHistoryRepository(database.sessionDao(), clock, ticks)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun session(
        exerciseId: String = "crazy8",
        status: SessionStatus = SessionStatus.COMPLETED,
        startedAgoMillis: Long = hour,
        day: Long = 100,
        gem: Boolean = false,
    ) = SessionRecord(
        exerciseId = exerciseId,
        status = status,
        startedAtEpochMillis = start.toEpochMilli() - startedAgoMillis,
        zoneId = "UTC",
        localEpochDay = day,
        isGem = gem,
    )

    private fun inProgress(agoHours: Long) =
        session(
            status = SessionStatus.IN_PROGRESS,
            startedAgoMillis =
                agoHours * hour,
        )

    private suspend fun create(
        record: SessionRecord,
        text: String? = "idée",
        answerGem: Boolean = false,
    ): Long {
        val id = repo.createSession(record)
        if (text != null) repo.upsertAnswers(id, listOf(AnswerRecord("f", 0, text, isGem = answerGem)))
        return id
    }

    private suspend fun stored(id: Long) = repo.observeDetail(id).first()!!

    @Test
    fun upsertsAreIdempotentAndOverwrite() =
        runTest {
            val id = create(session(), text = "a")
            repo.upsertAnswers(id, listOf(AnswerRecord("f", 0, "b"), AnswerRecord("f", 0, "b")))
            repo.upsertDraws(id, listOf(DrawRecord("s", 0, "t", "e", "x")))
            repo.upsertDraws(id, listOf(DrawRecord("s", 0, "t", "e", "y")))
            repo.upsertRubric(id, listOf(RubricRecord("q", false)))
            repo.upsertRubric(id, listOf(RubricRecord("q", true)))

            val detail = stored(id)
            assertEquals(listOf(AnswerRecord("f", 0, "b")), detail.answers)
            assertEquals(listOf("y"), detail.draws.map { it.textSnapshot })
            assertEquals(listOf(RubricRecord("q", true)), detail.rubric)
        }

    @Test
    fun detailReturnsSessionWithChildren() =
        runTest {
            val id = create(session())
            val manual = DrawRecord("s", 1, null, "7", "sept", true, false, "taste")
            repo.upsertDraws(id, listOf(manual, DrawRecord("s", 0, "t", "e", "x")))

            val detail = stored(id)
            assertEquals(listOf(0, 1), detail.draws.map { it.position })
            assertEquals(manual, detail.draws[1])
            assertNull(repo.observeDetail(999).first())
        }

    @Test
    fun updateSessionIsTargetedAndNeverRewritesDayOrZone() =
        runTest {
            val id = create(session(day = 100))
            val before = stored(id).session

            repo.updateSession(
                before.copy(
                    status = SessionStatus.COMPLETED,
                    endedAtEpochMillis = 5,
                    activeDurationSec = 60,
                    overtimeSec = 3,
                    difficulty = 2,
                    isGem = true,
                    selfRating = 3,
                    localEpochDay = 999,
                    zoneId = "Mars/Base",
                ),
            )

            val after = stored(id).session
            assertEquals(100L, after.localEpochDay)
            assertEquals("UTC", after.zoneId)
            assertEquals(60, after.activeDurationSec)
            assertEquals(3, after.overtimeSec)
            assertEquals(2, after.difficulty)
            assertEquals(3, after.selfRating)
            assertEquals(5L, after.endedAtEpochMillis)
            assertTrue(after.isGem)
        }

    @Test
    fun updateOfMissingSessionFailsExplicitly() =
        runTest {
            val result = runCatching { repo.updateSession(session().copy(id = 42)) }
            assertEquals(SessionNotFoundException::class, result.exceptionOrNull()!!::class)
        }

    @Test
    fun saveSnapshotWritesEverythingAndAdvancesLastActivity() =
        runTest {
            val id = create(session(), text = null)
            val record = stored(id).session
            clock.now = start.plusSeconds(60)

            repo.saveSnapshot(
                record.copy(activeDurationSec = 30),
                listOf(DrawRecord("s", 0, "t", "e", "x")),
                listOf(AnswerRecord("f", 0, "texte")),
                listOf(RubricRecord("q", true)),
            )

            val detail = stored(id)
            assertEquals(30, detail.session.activeDurationSec)
            assertEquals(clock.millis(), detail.session.lastActivityAtEpochMillis)
            assertEquals(1, detail.draws.size)
            assertEquals(1, detail.answers.size)
            assertEquals(1, detail.rubric.size)
        }

    @Test
    fun saveSnapshotOnMissingSessionFailsAndWritesNothing() =
        runTest {
            val result =
                runCatching {
                    repo.saveSnapshot(
                        session().copy(id = 77),
                        emptyList(),
                        listOf(AnswerRecord("f", 0, "x")),
                        emptyList(),
                    )
                }
            assertEquals(SessionNotFoundException::class, result.exceptionOrNull()!!::class)
        }

    @Test
    fun childrenWithoutSessionViolateForeignKey() =
        runTest {
            val result = runCatching { repo.upsertAnswers(404, listOf(AnswerRecord("f", 0, "x"))) }
            assertTrue(result.isFailure)
        }

    @Test
    fun deleteCascadesToChildren() =
        runTest {
            val id = create(session())
            repo.upsertDraws(id, listOf(DrawRecord("s", 0, "t", "e", "x")))
            repo.upsertRubric(id, listOf(RubricRecord("q", true)))

            repo.deleteSession(id)

            assertNull(repo.observeDetail(id).first())
            assertEquals(emptyList<Long>(), repo.observePracticedDays().first())
            // Un identifiant réutilisé ne ressuscite aucun enfant.
            repo.createSession(session().copy(id = id))
            val detail = stored(id)
            assertEquals(emptyList<DrawRecord>(), detail.draws)
            assertEquals(emptyList<AnswerRecord>(), detail.answers)
            assertEquals(emptyList<RubricRecord>(), detail.rubric)
        }

    @Test
    fun historyFiltersByExerciseAndOrdersByRecency() =
        runTest {
            val old = create(session("crazy8", startedAgoMillis = 5 * hour))
            val recent = create(session("crazy8", startedAgoMillis = 2 * hour))
            create(session("onepage", startedAgoMillis = 3 * hour))

            assertEquals(listOf(recent, old), repo.observeHistory("crazy8").first().map { it.id })
            assertEquals(3, repo.observeHistory().first().size)
        }

    @Test
    fun gemFilterMatchesSessionOrAnswerGemAndCombinesWithExercise() =
        runTest {
            val sessionGem = create(session(gem = true))
            val answerGem = create(session(), answerGem = true)
            create(session())
            create(session("onepage", gem = true))

            val all =
                repo
                    .observeHistory(gemsOnly = true)
                    .first()
                    .map { it.id }
                    .toSet()
            assertEquals(3, all.size)
            val crazy =
                repo
                    .observeHistory("crazy8", gemsOnly = true)
                    .first()
                    .map { it.id }
                    .toSet()
            assertEquals(setOf(sessionGem, answerGem), crazy)
        }

    @Test
    fun staleInProgressWithContentIsUnfinishedAndFreshOrEmptyAreHidden() =
        runTest {
            val stale = create(inProgress(30))
            create(inProgress(2))
            create(inProgress(30), text = "")
            create(inProgress(30), text = "   ")

            val items = repo.observeHistory().first()
            assertEquals(listOf(stale), items.map { it.id })
            assertEquals(SessionStatus.IN_PROGRESS, items.single().status)
        }

    @Test
    fun staleDelayIsStrictlyOver24HoursFromLastActivity() =
        runTest {
            val exactly = create(inProgress(24))
            val justOver = create(session(status = SessionStatus.IN_PROGRESS, startedAgoMillis = 24 * hour + 1))

            assertEquals(listOf(justOver), repo.observeHistory().first().map { it.id })
            assertEquals(0, repo.purgeEmptyInProgress())
            assertEquals(exactly, stored(exactly).session.id)
        }

    @Test
    fun delayCountsFromLastActivityNotFromStart() =
        runTest {
            val id = create(inProgress(30))
            repo.saveSnapshot(stored(id).session, emptyList(), listOf(AnswerRecord("f", 0, "encore")), emptyList())

            assertEquals(emptyList<Long>(), repo.observeHistory().first().map { it.id })
        }

    @Test
    fun historyReEmitsWhenThresholdCrossesWithoutNewQuery() =
        runTest {
            val id = create(inProgress(23))
            val results = Channel<List<Long>>(Channel.UNLIMITED)
            val job =
                launch(Dispatchers.Default) {
                    repo.observeHistory().collect {
                        results.send(
                            it.map { s ->
                                s.id
                            },
                        )
                    }
                }

            val first = withContext(Dispatchers.Default) { withTimeout(5_000) { results.receive() } }
            assertEquals(emptyList<Long>(), first)

            clock.now = start.plusSeconds(2 * 3600)
            ticks.emit(Unit)

            val second = withContext(Dispatchers.Default) { withTimeout(5_000) { results.receive() } }
            assertEquals(listOf(id), second)
            job.cancel()
        }

    @Test
    fun practicedDaysAreDistinctAndCountCompletedWithoutNotes() =
        runTest {
            create(session(day = 10))
            create(session(day = 10))
            create(session(day = 11, status = SessionStatus.IN_PROGRESS, startedAgoMillis = 30 * hour))
            create(session(day = 12), text = null)
            create(session(day = 13), text = " \n ")
            create(session(day = 14, status = SessionStatus.IN_PROGRESS))
            val drawsOnly = create(session(day = 15), text = null)
            repo.upsertDraws(drawsOnly, listOf(DrawRecord("s", 0, "t", "e", "x")))
            repo.upsertRubric(drawsOnly, listOf(RubricRecord("q", true)))

            assertEquals(listOf(10L, 11L, 12L, 13L, 15L), repo.observePracticedDays().first())
        }

    @Test
    fun countsCompletedSessionsAndUnfinishedOnesWithContentWithinRange() =
        runTest {
            create(session("crazy8", day = 10))
            create(session("crazy8", day = 11))
            create(session("crazy8", day = 20))
            create(session("onepage", day = 10))
            create(session("onepage", day = 10, status = SessionStatus.IN_PROGRESS))
            create(session("onepage", day = 10), text = null)
            create(session("onepage", day = 10, status = SessionStatus.IN_PROGRESS, startedAgoMillis = 30 * hour))

            assertEquals(6, repo.observeSessionCount().first())
            val counts = repo.countSessionsByExercise(10, 11).sortedBy { it.exerciseId }
            assertEquals(listOf(ExerciseCountRecord("crazy8", 2), ExerciseCountRecord("onepage", 3)), counts)
        }

    @Test
    fun purgeRemovesOnlyStaleEmptyInProgress() =
        runTest {
            val emptyStale = create(inProgress(30), text = null)
            val blankStale = create(inProgress(30), text = " ")
            val fresh = create(inProgress(2), text = null)
            val withContent = create(inProgress(30))
            val completedEmpty = create(session(startedAgoMillis = 30 * hour), text = null)

            assertEquals(2, repo.purgeEmptyInProgress())

            assertNull(repo.observeDetail(emptyStale).first())
            assertNull(repo.observeDetail(blankStale).first())
            listOf(fresh, withContent, completedEmpty).forEach { assertEquals(it, stored(it).session.id) }
        }
}
