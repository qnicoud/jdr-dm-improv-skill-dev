package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.minutes

/** Extensions génériques du moteur (ADR 0017 §1) : valeurs par défaut neutres et garde-fous de `init`. */
class EngineExtensionsTest {
    private val rubric2 = listOf(RubricQuestion("q1", 1), RubricQuestion("q2", 2))
    private val blockFirst = OptionCondition("block_first")

    private fun define(
        layout: FieldLayout,
        slots: List<DrawSlot>,
        rubric: List<RubricQuestion> = rubric2,
        options: List<ExerciseOption> = emptyList(),
        endChoiceLabel: StringResId? = null,
    ) = ExerciseDefinition(
        id = ExerciseId("ext"),
        name = 1,
        discipline = 2,
        instructions = 3,
        reminder = null,
        timer = TimerPolicy.Required(5.minutes),
        slots = slots,
        layout = layout,
        rubric = rubric,
        options = options,
        endChoiceLabel = endChoiceLabel,
    )

    private val types = TableSlot("types", TableId.ComplicationTypes, count = 3)
    private val offers = TableSlot("offers", TableId.PlayerOffers, count = 5)

    @Test
    fun `new field options are neutral by default`() {
        val spec = FieldSpec("a")
        assertNull(spec.icon)
        assertNull(spec.prefix)
        val fixed = FieldLayout.Fixed(listOf(spec))
        assertNull(fixed.boundSlot)
        assertTrue(fixed.hintByCode.isEmpty())
        val sequential = FieldLayout.Sequential("offers", listOf(spec))
        assertNull(sequential.itemLabel)
        assertEquals(false, sequential.revealProgressively)
    }

    @Test
    fun `a bound slot needs as many fields as draws`() {
        val fields = (1..3).map { FieldSpec("c$it") }
        define(FieldLayout.Fixed(fields, boundSlot = "types"), listOf(types))
        assertThrows<IllegalArgumentException> {
            define(FieldLayout.Fixed(fields.take(2), boundSlot = "types"), listOf(types))
        }
    }

    @Test
    fun `a bound slot must be a table slot of the definition`() {
        val fields = listOf(FieldSpec("c1"))
        assertThrows<IllegalArgumentException> { define(FieldLayout.Fixed(fields, boundSlot = "nope"), listOf(types)) }
        assertThrows<IllegalArgumentException> {
            define(FieldLayout.Fixed(fields, boundSlot = "n"), listOf(CardSlot("n")))
        }
    }

    @Test
    fun `sequential layout needs fields and an existing questions slot`() {
        assertThrows<IllegalArgumentException> { FieldLayout.Sequential("offers", emptyList()) }
        assertThrows<IllegalArgumentException> {
            define(FieldLayout.Sequential("missing", listOf(FieldSpec("a"))), listOf(offers))
        }
    }

    @Test
    fun `sequential field keys list every field in order`() {
        val d = define(FieldLayout.Sequential("offers", listOf(FieldSpec("block"), FieldSpec("yes"))), listOf(offers))
        assertEquals(listOf("block", "yes"), d.fieldKeys)
    }

    @Test
    fun `sequential visible fields follow the options`() {
        val layout =
            FieldLayout.Sequential(
                "offers",
                listOf(FieldSpec("block", visibleWhen = blockFirst), FieldSpec("yes")),
            )
        assertEquals(listOf("yes"), layout.visibleFields(mapOf("block_first" to false)).map { it.key })
        assertEquals(listOf("block", "yes"), layout.visibleFields(mapOf("block_first" to true)).map { it.key })
        assertEquals(listOf("yes"), layout.visibleFields(emptyMap()).map { it.key })
    }

    @Test
    fun `a conditional rubric question is asked only when its option holds`() {
        val d =
            define(
                FieldLayout.Single(FieldSpec("a")),
                listOf(offers),
                rubric = rubric2 + RubricQuestion("q3", 3, visibleWhen = blockFirst),
                options = listOf(ExerciseOption("block_first", 9)),
            )
        assertEquals(2, d.rubricFor(emptyMap()).size)
        assertEquals(3, d.rubricFor(mapOf("block_first" to true)).size)
    }

