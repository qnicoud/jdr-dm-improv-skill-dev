package io.github.qnicoud.dmgym.data.history.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Au moins une réponse non vide après `trim` (ADR 0011). */
private const val HAS_CONTENT =
    "id IN (SELECT sessionId FROM session_answers WHERE trim(text, ' ' || char(9, 10, 13)) <> '')"

private const val NOT_BLANK_A = "trim(a.text, ' ' || char(9, 10, 13)) <> ''"
private const val NOT_BLANK_B = "trim(b.text, ' ' || char(9, 10, 13)) <> ''"

/** Inachevée : en cours, sans activité depuis plus de 24 h (`:staleBefore`, comparaison stricte). */
private const val IS_STALE = "status = 'IN_PROGRESS' AND lastActivityAtEpochMillis < :staleBefore"

/** Session comptée : avec contenu, terminée ou inachevée ; la session en cours active est exclue. */
private const val IS_COUNTED = "$HAS_CONTENT AND (status = 'COMPLETED' OR ($IS_STALE))"

@Dao
abstract class SessionDao {
    @Insert
    abstract suspend fun insert(session: SessionEntity): Long

    /** Mise à jour ciblée : ni `localEpochDay` ni `zoneId`. Renvoie le nombre de lignes modifiées. */
    @Query(
        "UPDATE sessions SET status = :status, endedAtEpochMillis = :endedAt, activeDurationSec = :activeSec, " +
            "overtimeSec = :overtimeSec, difficulty = :difficulty, isGem = :isGem, selfRating = :selfRating, " +
            "lastActivityAtEpochMillis = :now WHERE id = :id",
    )
    abstract suspend fun updateOutcome(
        id: Long,
        status: String,
        endedAt: Long?,
        activeSec: Int,
        overtimeSec: Int,
        difficulty: Int,
        isGem: Boolean,
        selfRating: Int?,
        now: Long,
    ): Int

    @Query("DELETE FROM sessions WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Upsert
    abstract suspend fun upsertDraws(draws: List<DrawEntity>)

    @Upsert
    abstract suspend fun upsertAnswers(answers: List<AnswerEntity>)

    @Upsert
    abstract suspend fun upsertRubric(rubric: List<RubricEntity>)

    /** Tout ou rien. Lève [IllegalStateException] (annule la transaction) si la session n'existe pas. */
    @Transaction
    open suspend fun saveSnapshot(
        session: SessionEntity,
        draws: List<DrawEntity>,
        answers: List<AnswerEntity>,
        rubric: List<RubricEntity>,
        now: Long,
    ) {
        check(updateOutcome(session, now) == 1) { "Session ${session.id} does not exist" }
        upsertDraws(draws)
        upsertAnswers(answers)
        upsertRubric(rubric)
    }

    suspend fun updateOutcome(
        session: SessionEntity,
        now: Long,
    ): Int =
        updateOutcome(
            id = session.id,
            status = session.status,
            endedAt = session.endedAtEpochMillis,
            activeSec = session.activeDurationSec,
            overtimeSec = session.overtimeSec,
            difficulty = session.difficulty,
            isGem = session.isGem,
            selfRating = session.selfRating,
            now = now,
        )

    /** Terminées, plus les inachevées qui ont du contenu. */
    @Query(
        "SELECT * FROM sessions WHERE (status = 'COMPLETED' OR ($IS_STALE AND $HAS_CONTENT)) " +
            "AND (:exerciseId IS NULL OR exerciseId = :exerciseId) " +
            "AND (:gemsOnly = 0 OR isGem = 1 OR id IN (SELECT sessionId FROM session_answers WHERE isGem = 1)) " +
            "ORDER BY startedAtEpochMillis DESC",
    )
    abstract fun observeHistory(
        exerciseId: String?,
        gemsOnly: Boolean,
        staleBefore: Long,
    ): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    abstract fun observeDetail(id: Long): Flow<SessionWithChildren?>

    /** La session en cours la plus récente, encore active (`lastActivity >= :staleBefore`) et avec contenu. */
    @Transaction
    @Query(
        "SELECT * FROM sessions WHERE status = 'IN_PROGRESS' AND lastActivityAtEpochMillis >= :staleBefore " +
            "AND $HAS_CONTENT ORDER BY lastActivityAtEpochMillis DESC, id DESC LIMIT 1",
    )
    abstract fun observeResumable(staleBefore: Long): Flow<SessionWithChildren?>

    /** Première réponse non vide de chaque session (par groupe puis par champ), tronquée à 240 caractères. */
    @Query(
        "SELECT sessionId, substr(text, 1, 240) AS text FROM session_answers AS a WHERE $NOT_BLANK_A " +
            "AND NOT EXISTS (SELECT 1 FROM session_answers AS b WHERE b.sessionId = a.sessionId AND $NOT_BLANK_B " +
            "AND (b.groupIndex < a.groupIndex OR (b.groupIndex = a.groupIndex AND b.fieldKey < a.fieldKey)))",
    )
    abstract fun observePreviews(): Flow<List<PreviewRow>>

    @Query("SELECT DISTINCT localEpochDay FROM sessions WHERE $IS_COUNTED ORDER BY localEpochDay")
    abstract fun observePracticedDays(staleBefore: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM sessions WHERE $IS_COUNTED")
    abstract fun observeSessionCount(staleBefore: Long): Flow<Int>

    @Query(
        "SELECT exerciseId, COUNT(*) AS n FROM sessions WHERE $IS_COUNTED " +
            "AND localEpochDay BETWEEN :from AND :to GROUP BY exerciseId",
    )
    abstract suspend fun countByExercise(
        from: Long,
        to: Long,
        staleBefore: Long,
    ): List<ExerciseCount>

    @Query("DELETE FROM sessions WHERE $IS_STALE AND NOT ($HAS_CONTENT)")
    abstract suspend fun purgeEmptyInProgress(staleBefore: Long): Int
}
