package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.ExerciseCountRecord
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionNotFoundException
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.signal.EndSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Historique en mémoire qui garde la trace des écritures. */
class RecordingHistoryRepository : HistoryRepository {
    val sessions = mutableMapOf<Long, SessionDetail>()
    val snapshots = mutableListOf<SessionDetail>()
    val deleted = mutableListOf<Long>()
    var failOnSave = false
    private var nextId = 1L

    override suspend fun createSession(session: SessionRecord): Long {
        val id = nextId++
        sessions[id] = SessionDetail(session.copy(id = id), emptyList(), emptyList(), emptyList())
        return id
    }

    override suspend fun saveSnapshot(
        session: SessionRecord,
        draws: List<DrawRecord>,
        answers: List<AnswerRecord>,
        rubric: List<RubricRecord>,
    ) {
        check(!failOnSave) { "disk full" }
        if (session.id !in sessions) throw SessionNotFoundException(session.id)
        val detail = SessionDetail(session, draws, answers, rubric)
        sessions[session.id] = detail
        snapshots += detail
    }

    override suspend fun deleteSession(id: Long) {
        sessions.remove(id)
        deleted += id
    }

    override fun observeDetail(id: Long): Flow<SessionDetail?> = flowOf(sessions[id])

    override suspend fun updateSession(session: SessionRecord) = unused()

    override suspend fun upsertDraws(
        sessionId: Long,
        draws: List<DrawRecord>,
    ) = unused()

    override suspend fun upsertAnswers(
        sessionId: Long,
        answers: List<AnswerRecord>,
    ) = unused()

    override suspend fun upsertRubric(
        sessionId: Long,
        rubric: List<RubricRecord>,
    ) = unused()

    override fun observeHistory(
        exerciseId: String?,
        gemsOnly: Boolean,
    ): Flow<List<SessionRecord>> = flowOf(emptyList())

    override fun observePracticedDays(): Flow<List<Long>> = flowOf(emptyList())

    override fun observeSessionCount(): Flow<Int> = flowOf(0)

    override suspend fun countSessionsByExercise(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<ExerciseCountRecord> = unused()

    override suspend fun purgeEmptyInProgress(): Int = unused()

    private fun unused(): Nothing = error("not used by the exercise screen")
}

class FakeEndSignal : EndSignal {
    override val canVibrate = true
    var prepared = false
    var released = false
    val played = mutableListOf<Settings>()

    override fun prepare() {
        prepared = true
    }

    override fun play(settings: Settings) {
        played += settings
    }

    override fun release() {
        released = true
    }
}

/** Horloge murale réglable. */
class MutableClock(
    var millis: Long = 1_700_000_000_000,
) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("UTC")

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = Instant.ofEpochMilli(millis)
}
