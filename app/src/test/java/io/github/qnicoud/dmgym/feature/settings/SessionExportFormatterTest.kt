package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
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
        SessionExportFormatter(ExportLabels(exercises = names)).format(details.toList(), exportedAt)

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

    private val registryLabels = ExportLabels.from(ExerciseRegistry.Default) { "str$it" }

    private fun formatWithRegistry(vararg details: SessionDetail) =
        SessionExportFormatter(registryLabels).format(details.toList(), exportedAt)

    @Test
    fun `a Connect the Story export uses the labels of the definition, not the raw keys`() {
        val text =
            formatWithRegistry(
                detail(
                    session(1, exercise = "connect_story"),
                    draws = listOf(DrawRecord("place", 0, "story_elements", "e1", "Une crypte")),
                    answers = listOf(AnswerRecord("scene", 0, "Il pleuvait.")),
                    rubric = listOf(RubricRecord("connection_felt_planned", true)),
                ),
            )
        assertTrue(text.contains("str${R.string.exercise_connect_story}"))
        assertTrue(text.contains("str${R.string.connect_story_place} : Une crypte"))
        assertTrue(text.contains("str${R.string.connect_story_field_label} : Il pleuvait."))
        assertTrue(text.contains("str${R.string.connect_story_rubric_planned} : oui"))
        assertFalse(text.contains("connection_felt_planned"))
        assertFalse(text.contains("scene :"))
    }

    @Test
    fun `a One-Page export labels the fields and numbers the characters`() {
        val text =
            formatWithRegistry(
                detail(
                    session(1, exercise = "one_page"),
                    answers =
                        listOf(
                            AnswerRecord("npc_name", 0, "Brenna"),
                            AnswerRecord("npc_name", 1, "Odo"),
                            AnswerRecord("if_pcs_idle", 0, "Le pont brûle."),
                        ),
                    rubric = listOf(RubricRecord("fits", false)),
                ),
            )
        assertTrue(text.contains("str${R.string.one_page_name_label} : Brenna"))
        assertTrue(text.contains("str${R.string.one_page_name_label} (2) : Odo"))
        assertTrue(text.contains("str${R.string.one_page_inaction_label} : Le pont brûle."))
        assertTrue(text.contains("str${R.string.one_page_rubric_fits} : non"))
    }

    @Test
    fun `an unknown key falls back on the raw key`() {
        val text = formatWithRegistry(detail(session(1), answers = listOf(AnswerRecord("mystery_field", 0, "x"))))
        assertTrue(text.contains("mystery_field : x"))
    }
}
