package io.github.qnicoud.dmgym.core.exercise

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExerciseReducerTest {
    private val ready = ExerciseSession(DemoExercise.id)
    private val active = reduce(ready, ExerciseAction.Started(7))
    private val keyA = DemoExercise.keyA

    @Test
    fun `drawn replaces the draws`() {
        val draws = listOf(DemoExercise.draw())
        assertEquals(draws, reduce(ready, ExerciseAction.Drawn(draws)).draws)
    }

    @Test
    fun `started moves ready to active with the session id`() {
        assertEquals(Phase.Active, active.phase)
        assertEquals(7L, active.sessionId)
    }

    @Test
    fun `started is ignored once active`() {
        assertSame(active, reduce(active, ExerciseAction.Started(9)))
    }

    @Test
    fun `reroll before start replaces the slot and counts`() {
        val drawn = reduce(ready, ExerciseAction.Drawn(listOf(DemoExercise.draw(entry = "cave"))))
        val rerolled =
            reduce(drawn, ExerciseAction.Rerolled(DemoExercise.SLOT, listOf(DemoExercise.draw(entry = "pont"))))
        assertEquals(listOf("pont"), rerolled.draws.map { it.entryId })
        assertEquals(1, rerolled.rerollCount)
    }

    @Test
    fun `reroll after start keeps answers, phase and session`() {
        val typed = reduce(active, ExerciseAction.AnswerChanged(keyA, "idée"))
        val drawn = typed.copy(draws = listOf(DemoExercise.draw()))
        val rerolled = reduce(drawn, ExerciseAction.Rerolled(null, listOf(DemoExercise.draw(entry = "tour"))))
        assertEquals("idée", rerolled.textOf(keyA))
        assertEquals(Phase.Active, rerolled.phase)
        assertEquals(7L, rerolled.sessionId)
        assertEquals(listOf("tour"), rerolled.draws.map { it.entryId })
    }

    @Test
    fun `rerolling one slot keeps the order and resets its check`() {
        val first = DemoExercise.draw("s1", entry = "cave").copy(check = ElementCheck(true))
        val second = DemoExercise.draw("s2", entry = "pont").copy(check = ElementCheck(true))
        val state = reduce(ready, ExerciseAction.Drawn(listOf(first, second)))
        val rerolled = reduce(state, ExerciseAction.Rerolled("s1", listOf(DemoExercise.draw("s1", entry = "tour"))))
        assertEquals(listOf("s1" to "tour", "s2" to "pont"), rerolled.draws.map { it.slotKey to it.entryId })
        assertNull(rerolled.draws[0].check)
        assertEquals(ElementCheck(true), rerolled.draws[1].check)
    }

    @Test
    fun `answer changed stores the text and an unchanged text is a no-op`() {
        val typed = reduce(active, ExerciseAction.AnswerChanged(keyA, "un"))
        assertEquals("un", typed.textOf(keyA))
        assertSame(typed, reduce(typed, ExerciseAction.AnswerChanged(keyA, "un")))
    }

    @Test
    fun `an empty text on an unknown field creates nothing`() {
        assertSame(active, reduce(active, ExerciseAction.AnswerChanged(keyA, "")))
    }

    @Test
    fun `answers are addressed by group`() {
        val s = reduce(active, ExerciseAction.AnswerChanged(AnswerKey("npc", 0), "Aldo"))
        val t = reduce(s, ExerciseAction.AnswerChanged(AnswerKey("npc", 1), "Bela"))
        assertEquals("Aldo", t.textOf(AnswerKey("npc", 0)))
        assertEquals("Bela", t.textOf(AnswerKey("npc", 1)))
    }

    @Test
    fun `flag toggles on and off and keeps the text`() {
        val typed = reduce(active, ExerciseAction.AnswerChanged(keyA, "x"))
        val on = reduce(typed, ExerciseAction.FlagToggled(keyA))
        assertTrue(on.answers.getValue(keyA).flagged)
        assertEquals("x", on.textOf(keyA))
        assertFalse(reduce(on, ExerciseAction.FlagToggled(keyA)).answers.getValue(keyA).flagged)
    }

    @Test
    fun `flag on an empty field creates the answer`() {
        assertTrue(reduce(active, ExerciseAction.FlagToggled(keyA)).answers.getValue(keyA).flagged)
    }

    @Test
    fun `auto detection sets the check and keeps a manual override`() {
        val drawn =
            reduce(
                ready,
                ExerciseAction.Drawn(listOf(DemoExercise.draw(position = 0), DemoExercise.draw(position = 1))),
            )
        val detected = reduce(drawn, ExerciseAction.AutoDetected(mapOf(DrawKey(DemoExercise.SLOT, 0) to true)))
        assertEquals(ElementCheck(true, occurrences = 1), detected.draws[0].check)
        assertNull(detected.draws[1].check)
        val overridden = reduce(detected, ExerciseAction.CheckToggled(DrawKey(DemoExercise.SLOT, 0)))
        assertEquals(false, overridden.draws[0].check!!.manual)
        val redetected = reduce(overridden, ExerciseAction.AutoDetected(mapOf(DrawKey(DemoExercise.SLOT, 0) to false)))
        assertEquals(false, redetected.draws[0].check!!.manual)
        assertFalse(redetected.draws[0].check!!.checked)
    }

    private val first = DrawKey(DemoExercise.SLOT, 0)

    private fun uncheckedAfter(occurrences: Int): ExerciseSession {
        val drawn = reduce(ready, ExerciseAction.Drawn(listOf(DemoExercise.draw())))
        val detected = reduce(drawn, ExerciseAction.AutoDetected(mapOf(first to true), mapOf(first to occurrences)))
        return reduce(detected, ExerciseAction.CheckToggled(first))
    }

    private fun ExerciseSession.seen(occurrences: Int) =
        reduce(this, ExerciseAction.AutoDetected(mapOf(first to (occurrences > 0)), mapOf(first to occurrences)))

    @Test
    fun `an unchecked word stays unchecked while no new occurrence appears`() {
        val unchecked = uncheckedAfter(2)
        assertFalse(
            unchecked
                .seen(2)
                .draws[0]
                .check!!
                .checked,
        )
    }

    @Test
    fun `a new occurrence checks the word again`() {
        val rechecked = uncheckedAfter(1).seen(2)
        assertTrue(rechecked.draws[0].check!!.checked)
        assertNull(rechecked.draws[0].check!!.manual)
    }

    @Test
    fun `erasing text does not check the word again`() {
        val unchecked = uncheckedAfter(2)
        assertFalse(
            unchecked
                .seen(1)
                .draws[0]
                .check!!
                .checked,
        )
        assertFalse(
            unchecked
                .seen(1)
                .seen(2)
                .draws[0]
                .check!!
                .checked,
        )
        assertTrue(
            unchecked
                .seen(1)
                .seen(3)
                .draws[0]
                .check!!
                .checked,
        )
    }

    @Test
    fun `an unchecked word restored without its count adopts the next count`() {
        val restored =
            reduce(
                ready,
                ExerciseAction.Drawn(
                    listOf(DemoExercise.draw().copy(check = ElementCheck(auto = true, manual = false))),
                ),
            )
        assertFalse(
            restored
                .seen(3)
                .draws[0]
                .check!!
                .checked,
        )
        assertTrue(
            restored
                .seen(3)
                .seen(4)
                .draws[0]
                .check!!
                .checked,
        )
    }

    @Test
    fun `detection without news returns the same state`() {
        val drawn = reduce(ready, ExerciseAction.Drawn(listOf(DemoExercise.draw())))
        assertSame(drawn, reduce(drawn, ExerciseAction.AutoDetected(emptyMap())))
    }

    @Test
    fun `manual toggle works on an unchecked draw and toggles back`() {
        val drawn = reduce(ready, ExerciseAction.Drawn(listOf(DemoExercise.draw())))
        val key = DrawKey(DemoExercise.SLOT, 0)
        val on = reduce(drawn, ExerciseAction.CheckToggled(key))
        assertTrue(on.draws[0].check!!.checked)
        assertFalse(reduce(on, ExerciseAction.CheckToggled(key)).draws[0].check!!.checked)
    }

    @Test
    fun `manual toggle ignores other draws`() {
        val drawn =
            reduce(
                ready,
                ExerciseAction.Drawn(listOf(DemoExercise.draw(position = 0), DemoExercise.draw(position = 1))),
            )
        val toggled = reduce(drawn, ExerciseAction.CheckToggled(DrawKey(DemoExercise.SLOT, 1)))
        assertNull(toggled.draws[0].check)
    }

    @Test
    fun `next question advances and stops at the last one`() {
        val one = reduce(active, ExerciseAction.NextQuestion(lastIndex = 1))
        val two = reduce(one, ExerciseAction.NextQuestion(lastIndex = 1))
        assertEquals(1, one.currentQuestion)
        assertEquals(1, two.currentQuestion)
    }

    @Test
    fun `option changed stores the value`() {
        assertEquals(mapOf("video" to true), reduce(ready, ExerciseAction.OptionChanged("video", true)).options)
    }

    @Test
    fun `event fired records the id and upserts its draws`() {
        val base = reduce(ready, ExerciseAction.Drawn(listOf(DemoExercise.draw())))
        val fired = reduce(base, ExerciseAction.EventFired("shift", listOf(DemoExercise.draw("shift", entry = "gue"))))
        assertEquals(setOf("shift"), fired.firedEvents)
        assertEquals(listOf(DemoExercise.SLOT, "shift"), fired.draws.map { it.slotKey })
        val again =
            reduce(fired, ExerciseAction.EventFired("shift", listOf(DemoExercise.draw("shift", entry = "mine"))))
        assertEquals(listOf("cave", "gue"), again.draws.map { it.entryId })
    }

    @Test
    fun `finish ends only an active session`() {
        assertEquals(Phase.Ended, reduce(active, ExerciseAction.Finished).phase)
        assertSame(ready, reduce(ready, ExerciseAction.Finished))
    }

    @Test
    fun `assessment stores rating, gem and rubric and can be cleared`() {
        val ended = reduce(active, ExerciseAction.Finished)
        val rated = reduce(ended, ExerciseAction.Assessed(2, true, mapOf("q1" to true)))
        assertEquals(2, rated.selfRating)
        assertTrue(rated.isGem)
        assertEquals(mapOf("q1" to true), rated.rubric)
        val cleared = reduce(rated, ExerciseAction.Assessed(null, false, emptyMap()))
        assertNull(cleared.selfRating)
        assertFalse(cleared.isGem)
    }

    @Test
    fun `content ignores blank answers and draws`() {
        assertFalse(active.hasContent)
        assertFalse(reduce(active, ExerciseAction.AnswerChanged(keyA, "   ")).hasContent)
        assertTrue(reduce(active, ExerciseAction.AnswerChanged(keyA, " a ")).hasContent)
    }

    private val ended = reduce(active, ExerciseAction.Finished)

    @Test
    fun `an event fired twice is applied once`() {
        val first =
            reduce(active, ExerciseAction.EventFired("shift", listOf(DemoExercise.draw("shift", entry = "gue"))))
        val again =
            reduce(first, ExerciseAction.EventFired("shift", listOf(DemoExercise.draw("shift", entry = "mine"))))
        assertSame(first, again)
    }

    @Test
    fun `illegal transitions are ignored`() {
        val draws = listOf(DemoExercise.draw())
        assertSame(active, reduce(active, ExerciseAction.Drawn(draws)))
        assertSame(ended, reduce(ended, ExerciseAction.Drawn(draws)))
        assertSame(ended, reduce(ended, ExerciseAction.Rerolled(null, draws)))
        assertSame(ended, reduce(ended, ExerciseAction.AnswerChanged(keyA, "tard")))
        assertSame(ended, reduce(ended, ExerciseAction.EventFired("shift", draws)))
        assertSame(ready, reduce(ready, ExerciseAction.Assessed(1, true, emptyMap())))
        assertSame(active, reduce(active, ExerciseAction.Assessed(1, true, emptyMap())))
    }

    @Test
    fun `next question stays in bounds even without questions`() {
        assertEquals(0, reduce(active, ExerciseAction.NextQuestion(lastIndex = -1)).currentQuestion)
        assertEquals(0, reduce(active, ExerciseAction.NextQuestion(lastIndex = 0)).currentQuestion)
    }
}
