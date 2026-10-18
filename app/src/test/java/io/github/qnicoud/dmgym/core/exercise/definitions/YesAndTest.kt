package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.exercise.visibleFields
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class YesAndTest {
    private val definition = YesAnd.definition
    private val layout = definition.layout as FieldLayout.Sequential

    @Test
    fun `is registered, global 5 minute countdown, warm-up`() {
        assertEquals(ExerciseId.YES_AND, definition.id)
        assertNotNull(ExerciseRegistry.Default[ExerciseId.YES_AND])
        assertEquals(TimerPolicy.Required(5.minutes), definition.timer)
        assertEquals(5.minutes, ExerciseId.DEFAULT_DURATIONS[ExerciseId.YES_AND])
        assertTrue(ExerciseId.YES_AND in ExerciseId.DAILY_WARM_UPS)
        assertTrue(ExerciseId.YES_AND in ExerciseId.ALL)
    }

    @Test
    fun `draws 5 offers cycling the 4 categories and needs no free subject`() {
        val slot = definition.slots.single() as TableSlot
        assertEquals(TableId.PlayerOffers, slot.table)
        assertEquals(5, slot.count)
        assertEquals(listOf("social", "action", "exploration", "unexpected"), slot.categoryCycle)
        assertFalse(definition.acceptsFreeSubject)
    }

    @Test
    fun `reveals offers one by one, the yes-and is the field title`() {
        assertTrue(layout.revealProgressively)
        assertNotNull(layout.itemLabel)
        assertEquals(R.string.yes_and_title, layout.fields.last().label)
        assertEquals(R.string.yes_and_answer_hint, layout.fields.last().hint)
        assertEquals(listOf("block", "yes_and"), layout.fields.map { it.key })
    }

    @Test
    fun `the reflex field exists only with block first`() {
        assertEquals(listOf("yes_and"), layout.visibleFields(definition.resolveOptions(emptyMap())).map { it.key })
        assertEquals(
            listOf("block", "yes_and"),
            layout.visibleFields(definition.resolveOptions(mapOf(YesAnd.BLOCK_FIRST to true))).map { it.key },
        )
    }

    @Test
    fun `block first is off by default with a neutral help text`() {
        val option = definition.options.single()
        assertEquals(YesAnd.BLOCK_FIRST, option.key)
        assertFalse(option.default)
        assertNotNull(option.help)
    }

    @Test
    fun `the grid has a third question only with block first`() {
        assertEquals(2, definition.rubricFor(emptyMap()).size)
        assertEquals(3, definition.rubricFor(mapOf(YesAnd.BLOCK_FIRST to true)).size)
    }

    @Test
    fun `beats between two offers and has no choice at the end`() {
        assertEquals(TimedEvent.Steps(5, haptic = true), definition.events.single().timing)
        assertNull(definition.endChoiceLabel)
        assertFalse(definition.endChoice)
    }

    @Test
    fun `five distinct offers are drawn in category order from the real table`() =
        runTest {
            val draws = RealTables.drawAll(definition, seed = 3).sortedBy { it.position }
            assertEquals(5, draws.size)
            assertEquals(5, draws.map { it.entryId }.distinct().size)
            assertEquals(listOf("social", "action", "exploration", "unexpected", "social"), draws.map { it.category })
        }
}
