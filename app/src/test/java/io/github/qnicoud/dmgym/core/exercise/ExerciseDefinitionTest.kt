package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.tables.TableId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class ExerciseDefinitionTest {
    private val demo = DemoExercise.definition

    @Test
    fun `field keys follow the layout`() {
        assertEquals(listOf("a", "b"), demo.fieldKeys)
        assertEquals(listOf("x"), demo.copy(layout = FieldLayout.Single(FieldSpec("x"))).fieldKeys)
        assertEquals(
            listOf("q"),
            demo.copy(layout = FieldLayout.Sequential("prompt", listOf(FieldSpec("q")))).fieldKeys,
        )
        assertEquals(
            listOf("n", "m"),
            demo.copy(layout = FieldLayout.RepeatableGroup(listOf(FieldSpec("n"), FieldSpec("m")), 5)).fieldKeys,
        )
    }

    @Test
    fun `timer policy exposes its default duration`() {
        assertEquals(5.minutes, TimerPolicy.Required(5.minutes).defaultDuration)
        assertEquals(15.minutes, TimerPolicy.Optional(15.minutes).defaultDuration)
        assertNull(TimerPolicy.None.defaultDuration)
    }

    @Test
    fun `rubric must have two or three questions`() {
        assertThrows(IllegalArgumentException::class.java) { demo.copy(rubric = demo.rubric.take(1)) }
        assertThrows(IllegalArgumentException::class.java) { demo.copy(rubric = List(4) { RubricQuestion("q$it", 1) }) }
    }

    @Test
    fun `slot keys must be unique`() {
        val slot = TableSlot("s", TableId.Places)
        assertThrows(IllegalArgumentException::class.java) { demo.copy(slots = listOf(slot, slot)) }
    }

    @Test
    fun `slots validate their arguments`() {
        assertThrows(IllegalArgumentException::class.java) { TableSlot("s", TableId.Places, count = 0) }
        assertThrows(IllegalArgumentException::class.java) { CardSlot("c", 5..1) }
        assertThrows(IllegalArgumentException::class.java) { ChoiceSlot("k", listOf("a"), default = "b") }
        assertThrows(IllegalArgumentException::class.java) { FieldLayout.RepeatableGroup(emptyList(), 0) }
        assertEquals("a", ChoiceSlot("k", listOf("a", "b")).default)
    }
}
