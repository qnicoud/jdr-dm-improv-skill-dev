package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SenseTest {
    @Test
    fun `each sense category has its own icon and label`() {
        val senses = listOf("sight", "hearing", "smell", "touch", "taste").map { checkNotNull(Sense.fromCategory(it)) }
        assertEquals(5, senses.map { it.icon.name }.distinct().size)
        assertEquals(5, senses.map { it.label }.distinct().size)
    }

    @Test
    fun `an unknown or missing category has no sense`() {
        assertNull(Sense.fromCategory(null))
        assertNull(Sense.fromCategory("place"))
    }

    @Test
    fun `every category of the sensory words table is a known sense`() =
        runTest {
            val categories =
                RealTables.repository
                    .table(TableId.SensoryWords)
                    .entries
                    .map { it.category }
            categories.forEach { assertNotNull(Sense.fromCategory(it), "category $it") }
        }
}
