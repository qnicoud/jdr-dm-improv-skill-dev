package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.core.exercise.ChoiceSlot
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.DistinctBy
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class ConstraintBoxTest {
    private val definition = ConstraintBox.definition

    @Test
    fun `is the constraint box exercise with a 5 minute countdown`() {
        assertEquals(ExerciseId.CONSTRAINT_BOX, definition.id)
        assertEquals(TimerPolicy.Required(5.minutes), definition.timer)
    }

    @Test
    fun `draws a place and three sensory words of distinct senses, without a domain choice`() {
        assertFalse(definition.slots.any { it is ChoiceSlot })
        val place = definition.slots.first { it.key == ConstraintBox.PLACE } as TableSlot
        val words = definition.slots.first { it.key == ConstraintBox.WORDS } as TableSlot
        assertEquals(TableId.Places, place.table)
        assertEquals(TableId.SensoryWords, words.table)
        assertEquals(3, words.count)
        assertEquals(DistinctBy.Category, words.distinctBy)
    }

    @Test
    fun `has a single field and detects the words in it`() {
        val layout = definition.layout as FieldLayout.Single
        assertEquals(ConstraintBox.TEXT, layout.field.key)
        assertEquals(listOf(ConstraintBox.WORDS), definition.detection?.slotKeys)
        assertEquals(ConstraintBox.TEXT, definition.detection?.field)
    }

    @Test
    fun `every real draw has three words of different senses`() =
        runTest {
            repeat(40) { seed ->
                val draws = RealTables.drawAll(definition, seed)
                val words = draws.filter { it.slotKey == ConstraintBox.WORDS }
                val senses = words.map { RealTables.entry(TableId.SensoryWords, it.entryId).category }
                assertEquals(3, words.size)
                assertEquals(3, senses.distinct().size, "senses $senses for seed $seed")
                assertEquals(1, draws.count { it.slotKey == ConstraintBox.PLACE })
            }
        }

    @Test
    fun `every drawn word carries its sense as category`() =
        runTest {
            repeat(20) { seed ->
                val words = RealTables.drawAll(definition, seed).filter { it.slotKey == ConstraintBox.WORDS }
                assertEquals(
                    words.map { RealTables.entry(TableId.SensoryWords, it.entryId).category },
                    words.map { it.category },
                )
                assertTrue(words.all { it.category != null })
            }
        }

    @Test
    fun `every sensory word carries keywords to detect`() =
        runTest {
            val entries = RealTables.repository.table(TableId.SensoryWords).entries
            assertTrue(entries.all { it.keywords.isNotEmpty() })
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
}
