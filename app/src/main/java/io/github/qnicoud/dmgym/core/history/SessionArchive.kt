package io.github.qnicoud.dmgym.core.history

/**
 * Accès en masse aux sessions : export et effacement total (S11, Données). Port distinct de [HistoryRepository],
 * qui reste centré sur les écrans de l'app.
 */
interface SessionArchive {
    /** Toutes les sessions, avec tirages, réponses et grille, y compris celles en cours. */
    suspend fun readAll(): List<SessionDetail>

    /** Supprime toutes les sessions et leur contenu. Les réglages ne sont pas touchés. */
    suspend fun deleteAll()

    /** Identités des sessions présentes (y compris celles en cours), pour l'aperçu d'un import. */
    suspend fun identities(): Set<SessionIdentity>

    /**
     * Ajoute les [sessions] qui ne sont pas déjà présentes, en une seule transaction : tout ou rien (ADR 0015 §4).
     * Chaque session reçoit un nouvel `id` local ; la présence est relue dans la transaction, donc une session créée
     * depuis l'aperçu n'est pas dupliquée. Un doublon dans [sessions] n'est ajouté qu'une fois.
     */
    suspend fun importSessions(sessions: List<SessionDetail>): ImportResult

    /** Supprime exactement ces sessions (annulation d'un import), en une transaction. Les autres restent. */
    suspend fun deleteSessions(ids: List<Long>)
}
