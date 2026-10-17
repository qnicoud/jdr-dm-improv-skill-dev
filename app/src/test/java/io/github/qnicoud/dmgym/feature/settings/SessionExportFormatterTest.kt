package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.core.history.AnswerRecord
import io.github.qnicoud.dmgym.core.history.DrawRecord
import io.github.qnicoud.dmgym.core.history.RubricRecord
import io.github.qnicoud.dmgym.core.history.SessionDetail
import io.github.qnicoud.dmgym.core.history.SessionRecord
import io.github.qnicoud.dmgym.core.history.SessionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class SessionExportFormatterTest {
    private val names = mapOf("crazy_eights" to "Crazy Eights", "one_page" to "One-Page Design")
    private val exportedAt = Instant.parse("2026-10-17T08:00:00Z")

    private fun session(
        id: Long,
        exercise: String = "crazy_eights",
        status: SessionStatus = SessionStatus.COMPLETED,
        started: String = "2026-10-16T12:03:00Z",
        gem: Boolean = false,
    ) = SessionRecord(
        id = id,
        exerciseId = exercise,
        status = status,
        startedAtEpochMillis = Instant.parse(started).toEpochMilli(),
        zoneId = "Europe/Paris",
        localEpochDay = 20_742,
        plannedDurationSec = 480,
        activeDurationSec = 485,
        overtimeSec = 5,
        selfRating = 2,
        isGem = gem,
    )

    private fun detail(
        session: SessionRecord,
        answers: List<AnswerRecord> = emptyList(),
        draws: List<DrawRecord> = emptyList(),
        rubric: List<RubricRecord> = emptyList(),
    ) = SessionDetail(session, draws, answers, rubric)

    private fun format(vararg details: SessionDetail) =
        SessionExportFormatter(names).format(details.toList(), exportedAt)

    @Test
    fun `an empty archive still gives a readable file`() {
        val text = format()
        assertTrue(text.contains("DM Gym"))
        assertTrue(text.contains("0 session"))
    }

    @Test
    fun `a session shows its exercise, local date, duration, draws and answers`() {
        val text =
            format(
                detail(
                    session(1, gem = true),
                    answers = listOf(AnswerRecord("idea", 0, "Un dragon timide")),
                    draws = listOf(DrawRecord("item", 0, "objects", "e1", "Une clé rouillée")),
                    rubric = listOf(RubricRecord("surprise", true)),
                ),
            )
        assertTrue(text.contains("Crazy Eights"))
        assertTrue(text.contains("2026-10-16 14:03"))
        assertTrue(text.contains("Europe/Paris"))
        assertTrue(text.contains("8 min 05 s"))
        assertTrue(text.contains("Un dragon timide"))
        assertTrue(text.contains("Une clé rouillée"))
        assertTrue(text.contains("surprise : oui"))
        assertTrue(text.contains("Bijou"))
    }

    @Test
    fun `sessions come in chronological order`() {
        val text =
            format(
                detail(session(2, exercise = "one_page", started = "2026-10-17T07:00:00Z")),
                detail(session(1, started = "2026-10-15T07:00:00Z")),
            )
        assertTrue(text.indexOf("Crazy Eights") < text.indexOf("One-Page Design"))
        assertTrue(text.contains("2 sessions"))
    }

    @Test
    fun `an empty unfinished session is left out, an unfinished one with content is kept`() {
        val empty = detail(session(1, status = SessionStatus.IN_PROGRESS), answers = listOf(AnswerRecord("a", 0, "  ")))
        val kept =
            detail(
                session(2, exercise = "one_page", status = SessionStatus.IN_PROGRESS),
                answers = listOf(AnswerRecord("a", 0, "brouillon")),
            )
        val text = format(empty, kept)
        assertFalse(text.contains("Crazy Eights"))
        assertTrue(text.contains("One-Page Design"))
        assertTrue(text.contains("inachevée"))
        assertTrue(text.contains("1 session"))
    }

    @Test
    fun `an unknown exercise keeps its identifier and a bad zone does not fail`() {
        val odd = session(1, exercise = "mystery").copy(zoneId = "Nowhere/Land")
        val text = format(detail(odd))
        assertTrue(text.contains("mystery"))
        assertEquals(1, Regex("""=== """).findAll(text).count())
    }
}