    @Test
    fun `every option combination must leave two or three questions`() {
        val options = listOf(ExerciseOption("block_first", 9))
        val conditional = RubricQuestion("q3", 3, visibleWhen = blockFirst)
        val layout = FieldLayout.Single(FieldSpec("a"))
        assertThrows<IllegalArgumentException> {
            define(layout, listOf(offers), rubric = listOf(RubricQuestion("q1", 1), conditional), options = options)
        }
        assertThrows<IllegalArgumentException> {
            define(layout, listOf(offers), rubric = rubric2 + RubricQuestion("a", 4) + conditional, options = options)
        }
    }

    @Test
    fun `bound fields read the code and name of each draw`() {
        val draws =
            listOf(
                Draw("types", 0, "complication_types", "cost", "Coût"),
                Draw("types", 1, "complication_types", "time", "Temps"),
            )
        val layout =
            FieldLayout.Fixed(
                listOf(FieldSpec("c1"), FieldSpec("c2")),
                boundSlot = "types",
                hintByCode = mapOf("cost" to 11),
            )
        val bound = layout.boundFields(draws)
        assertEquals(listOf("cost", "time"), bound.map { it?.code })
        assertEquals(listOf("Coût", "Temps"), bound.map { it?.name })
        assertEquals(listOf(11, null), bound.map { it?.hint })
    }

    @Test
    fun `bound fields are empty without a binding or without draws`() {
        val layout = FieldLayout.Fixed(listOf(FieldSpec("c1")))
        assertEquals(listOf(null), layout.boundFields(listOf(Draw("types", 0, null, "cost", "Coût"))))
        val bound = layout.copy(boundSlot = "types")
        assertEquals(listOf(null), bound.boundFields(emptyList()))
    }

    private fun session(
        phase: Phase,
        current: Int,
    ) = ExerciseSession(
        ExerciseId("ext"),
        phase = phase,
        currentQuestion = current,
        draws = (0..4).map { Draw("offers", it, "player_offers", "o$it", "Offre $it") },
    )

    @Test
    fun `without progressive reveal all questions are shown`() {
        val layout = FieldLayout.Sequential("offers", listOf(FieldSpec("a")))
        assertEquals(5, layout.revealedQuestions(session(Phase.Ready, 0)).size)
    }

    @Test
    fun `progressive reveal shows nothing before the start and the current question after`() {
        val layout = FieldLayout.Sequential("offers", listOf(FieldSpec("a")), revealProgressively = true)
        assertTrue(layout.revealedQuestions(session(Phase.Ready, 0)).isEmpty())
        assertEquals(listOf("Offre 2"), layout.revealedQuestions(session(Phase.Active, 2)).map { it.text })
    }

    @Test
    fun `progressive reveal keeps every question once the exercise has ended`() {
        val layout = FieldLayout.Sequential("offers", listOf(FieldSpec("a")), revealProgressively = true)
        assertEquals(5, layout.revealedQuestions(session(Phase.Ended, 4)).size)
    }

    @Test
    fun `the minute marker fills over each period and restarts`() {
        assertEquals(0f, minuteFraction(0, 60_000))
        assertEquals(0.5f, minuteFraction(30_000, 60_000))
        assertEquals(0f, minuteFraction(60_000, 60_000))
        assertEquals(0.25f, minuteFraction(135_000, 60_000))
    }

    @Test
    fun `the marker period comes from a periodic event`() {
        val base = define(FieldLayout.Single(FieldSpec("a")), listOf(offers))
        assertNull(base.rhythmPeriodMs)
        val periodic = base.copy(events = listOf(ScheduledEvent("beat", TimedEvent.Periodic(60_000, haptic = true))))
        assertEquals(60_000L, periodic.rhythmPeriodMs)
    }

    @Test
    fun `end choice label is carried by the definition`() {
        val d =
            define(FieldLayout.Single(FieldSpec("a")), listOf(offers), endChoiceLabel = R.string.export_played_first)
        assertEquals(R.string.export_played_first, d.endChoiceLabel)
    }

    @Test
    fun `an automatic event never moves the current question`() {
        val s = session(Phase.Active, 1)
        val after = reduce(s, ExerciseAction.EventFired("beat", emptyList()))
        assertEquals(1, after.currentQuestion)
    }
}
