package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.answerRecords
import io.github.qnicoud.dmgym.core.exercise.drawRecords
import io.github.qnicoud.dmgym.core.exercise.optionsJson
import io.github.qnicoud.dmgym.core.exercise.rubricRecords
import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.history.SessionNotFoundException
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.time.localEpochDay
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.withContext
import java.time.Clock

/** Durées de la séance, en millisecondes, au moment de l'écriture. */
data class SessionTimes(
    val activeMs: Long,
    val overtimeMs: Long,
)

/** Écrit la session dans l'historique : création, instantanés atomiques, suppression. */
class SessionRecorder(
    private val history: HistoryRepository,
    private val wallClock: Clock,
    private val io: CoroutineDispatcher,
) {
    private var base: SessionRecord? = null
    private var endedAt: Long? = null

    suspend fun create(
        exerciseId: String,
        plannedSec: Int?,
        fromDaily: Boolean,
        seed: Long,
    ): Long {
        val now = wallClock.millis()
        val record =
            SessionRecord(
                exerciseId = exerciseId,
                status = SessionStatus.IN_PROGRESS,
                startedAtEpochMillis = now,
                zoneId = wallClock.zone.id,
                localEpochDay = wallClock.localEpochDay(),
                plannedDurationSec = plannedSec,
                fromDaily = fromDaily,
                drawSeed = seed,
            )
        val id = withContext(io) { history.createSession(record) }
        base = record.copy(id = id)
        return id
    }

    /** Reprend l'enregistrement d'une session existante (reprise après la mort du processus). */
    fun adopt(record: SessionRecord) {
        base = record
        endedAt = record.endedAtEpochMillis
    }

    fun markEnded() {
        endedAt = wallClock.millis()
    }

    /** Écrit l'instantané ; renvoie `false` si la session n'existe plus. */
    suspend fun save(
        session: ExerciseSession,
        times: SessionTimes,
    ): Boolean {
        val record = base ?: return false
        val updated =
            record.copy(
                status = if (session.phase == Phase.Ended) SessionStatus.COMPLETED else SessionStatus.IN_PROGRESS,
                endedAtEpochMillis = if (session.phase == Phase.Ended) endedAt else null,
                activeDurationSec = (times.activeMs / MS_PER_SEC).toInt(),
                overtimeSec = (times.overtimeMs / MS_PER_SEC).toInt(),
                optionsJson = session.optionsJson(),
                selfRating = session.selfRating,
                isGem = session.isGem,
            )
        return try {
            withContext(io) {
                history.saveSnapshot(updated, session.drawRecords(), session.answerRecords(), session.rubricRecords())
            }
            true
        } catch (_: SessionNotFoundException) {
            false
        }
    }

    suspend fun delete(sessionId: Long) {
        withContext(io) { history.deleteSession(sessionId) }
        base = null
    }

    private companion object {
        const val MS_PER_SEC = 1_000L
    }
}

/**
 * Débounce de 1 s pour la frappe, écriture immédiate pour tout le reste (tirages, cases, bilan).
 * Ne s'applique qu'aux sessions déjà créées.
 */
@OptIn(FlowPreview::class)
internal suspend fun Flow<ExerciseSession>.autosave(
    debounceMs: Long,
    persist: suspend (ExerciseSession) -> Unit,
) {
    filter { it.sessionId != null }
        .distinctUntilChanged()
        .runningFold(null as ExerciseSession? to null as ExerciseSession?) { (_, previous), current ->
            previous to
                current
        }.drop(1)
        .debounce { (previous, current) -> if (isTypingOnly(previous, current)) debounceMs else 0L }
        .collect { (_, current) -> current?.let { persist(it) } }
}

private fun isTypingOnly(
    previous: ExerciseSession?,
    current: ExerciseSession?,
): Boolean = previous != null && current != null && previous.copy(answers = current.answers) == current
