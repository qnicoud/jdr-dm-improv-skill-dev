package io.github.qnicoud.dmgym.data.history.db

import androidx.room3.useWriterConnection

/**
 * Vide le journal WAL dans le fichier principal et le tronque, pour qu'une suppression ne laisse
 * pas de données lisibles dans `-wal`. Le PRAGMA renvoie une ligne : on le lit via `usePrepared`.
 */
suspend fun DmGymDatabase.truncateWal() {
    useWriterConnection { connection ->
        connection.usePrepared("PRAGMA wal_checkpoint(TRUNCATE)") { it.step() }
    }
}
