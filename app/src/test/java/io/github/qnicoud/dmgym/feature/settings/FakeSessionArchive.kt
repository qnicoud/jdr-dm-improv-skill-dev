package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.core.history.SessionArchive
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus

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
