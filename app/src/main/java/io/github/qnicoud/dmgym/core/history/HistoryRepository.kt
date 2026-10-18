package io.github.qnicoud.dmgym.core.history

import kotlinx.coroutines.flow.Flow

/**
 * Port de l'historique. Une session « en cours » dure 24 h (D36) : au-delà, si elle a du contenu,
 * elle apparaît dans l'historique avec le statut [SessionStatus.IN_PROGRESS] (« inachevée »).
 * Un « contenu » est au moins une réponse non vide après `trim` (ADR 0011). Une session est comptée si elle est
 * terminée, ou inachevée avec contenu. Un « jour pratiqué » est un jour avec au moins une session comptée (D35) ;
 * les compteurs suivent la même définition.
 * Le délai de 24 h se compte depuis la dernière activité.
 */
interface HistoryRepository {
    /** Crée la session et renvoie son identifiant. */
    suspend fun createSession(session: SessionRecord): Long

    /**
     * Met à jour la fin, le statut, les durées, le bijou, le niveau et l'auto-évaluation.
     * `localEpochDay` et `zoneId` ne sont jamais réécrits.
     * @throws SessionNotFoundException si la session n'existe pas.
     */
    suspend fun updateSession(session: SessionRecord)

    /**
     * Sauvegarde atomique (autosave) : mise à jour ciblée de la session, tirages, réponses et grille,
     * et avance la dernière activité. Tout ou rien.
     * @throws SessionNotFoundException si la session n'existe pas.
     */
    suspend fun saveSnapshot(
        session: SessionRecord,
        draws: List<DrawRecord>,
        answers: List<AnswerRecord>,
        rubric: List<RubricRecord>,
    )

    /**
     * Change le niveau d'auto-évaluation (`null` pour le retirer) sans toucher à la dernière activité : une session
     * inachevée le reste.
     * @throws SessionNotFoundException si la session n'existe pas.
     */
    suspend fun updateRating(
        sessionId: Long,
        rating: Int?,
    )

    /** Pose ou retire le bijou, avec les mêmes garanties que [updateRating]. */
    suspend fun updateGem(
        sessionId: Long,
        isGem: Boolean,
    )

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

    /** Sessions terminées et inachevées (en cours, sans activité depuis plus de 24 h, avec contenu), les plus récentes d'abord. */
    fun observeHistory(
        exerciseId: String? = null,
        gemsOnly: Boolean = false,
    ): Flow<List<SessionRecord>>

    fun observeDetail(id: Long): Flow<SessionDetail?>

    /**
     * La session en cours la plus récemment active (moins de 24 h, avec contenu) : la carte « Reprendre » de
     * l'accueil (D36). `null` s'il n'y en a pas.
     */
    fun observeResumable(): Flow<SessionDetail?>

    /**
     * Aperçu des sessions de l'historique : leur première réponse non vide, sans espaces autour (240 caractères au
     * plus), indexé par identifiant. Ne réémet pas à la frappe d'un brouillon en cours.
     */
    fun observePreviews(): Flow<Map<Long, String>>

    fun observePracticedDays(): Flow<List<Long>>

    /** Sessions comptées : terminées, ou inachevées avec contenu (la session active est exclue). */
    fun observeSessionCount(): Flow<Int>

    /**
     * Parmi [exerciseIds], ceux qui ont au moins une session comptée (même règle que [observeSessionCount]).
     * N'émet que lorsque l'ensemble change.
     */
    fun observeCountedExerciseIds(exerciseIds: Set<String>): Flow<Set<String>>

    suspend fun countSessionsByExercise(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<ExerciseCountRecord>

    /** Supprime les sessions en cours vides, sans activité depuis plus de 24 h. Renvoie leur nombre. */
    suspend fun purgeEmptyInProgress(): Int
}

class SessionNotFoundException(
    val sessionId: Long,
) : IllegalStateException("Session $sessionId does not exist")
