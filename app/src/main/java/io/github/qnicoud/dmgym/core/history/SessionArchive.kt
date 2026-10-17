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
}
