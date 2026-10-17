package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.exercise.definitions.DecisionPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirstPickTest {
    @Test
    fun `the choice lives in the rubric under a prefixed key`() {
        val rubric = FirstPick.with(mapOf("playable" to true), "answer_2")
        assertEquals(mapOf("playable" to true, "first_pick:answer_2" to true), rubric)
        assertEquals("answer_2", FirstPick.chosen(rubric))
    }

    @Test
    fun `choosing another answer replaces the previous one and null clears it`() {
        val first = FirstPick.with(emptyMap(), "answer_1")
        val second = FirstPick.with(first, "answer_3")
        assertEquals("answer_3", FirstPick.chosen(second))
        assertEquals(1, second.size)
        assertNull(FirstPick.chosen(FirstPick.with(second, null)))
        assertTrue(FirstPick.with(second, null).isEmpty())
    }

    @Test
    fun `only prefixed keys are choice keys`() {
        assertTrue(FirstPick.isKey("first_pick:answer_1"))
        assertEquals(false, FirstPick.isKey("playable"))
        assertNull(FirstPick.chosen(mapOf("playable" to true)))
    }

    @Test
    fun `decision point offers its three answers as the choice`() {
        assertEquals(DecisionPoint.FIELD_KEYS, DecisionPoint.definition.firstPickFields())
        assertTrue(DecisionPoint.definition.rubric.none { FirstPick.isKey(it.key) })
    }
}
