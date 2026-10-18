package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.ProgressRule
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.ui.components.CategoryIcon
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class ThreeCluesTest {
    private val definition = ThreeClues.definition
    private val layout = definition.layout as FieldLayout.Fixed

    @Test
    fun `is the three clues exercise with a 5 minute countdown`() {
        assertEquals(ExerciseId.THREE_CLUES, definition.id)
        assertEquals(TimerPolicy.Required(5.minutes), definition.timer)
        assertFalse(definition.warmUp)
    }

    @Test
    fun `is registered with a 5 minute default duration`() {
        assertSame(definition, ExerciseRegistry.Default[ExerciseId.THREE_CLUES])
        assertEquals(5.minutes, ExerciseId.DEFAULT_DURATIONS.getValue(ExerciseId.THREE_CLUES))
        assertTrue(ExerciseId.THREE_CLUES in ExerciseId.ALL)
    }

    @Test
    fun `draws one conclusion`() {
        val slot = definition.slots.single() as TableSlot
        assertEquals(TableId.ClueConclusions, slot.table)
        assertEquals(1, slot.count)
    }

    @Test
    fun `has three fields in the object npc place registers, free order, no bound slot`() {
        assertEquals(listOf("object", "npc", "place"), layout.fields.map { it.icon })
        assertEquals(
            3,
            layout.fields
                .map { it.key }
                .distinct()
                .size,
        )
        assertNull(layout.boundSlot)
        assertEquals(ProgressRule.FilledFields, definition.progress)
        layout.fields.forEach {
            assertNotNull(it.label)
            assertNotNull(it.hint)
            assertNotNull(CategoryIcon.fromCategory(it.icon))
        }
    }

    @Test
    fun `asks two rubric questions and has no end choice`() {
        assertEquals(2, definition.rubricFor(emptyMap()).size)
        assertFalse(definition.endChoice)
    }

    @Test
    fun `accepts a free conclusion when the table is unreadable`() {
        assertTrue(definition.acceptsFreeSubject)
    }

    @Test
    fun `every real draw is a conclusion of the table`() =
        runTest {
            repeat(20) { seed ->
                val draw = RealTables.drawAll(definition, seed).single()
                RealTables.entry(TableId.ClueConclusions, draw.entryId)
            }
        }
}
