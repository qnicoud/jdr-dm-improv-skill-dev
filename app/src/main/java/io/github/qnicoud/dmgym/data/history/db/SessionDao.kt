package io.github.qnicoud.dmgym.data.history.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Upsert
    suspend fun upsertDraws(draws: List<DrawEntity>)

    @Upsert
    suspend fun upsertAnswers(answers: List<AnswerEntity>)

    @Upsert
    suspend fun upsertRubric(rubric: List<RubricEntity>)

    /** Terminées, plus les en-cours antérieures à [staleBefore] qui ont du contenu (inachevées). */
    @Query(
        "SELECT * FROM sessions WHERE (status = 'COMPLETED' OR " +
            "(status = 'IN_PROGRESS' AND startedAtEpochMillis < :staleBefore " +
            "AND id IN (SELECT sessionId FROM session_answers WHERE text <> ''))) " +
            "AND (:exerciseId IS NULL OR exerciseId = :exerciseId) " +
            "AND (:gemsOnly = 0 OR isGem = 1 OR id IN (SELECT sessionId FROM session_answers WHERE isGem = 1)) " +
            "ORDER BY startedAtEpochMillis DESC",
    )
    fun observeHistory(
        exerciseId: String?,
        gemsOnly: Boolean,
        staleBefore: Long,
    ): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeDetail(id: Long): Flow<SessionWithChildren?>

    @Query(
        "SELECT DISTINCT localEpochDay FROM sessions " +
            "WHERE id IN (SELECT sessionId FROM session_answers WHERE text <> '') ORDER BY localEpochDay",
    )
    fun observePracticedDays(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM sessions WHERE status = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>

    @Query(
        "SELECT exerciseId, COUNT(*) AS n FROM sessions WHERE status = 'COMPLETED' " +
            "AND localEpochDay BETWEEN :from AND :to GROUP BY exerciseId",
    )
    suspend fun countByExercise(
        from: Long,
        to: Long,
    ): List<ExerciseCount>

    @Query(
        "DELETE FROM sessions WHERE status = 'IN_PROGRESS' AND startedAtEpochMillis < :before " +
            "AND id NOT IN (SELECT sessionId FROM session_answers WHERE text <> '')",
    )
    suspend fun purgeEmptyInProgress(before: Long): Int
}
