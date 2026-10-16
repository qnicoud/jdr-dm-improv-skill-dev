package io.github.qnicoud.dmgym.data.history

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.ExerciseCountRecord
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.data.history.db.DmGymDatabase
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
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RoomHistoryRepositoryTest {
    private lateinit var database: DmGymDatabase
    private lateinit var repo: RoomHistoryRepository
    private val now = Instant.parse("2026-10-16T12:00:00Z")
    private val hour = 3_600_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder<DmGymDatabase>(context)
                .setDriver(AndroidSQLiteDriver())
                .build()
        repo = RoomHistoryRepository(database.sessionDao(), Clock.fixed(now, ZoneOffset.UTC))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun session(
        exerciseId: String = "crazy8",
        status: SessionStatus = SessionStatus.COMPLETED,
        startedAgoHours: Long = 1,
        day: Long = 100,
        gem: Boolean = false,
    ) = SessionRecord(
        exerciseId = exerciseId,
        status = status,
        startedAtEpochMillis = now.toEpochMilli() - startedAgoHours * hour,
        zoneId = "UTC",
        localEpochDay = day,
        isGem = gem,
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

    @Test
    fun upsertsAreIdempotentAndOverwrite() =
        runTest {
            val id = create(session(), text = "a")
            repo.upsertAnswers(id, listOf(AnswerRecord("f", 0, "b"), AnswerRecord("f", 0, "b")))
            repo.upsertDraws(id, listOf(DrawRecord("s", 0, "t", "e", "x")))
            repo.upsertDraws(id, listOf(DrawRecord("s", 0, "t", "e", "y")))
            repo.upsertRubric(id, listOf(RubricRecord("q", false)))
            repo.upsertRubric(id, listOf(RubricRecord("q", true)))

            val detail = repo.observeDetail(id).first()!!
            assertEquals(listOf(AnswerRecord("f", 0, "b")), detail.answers)
            assertEquals(listOf("y"), detail.draws.map { it.textSnapshot })
            assertEquals(listOf(RubricRecord("q", true)), detail.rubric)
        }

    @Test
    fun detailReturnsSessionWithChildrenAndUpdates() =
        runTest {
            val id = create(session())
            val stored = repo.observeDetail(id).first()!!.session
            repo.updateSession(stored.copy(selfRating = 3, isGem = true))
            val manual = DrawRecord("s", 1, null, "7", "sept", true, false)
            repo.upsertDraws(id, listOf(manual, DrawRecord("s", 0, "t", "e", "x")))

            val detail = repo.observeDetail(id).first()!!
            assertEquals(3, detail.session.selfRating)
            assertEquals(listOf(0, 1), detail.draws.map { it.position })
            assertEquals(manual, detail.draws[1])
            assertNull(repo.observeDetail(999).first())
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
            val detail = repo.observeDetail(id).first()!!
            assertEquals(emptyList<DrawRecord>(), detail.draws)
            assertEquals(emptyList<AnswerRecord>(), detail.answers)
            assertEquals(emptyList<RubricRecord>(), detail.rubric)
        }

    @Test
    fun historyFiltersByExerciseAndOrdersByRecency() =
        runTest {
            val old = create(session("crazy8", startedAgoHours = 5))
            val recent = create(session("crazy8", startedAgoHours = 2))
            create(session("onepage", startedAgoHours = 3))

            assertEquals(listOf(recent, old), repo.observeHistory("crazy8").first().map { it.id })
            assertEquals(3, repo.observeHistory().first().size)
        }

    @Test
    fun gemFilterMatchesSessionOrAnswerGem() =
        runTest {
            val sessionGem = create(session(gem = true))
            val answerGem = create(session(), answerGem = true)
            create(session())

            val ids =
                repo
                    .observeHistory(gemsOnly = true)
                    .first()
                    .map { it.id }
                    .toSet()
            assertEquals(setOf(sessionGem, answerGem), ids)
        }

    @Test
    fun staleInProgressWithContentIsUnfinishedAndFreshOrEmptyAreHidden() =
        runTest {
            val stale = create(session(status = SessionStatus.IN_PROGRESS, startedAgoHours = 30))
            create(session(status = SessionStatus.IN_PROGRESS, startedAgoHours = 2))
            create(session(status = SessionStatus.IN_PROGRESS, startedAgoHours = 30), text = "")

            val items = repo.observeHistory().first()
            assertEquals(listOf(stale), items.map { it.id })
            assertEquals(SessionStatus.IN_PROGRESS, items.single().status)
        }

    @Test
    fun practicedDaysAreDistinctAndRequireContent() =
        runTest {
            create(session(day = 10))
            create(session(day = 10))
            create(session(day = 11, status = SessionStatus.IN_PROGRESS, startedAgoHours = 30))
            create(session(day = 12), text = null)
            create(session(day = 13), text = "")

            assertEquals(listOf(10L, 11L), repo.observePracticedDays().first())
        }

    @Test
    fun countsOnlyCompletedAndGroupsByExerciseWithinRange() =
        runTest {
            create(session("crazy8", day = 10))
            create(session("crazy8", day = 11))
            create(session("crazy8", day = 20))
            create(session("onepage", day = 10))
            create(session("onepage", day = 10, status = SessionStatus.IN_PROGRESS))

            assertEquals(4, repo.observeCompletedCount().first())
            val counts = repo.countByExercise(10, 11).sortedBy { it.exerciseId }
            assertEquals(listOf(ExerciseCountRecord("crazy8", 2), ExerciseCountRecord("onepage", 1)), counts)
        }

    @Test
    fun purgeRemovesOnlyStaleEmptyInProgress() =
        runTest {
            val emptyStale = create(session(status = SessionStatus.IN_PROGRESS, startedAgoHours = 30), text = null)
            val blankStale = create(session(status = SessionStatus.IN_PROGRESS, startedAgoHours = 30), text = "")
            val fresh = create(session(status = SessionStatus.IN_PROGRESS, startedAgoHours = 2), text = null)
            val withContent = create(session(status = SessionStatus.IN_PROGRESS, startedAgoHours = 30))
            val completedEmpty = create(session(startedAgoHours = 30), text = null)

            assertEquals(2, repo.purgeEmptyInProgress())

            assertNull(repo.observeDetail(emptyStale).first())
            assertNull(repo.observeDetail(blankStale).first())
            listOf(
                fresh,
                withContent,
                completedEmpty,
            ).forEach {
                assertEquals(
                    it,
                    repo
                        .observeDetail(it)
                        .first()!!
                        .session.id,
                )
            }
        }
}
