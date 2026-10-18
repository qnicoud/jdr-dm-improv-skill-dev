package io.github.qnicoud.dmgym.ui.components

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.definitions.RealTables
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CategoryIconTest {
    private val senses = listOf("sight", "hearing", "smell", "touch", "taste")
    private val story = listOf("place", "object", "trait", "sensory", "line", "npc")

    @Test
    fun `each category has its own icon and label`() {
        val all = (senses + story).map { checkNotNull(CategoryIcon.fromCategory(it)) }
        assertEquals(11, all.map { it.icon.name }.distinct().size)
        assertEquals(11, all.map { it.label }.distinct().size)
    }

    @Test
    fun `story elements are labelled like their slot`() {
        assertEquals(R.string.connect_story_place, CategoryIcon.fromCategory("place")?.label)
        assertEquals(R.string.connect_story_line, CategoryIcon.fromCategory("line")?.label)
    }

    @Test
    fun `the npc icon is labelled for talkback`() {
        assertEquals(R.string.category_npc, CategoryIcon.fromCategory("npc")?.label)
    }

    @Test
    fun `an unknown or missing category has no icon`() {
        assertNull(CategoryIcon.fromCategory(null))
        assertNull(CategoryIcon.fromCategory("inconnue"))
    }

    @Test
    fun `every category of the sensory words and story elements tables has an icon`() =
        runTest {
            listOf(TableId.SensoryWords, TableId.StoryElements).forEach { id ->
                RealTables.repository
                    .table(id)
                    .entries
                    .forEach { assertNotNull(CategoryIcon.fromCategory(it.category), "${id.fileName} ${it.category}") }
            }
        }
}
