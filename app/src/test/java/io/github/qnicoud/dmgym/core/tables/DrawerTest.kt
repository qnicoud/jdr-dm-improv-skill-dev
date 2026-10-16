package io.github.qnicoud.dmgym.core.tables

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class DrawerTest {
    private val table = TestTables.fixtureSmall()

    private fun drawer(seed: Int = 42) = Drawer(Random(seed))

    @Test
    fun `draws without replacement`() {
        val drawn = drawer().draw(table, count = 6)

        assertEquals(6, drawn.map { it.id }.toSet().size)
    }

    @Test
    fun `same seed gives same draw`() {
        assertEquals(drawer(42).draw(table, 3), drawer(42).draw(table, 3))
    }

    @Test
    fun `different seeds eventually give different draws`() {
        val draws = (1..20).map { drawer(it).draw(table, 3).map { e -> e.id } }.toSet()

        assertTrue(draws.size > 1)
    }

    @Test
    fun `count zero returns an empty list`() {
        assertEquals(emptyList<TableEntry>(), drawer().draw(table, 0))
    }

    @Test
    fun `negative count is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { drawer().draw(table, -1) }
    }

    @Test
    fun `filters by category`() {
        val drawn = drawer().draw(table, 2, category = "a")

        assertEquals(setOf("cave", "pont"), drawn.map { it.id }.toSet())
    }

    @Test
    fun `filters by difficulty`() {
        val drawn = drawer().draw(table, 2, difficulty = 3..3)

        assertEquals(setOf("mine", "puits"), drawn.map { it.id }.toSet())
    }

    @Test
    fun `filters by tags`() {
        val drawn = drawer().draw(table, 2, tagFilter = { "rank:high" in it })

        assertEquals(setOf("cave", "tour"), drawn.map { it.id }.toSet())
    }

    @Test
    fun `distinctBy category yields one entry per category`() {
        repeat(20) { seed ->
            val drawn = drawer(seed).draw(table, 3, distinctBy = DistinctBy.Category)

            assertEquals(3, drawn.map { it.category }.toSet().size)
        }
    }

    @Test
    fun `distinctBy category fails when there are fewer categories than requested`() {
        assertThrows(NotEnoughEntriesException::class.java) {
            drawer().draw(table, 5, distinctBy = DistinctBy.Category)
        }
    }

    @Test
    fun `excluded ids are avoided when possible`() {
        repeat(20) { seed ->
            val drawn = drawer(seed).draw(table, 5, exclude = setOf("cave"))

            assertTrue(drawn.none { it.id == "cave" })
        }
    }

    @Test
    fun `exclusion is relaxed when too few entries remain`() {
        val drawn = drawer().draw(table, 6, exclude = setOf("cave", "pont"))

        assertEquals(6, drawn.size)
    }

    @Test
    fun `exclusion is a preference filled up with excluded entries only if needed`() {
        repeat(20) { seed ->
            val drawn = drawer(seed).draw(table, 5, exclude = setOf("cave", "pont"))

            assertEquals(5, drawn.size)
            assertEquals(1, drawn.count { it.id in setOf("cave", "pont") })
        }
    }

    @Test
    fun `exclusion is lifted before difficulty`() {
        repeat(20) { seed ->
            val drawn = drawer(seed).draw(table, 2, difficulty = 3..3, exclude = setOf("mine"))

            assertEquals(setOf("mine", "puits"), drawn.map { it.id }.toSet())
        }
    }

    @Test
    fun `non excluded entries come first within a difficulty tier`() {
        repeat(20) { seed ->
            val drawn = drawer(seed).draw(table, 1, difficulty = 3..3, exclude = setOf("mine"))

            assertEquals("puits", drawn.single().id)
        }
    }

    @Test
    fun `entries without category are all distinct`() {
        val uncategorized = TestTables.fixtureSmall().copy(entries = List(3) { TableEntry("e$it", "t$it", 1) })

        val drawn = drawer().draw(uncategorized, 3, distinctBy = DistinctBy.Category)

        assertEquals(3, drawn.size)
    }

    @Test
    fun `difficulty is relaxed after exclusion`() {
        val drawn = drawer().draw(table, 4, difficulty = 3..3)

        assertEquals(4, drawn.size)
    }

    @Test
    fun `category is never relaxed`() {
        val error =
            assertThrows(NotEnoughEntriesException::class.java) {
                drawer().draw(table, 3, category = "a")
            }

        assertEquals(3, error.requested)
        assertEquals(2, error.available)
    }

    @Test
    fun `fails when the table is too small`() {
        assertThrows(NotEnoughEntriesException::class.java) { drawer().draw(table, 7) }
    }
}
