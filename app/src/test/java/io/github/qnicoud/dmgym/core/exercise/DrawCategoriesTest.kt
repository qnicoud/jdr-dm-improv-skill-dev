package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.history.DrawRecord
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DrawCategoriesTest {
    private val repository = RealTables.repository

    private fun word(
        entry: String,
        category: String? = null,
    ) = Draw("words", 0, "sensory_words", entry, entry, category = category)

    @Test
    fun `an old draw without category gets the one of its table entry`() =
        runTest {
            val sight =
                RealTables.repository
                    .table(io.github.qnicoud.dmgym.core.tables.TableId.SensoryWords)
                    .entries
                    .first { it.category == "sight" }
            val result = listOf(word(sight.id)).withCategories(repository)
            assertEquals("sight", result.single().category)
        }

    @Test
    fun `a stored category is kept, and draws that cannot be resolved are left alone`() =
        runTest {
            val card = Draw("card", 0, null, "7", "7")
            val unknownEntry = word("n-existe-pas")
            val unknownTable = Draw("x", 0, "table_inconnue", "a", "a")
            val kept = word("whatever", category = "taste")
            val result = listOf(kept, card, unknownEntry, unknownTable).withCategories(repository)
            assertEquals(listOf("taste", null, null, null), result.map { it.category })
        }

    @Test
    fun `an unreadable table leaves the draws unchanged`() =
        runTest {
            val broken = DemoExercise.tables(DemoExercise.FixtureSource(failing = true))
            val draws = listOf(word("poussiereux"))
            assertEquals(draws, draws.withCategories(broken))
        }

    @Test
    fun `archived draws get their category the same way`() =
        runTest {
            val place =
                repository
                    .table(io.github.qnicoud.dmgym.core.tables.TableId.StoryElements)
                    .entries
                    .first { it.category == "place" }
            val old = DrawRecord("place", 0, "story_elements", place.id, place.text)
            val kept = DrawRecord("x", 1, "story_elements", place.id, place.text, category = "line")
            val result = listOf(old, kept, DrawRecord("c", 2, null, "7", "7")).withRecordCategories(repository)
            assertEquals(listOf("place", "line", null), result.map { it.category })
        }
}
