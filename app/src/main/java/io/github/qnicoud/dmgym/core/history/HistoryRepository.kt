package io.github.qnicoud.dmgym.core.history

import kotlinx.coroutines.flow.Flow

/**
 * Port de l'historique. Une session « en cours » dure 24 h (D36) : au-delà, si elle a du contenu,
 * elle apparaît dans l'historique avec le statut [SessionStatus.IN_PROGRESS] (« inachevée »).
 * Un « jour pratiqué » est un jour avec au moins une session qui a du contenu (D35).
 */
interface HistoryRepository {
    /** Crée la session et renvoie son identifiant. */
    suspend fun createSession(session: SessionRecord): Long

    suspend fun updateSession(session: SessionRecord)

    suspend fun deleteSession(id: Long)

    suspend fun upsertDraws(
        sessionId: Long,
        draws: List<DrawRecord>,
    )

    suspend fun upsertAnswers(
        sessionId: Long,
        answers: List<AnswerRecord>,
    )

    suspend fun upsertRubric(
        sessionId: Long,
        rubric: List<RubricRecord>,
    )

    /** Sessions terminées et inachevées (en cours depuis plus de 24 h, avec contenu), les plus récentes d'abord. */
    fun observeHistory(
        exerciseId: String? = null,
        gemsOnly: Boolean = false,
    ): Flow<List<SessionRecord>>

    fun observeDetail(id: Long): Flow<SessionDetail?>

    fun observePracticedDays(): Flow<List<Long>>

    fun observeCompletedCount(): Flow<Int>

    suspend fun countByExercise(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<ExerciseCountRecord>

    /** Supprime les sessions en cours vides, plus vieilles que 24 h. Renvoie leur nombre. */
    suspend fun purgeEmptyInProgress(): Int
}
