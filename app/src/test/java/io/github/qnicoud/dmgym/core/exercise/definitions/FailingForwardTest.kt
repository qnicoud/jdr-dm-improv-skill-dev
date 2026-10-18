package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.ProgressRule
import io.github.qnicoud.dmgym.core.exercise.SlotDrawer
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.exercise.boundFields
import io.github.qnicoud.dmgym.core.tables.DistinctBy
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

class FailingForwardTest {
    private val definition = FailingForward.definition
    private val layout = definition.layout as FieldLayout.Fixed

    @Test
    fun `is registered, takes 4 minutes and is a warm-up`() {
        assertEquals(ExerciseId.FAILING_FORWARD, definition.id)
        assertSame(definition, ExerciseRegistry.Default[ExerciseId.FAILING_FORWARD])
        assertEquals(TimerPolicy.Required(4.minutes), definition.timer)
        assertTrue(definition.warmUp)
        assertTrue(ExerciseId.FAILING_FORWARD in ExerciseId.DAILY_WARM_UPS)
    }

    @Test
    fun `draws one situation and three distinct complication types`() {
        val situation = definition.slots.first { it.key == FailingForward.SITUATION } as TableSlot
        val types = definition.slots.first { it.key == FailingForward.TYPES } as TableSlot
        assertEquals(TableId.FailureSituations, situation.table)
        assertEquals(1, situation.count)
        assertEquals(TableId.ComplicationTypes, types.table)
        assertEquals(3, types.count)
        assertEquals(DistinctBy.Id, types.distinctBy)
    }

    @Test
    fun `has three fields bound to the drawn types, with a hint for each of the six`() {
        assertEquals(listOf("complication_1", "complication_2", "complication_3"), layout.fields.map { it.key })
        assertEquals(FailingForward.TYPES, layout.boundSlot)
        val ids =
            RealTables
                .file(TableId.ComplicationTypes)
                .entries
                .map { it.id }
                .toSet()
        assertEquals(ids, layout.hintByCode.keys)
        assertEquals(ProgressRule.FilledFields, definition.progress)
    }

    @Test
    fun `every draw imposes three different types on the three fields`() =
        runTest {
            (1..40).forEach { seed ->
                val draws = SlotDrawer(RealTables.repository, Random(seed)).drawAll(definition.slots)
                val codes = layout.boundFields(draws).map { checkNotNull(it).code }
                assertEquals(3, codes.distinct().size, "seed $seed")
            }
        }

    @Test
    fun `asks for a favourite at the end and two rubric questions`() {
        assertTrue(definition.endChoice)
        assertNotNull(definition.endPrompt)
        assertNotNull(definition.endChoiceLabel)
        assertEquals(2, definition.rubric.size)
        assertTrue(definition.acceptsFreeSubject)
    }
}
