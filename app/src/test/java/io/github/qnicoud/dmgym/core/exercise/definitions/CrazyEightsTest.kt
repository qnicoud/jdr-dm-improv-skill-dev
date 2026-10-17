package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.ProgressRule
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class CrazyEightsTest {
    private val definition = CrazyEights.definition

    @Test
    fun `is the crazy eights exercise with an 8 minute countdown`() {
        assertEquals(ExerciseId.CRAZY_EIGHTS, definition.id)
        assertEquals(TimerPolicy.Required(8.minutes), definition.timer)
    }

    @Test
    fun `draws one problem`() {
        val slot = definition.slots.single() as TableSlot
        assertEquals(TableId.Problems, slot.table)
        assertEquals(1, slot.count)
    }

    @Test
    fun `has 8 numbered fields and counts the filled ones`() {
        val layout = definition.layout as FieldLayout.Fixed
        assertEquals((1..8).map { "idea$it" }, layout.fields.map { it.key })
        assertEquals(ProgressRule.FilledFields, definition.progress)
    }

    @Test
    fun `does not detect anything and accepts a free subject`() {
        assertNull(definition.detection)
        assertTrue(definition.acceptsFreeSubject)
    }

    @Test
    fun `reminds every minute with a haptic tick`() {
        val event = definition.events.single()
        assertEquals(TimedEvent.Periodic(60_000, haptic = true), event.timing)
    }

    @Test
    fun `has two distinct rubric questions`() {
        assertEquals(
            2,
            definition.rubric
                .map { it.key }
                .distinct()
                .size,
        )
    }

    @Test
    fun `draws a problem from the real table`() =
        runTest {
            val draws = RealTables.drawAll(definition, seed = 1)
            assertEquals(1, draws.size)
            assertEquals("problems", draws.single().tableId)
        }
}
