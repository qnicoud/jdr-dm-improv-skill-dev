package io.github.qnicoud.dmgym.feature.settings

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.DemoEngine
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
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

    @Test
    fun `the fixed words of the export come from the labels`() {
        val labels = ExportLabels(playedFirst = "PF", rubricTitle = "GR", yes = "Y", no = "N")
        val text =
            SessionExportFormatter(labels).format(
                listOf(
                    detail(
                        session(1),
                        answers = listOf(AnswerRecord("a", 0, "négocier")),
                        rubric =
                            listOf(
                                RubricRecord("q", true),
                                RubricRecord("r", false),
                                RubricRecord("first_pick:a", true),
                            ),
                    ),
                ),
                exportedAt,
            )
        assertTrue(text.contains("GR :"))
        assertTrue(text.contains("q : Y"))
        assertTrue(text.contains("r : N"))
        assertTrue(text.contains("PF : négocier"))
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
        assertTrue(text.contains("str${R.string.connect_story_rubric_planned} : str${R.string.export_yes}"))
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
        assertTrue(text.contains("str${R.string.one_page_rubric_fits} : str${R.string.export_no}"))
    }

    @Test
    fun `the answer played first is exported as a sentence, not as a raw grid key`() {
        val text =
            formatWithRegistry(
                detail(
                    session(1, exercise = "decision_point"),
                    answers = listOf(AnswerRecord("answer_2", 0, "négocier")),
                    rubric = listOf(RubricRecord("first_pick:answer_2", true)),
                ),
            )
        assertTrue(text.contains("str${R.string.export_played_first} : négocier"))
        assertTrue(!text.contains("first_pick"))
    }

    @Test
    fun `the label of the chosen answer comes from the definition`() {
        val definition = DemoEngine.bound.copy(id = ExerciseId("demo_pick"), endChoiceLabel = R.string.export_rubric)
        val labels = ExportLabels.from(ExerciseRegistry(listOf(definition))) { "str$it" }
        val text =
            SessionExportFormatter(labels).format(
                listOf(
                    detail(
                        session(1, exercise = "demo_pick"),
                        answers = listOf(AnswerRecord("complication_2", 0, "un orage")),
                        rubric = listOf(RubricRecord("first_pick:complication_2", true)),
                    ),
                ),
                exportedAt,
            )
        assertTrue(text.contains("str${R.string.export_rubric} : un orage"))
        assertFalse(text.contains("str${R.string.export_played_first}"))
    }

    @Test
    fun `an unknown key falls back on the raw key`() {
        val text = formatWithRegistry(detail(session(1), answers = listOf(AnswerRecord("mystery_field", 0, "x"))))
        assertTrue(text.contains("mystery_field : x"))
    }

    @Test
    fun `an exercise shown one item at a time lists only reached items, numbered per item`() {
        val labels =
            ExportLabels(
                exercises = mapOf("yes_and" to "Oui, et…"),
                fields = mapOf(("yes_and" to "yes_and") to "Ma réponse", ("yes_and" to "block") to "Réflexe"),
                itemLabels = mapOf("yes_and" to { n, total -> "Offre $n sur $total" }),
                questionSlots = mapOf("yes_and" to "offers"),
                fieldOrders = mapOf("yes_and" to listOf("block", "yes_and")),
            )
        val draws = (0 until 5).map { DrawRecord("offers", it, "player_offers", "o$it", "Je veux $it") }
        val answers =
            listOf(
                AnswerRecord("yes_and", 0, "et zéro"),
                AnswerRecord("yes_and", 1, "et un"),
                AnswerRecord("block", 1, "non"),
            )
        val text =
            SessionExportFormatter(labels)
                .format(listOf(detail(session(1, "yes_and"), answers, draws)), exportedAt)
        assertTrue(text.contains("Je veux 1"))
        assertFalse(text.contains("Je veux 2"))
        assertTrue(text.indexOf("Offre 2 sur 5 · Réflexe : non") < text.indexOf("Offre 2 sur 5 · Ma réponse : et un"))
        assertTrue(text.contains("- Offre 1 sur 5 · Ma réponse : et zéro"))
    }
}
