package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class ConnectStoryTest {
    private val definition = ConnectStory.definition
    private val categories = listOf("place", "object", "trait", "sensory", "line")

    @Test
    fun `is the connect the story exercise with a 10 minute countdown`() {
        assertEquals(ExerciseId.CONNECT_STORY, definition.id)
        assertEquals(TimerPolicy.Required(10.minutes), definition.timer)
    }

    @Test
    fun `draws one element per category from the story elements table`() {
        val slots = definition.slots.map { it as TableSlot }
        assertEquals(categories, slots.map { it.category })
        assertEquals(categories, slots.map { it.key })
        assertTrue(slots.all { it.table == TableId.StoryElements && it.count == 1 })
    }

    @Test
    fun `has a single field and detects the five elements`() {
        assertTrue(definition.layout is FieldLayout.Single)
        assertEquals(categories, definition.detection?.slotKeys)
        assertEquals(ConnectStory.TEXT, definition.detection?.field)
        assertTrue(definition.collapsiblePanel)
    }

    @Test
    fun `every real draw has one entry of each category`() =
        runTest {
            repeat(30) { seed ->
                val draws = RealTables.drawAll(definition, seed)
                val found = draws.map { RealTables.entry(TableId.StoryElements, it.entryId).category }
                assertEquals(categories, found, "seed $seed")
            }
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
