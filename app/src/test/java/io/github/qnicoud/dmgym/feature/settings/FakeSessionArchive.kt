package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.core.history.ImportResult
import io.github.qnicoud.dmgym.core.history.SessionArchive
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionIdentity
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import io.github.qnicoud.dmgym.core.history.identity

class FakeSessionArchive : SessionArchive {
    var sessions: List<SessionDetail> = emptyList()
    var failure: Exception? = null
    var deleteCalls = 0

    override suspend fun readAll(): List<SessionDetail> {
        failure?.let { throw it }
        return sessions
    }

    override suspend fun deleteAll() {
        failure?.let { throw it }
        deleteCalls++
        sessions = emptyList()
    }

    /** Nombre d'appels à [importSessions] et à [deleteSessions]. */
    var importCalls = 0
    val deletedIds = mutableListOf<Long>()
    var deleteSessionsCalls = 0
    private var nextId = 1_000L

    /** Appelé au début de [importSessions], pour simuler une session créée entre l'aperçu et la confirmation. */
    var beforeImport: () -> Unit = {}

    override suspend fun identities(): Set<SessionIdentity> {
        failure?.let { throw it }
        return sessions.map { it.session.identity }.toSet()
    }

    override suspend fun importSessions(sessions: List<SessionDetail>): ImportResult {
        failure?.let { throw it }
        importCalls++
        beforeImport()
        val present = this.sessions.map { it.session.identity }.toMutableSet()
        val added =
            sessions
                .filter {
                    present.add(it.session.identity)
                }.map { it.copy(session = it.session.copy(id = nextId++)) }
        this.sessions = this.sessions + added
        return ImportResult(added.map { it.session.id }, sessions.size - added.size)
    }

    override suspend fun deleteSessions(ids: List<Long>) {
        failure?.let { throw it }
        deleteSessionsCalls++
        deletedIds += ids
        sessions = sessions.filterNot { it.session.id in ids }
    }
}

fun completedSession(exerciseId: String = "crazy_eights") =
    SessionDetail(
        session =
            SessionRecord(
                id = 1,
                exerciseId = exerciseId,
                status = SessionStatus.COMPLETED,
                startedAtEpochMillis =
                    java.time.Instant
                        .parse("2026-10-17T07:00:00Z")
                        .toEpochMilli(),
                zoneId = "UTC",
                localEpochDay = 20_743,
            ),
        draws = emptyList(),
        answers = emptyList(),
        rubric = emptyList(),
    )
