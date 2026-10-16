package io.github.qnicoud.dmgym.data.history

import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.ExerciseCountRecord
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.data.history.db.SessionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration

class RoomHistoryRepository(
    private val dao: SessionDao,
    private val clock: Clock = Clock.systemUTC(),
) : HistoryRepository {
    private fun staleBefore(): Long = clock.millis() - IN_PROGRESS_TTL.toMillis()

    override suspend fun createSession(session: SessionRecord): Long = dao.insert(session.toEntity())

    override suspend fun updateSession(session: SessionRecord) = dao.update(session.toEntity())

    override suspend fun deleteSession(id: Long) = dao.deleteById(id)

    override suspend fun upsertDraws(
        sessionId: Long,
        draws: List<DrawRecord>,
    ) = dao.upsertDraws(draws.map { it.toEntity(sessionId) })

    override suspend fun upsertAnswers(
        sessionId: Long,
        answers: List<AnswerRecord>,
    ) = dao.upsertAnswers(answers.map { it.toEntity(sessionId) })

    override suspend fun upsertRubric(
        sessionId: Long,
        rubric: List<RubricRecord>,
    ) = dao.upsertRubric(rubric.map { it.toEntity(sessionId) })

    override fun observeHistory(
        exerciseId: String?,
        gemsOnly: Boolean,
    ): Flow<List<SessionRecord>> =
        dao.observeHistory(exerciseId, gemsOnly, staleBefore()).map { list -> list.map { it.toRecord() } }

    override fun observeDetail(id: Long): Flow<SessionDetail?> = dao.observeDetail(id).map { it?.toDetail() }

    override fun observePracticedDays(): Flow<List<Long>> = dao.observePracticedDays()

    override fun observeCompletedCount(): Flow<Int> = dao.observeCompletedCount()

    override suspend fun countByExercise(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<ExerciseCountRecord> = dao.countByExercise(fromEpochDay, toEpochDay).map { it.toRecord() }

    override suspend fun purgeEmptyInProgress(): Int = dao.purgeEmptyInProgress(staleBefore())

    private companion object {
        val IN_PROGRESS_TTL: Duration = Duration.ofHours(24)
    }
}
