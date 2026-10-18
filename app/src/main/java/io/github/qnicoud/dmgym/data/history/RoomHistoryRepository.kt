package io.github.qnicoud.dmgym.data.history

import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.ExerciseCountRecord
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.ImportResult
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionArchive
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionIdentity
import io.github.qnicoud.dmgym.core.history.SessionNotFoundException
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.data.history.db.SessionDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration

/**
 * @param clock horloge injectée, sans valeur par défaut : l'application passe l'horloge système.
 * @param ticks signal qui recalcule le seuil des 24 h (au moins une émission, puis périodique) ;
 *   voir [periodicTicks]. Injectable pour piloter le temps dans les tests.
 * @param checkpoint appelé après « Tout effacer » pour tronquer le WAL (voir `truncateWal`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomHistoryRepository(
    private val dao: SessionDao,
    private val clock: Clock,
    ticks: Flow<Unit>,
    private val checkpoint: suspend () -> Unit = {},
) : HistoryRepository,
    SessionArchive {
    private val staleThresholds: Flow<Long> = ticks.map { staleBefore() }.distinctUntilChanged()

    private fun staleBefore(): Long = clock.millis() - IN_PROGRESS_TTL.toMillis()

    override suspend fun createSession(session: SessionRecord): Long = dao.insert(session.toEntity())

    override suspend fun updateSession(session: SessionRecord) {
        if (dao.updateOutcome(session.toEntity(), clock.millis()) != 1) throw SessionNotFoundException(session.id)
    }

    override suspend fun saveSnapshot(
        session: SessionRecord,
        draws: List<DrawRecord>,
        answers: List<AnswerRecord>,
        rubric: List<RubricRecord>,
    ) {
        val id = session.id
        try {
            dao.saveSnapshot(
                session.toEntity(),
                draws.map { it.toEntity(id) },
                answers.map { it.toEntity(id) },
                rubric.map { it.toEntity(id) },
                clock.millis(),
            )
        } catch (e: IllegalStateException) {
            throw SessionNotFoundException(id).apply { initCause(e) }
        }
    }

    override suspend fun updateRating(
        sessionId: Long,
        rating: Int?,
    ) {
        if (dao.updateRating(sessionId, rating) != 1) throw SessionNotFoundException(sessionId)
    }

    override suspend fun updateGem(
        sessionId: Long,
        isGem: Boolean,
    ) {
        if (dao.updateGem(sessionId, isGem) != 1) throw SessionNotFoundException(sessionId)
    }

    override suspend fun deleteSession(id: Long) = dao.deleteById(id)

    override suspend fun readAll(): List<SessionDetail> = dao.readAll().map { it.toDetail() }

    override suspend fun deleteAll() {
        dao.deleteAll()
        checkpoint()
    }

    override suspend fun identities(): Set<SessionIdentity> =
        dao.identities().mapTo(HashSet()) { SessionIdentity(it.exerciseId, it.startedAtEpochMillis) }

    override suspend fun importSessions(sessions: List<SessionDetail>): ImportResult {
        val rows = dao.importAll(sessions.map { it.toNewRows() })
        return ImportResult(rows.insertedIds, rows.skipped)
    }

    override suspend fun deleteSessions(ids: List<Long>) = dao.deleteSessions(ids)

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
        staleThresholds
            .flatMapLatest { dao.observeHistory(exerciseId, gemsOnly, it) }
            .map { list -> list.map { it.toRecord() } }

    override fun observeDetail(id: Long): Flow<SessionDetail?> = dao.observeDetail(id).map { it?.toDetail() }

    override fun observeResumable(): Flow<SessionDetail?> =
        staleThresholds.flatMapLatest { dao.observeResumable(it) }.map { it?.toDetail() }

    override fun observePreviews(): Flow<Map<Long, String>> =
        staleThresholds
            .flatMapLatest { dao.observePreviews(it) }
            .map { rows -> rows.associate { it.sessionId to it.text } }
            .distinctUntilChanged()

    override fun observePracticedDays(): Flow<List<Long>> =
        staleThresholds.flatMapLatest { dao.observePracticedDays(it) }

    override fun observeSessionCount(): Flow<Int> = staleThresholds.flatMapLatest { dao.observeSessionCount(it) }

    override fun observeCountedExerciseIds(exerciseIds: Set<String>): Flow<Set<String>> =
        staleThresholds
            .flatMapLatest { dao.observeCountedExerciseIds(exerciseIds.toList(), it) }
            .map { it.toSet() }
            .distinctUntilChanged()

    override suspend fun countSessionsByExercise(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<ExerciseCountRecord> = dao.countByExercise(fromEpochDay, toEpochDay, staleBefore()).map { it.toRecord() }

    override suspend fun purgeEmptyInProgress(): Int = dao.purgeEmptyInProgress(staleBefore())

    private companion object {
        val IN_PROGRESS_TTL: Duration = Duration.ofHours(24)
    }
}

/** Émet immédiatement puis à chaque [period] ; alimente le seuil d'« inachevée » en production. */
fun periodicTicks(period: Duration): Flow<Unit> =
    flow {
        while (true) {
            emit(Unit)
            delay(period.toMillis())
        }
    }
