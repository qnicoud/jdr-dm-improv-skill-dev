package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ElementCheck
import io.github.qnicoud.dmgym.core.exercise.definitions.ConstraintBox
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChecklistItemsTest {
    private val place = Draw(ConstraintBox.PLACE, 0, "places", "p", "une cave")
    private val smell = Draw(ConstraintBox.WORDS, 0, "sensory_words", "s", "moisi", ElementCheck(auto = true))
    private val touch = Draw(ConstraintBox.WORDS, 1, "sensory_words", "t", "rugueux")
    private val sound =
        Draw(ConstraintBox.WORDS, 2, "sensory_words", "u", "grinçant", ElementCheck(false, manual = true))

    private val items = checklistItems(ConstraintBox.definition, listOf(place, smell, touch, sound))

    @Test
    fun `only the detected slot is checkable, and slots carry their label`() {
        assertEquals(listOf(false, true, true, true), items.map { it.checkable })
        assertEquals(R.string.constraint_box_place, items.first().label)
        assertEquals(R.string.constraint_box_words, items[1].label)
    }

    @Test
    fun `an automatic check is marked auto, a manual one is not`() {
        assertTrue(items[1].checked && items[1].auto)
        assertTrue(items[3].checked && !items[3].auto)
        assertFalse(items[2].checked || items[2].auto)
    }

    @Test
    fun `a manual uncheck of an automatic detection is unchecked and no longer auto`() {
        val unchecked = smell.copy(check = ElementCheck(auto = true, manual = false))
        val item = checklistItems(ConstraintBox.definition, listOf(unchecked)).single()
        assertFalse(item.checked)
        assertFalse(item.auto)
    }

    @Test
    fun `progress counts the checked among the checkable`() {
        assertEquals(2 to 3, items.progress())
    }

    @Test
    fun `keys follow slot and position`() {
        assertEquals(DrawKey(ConstraintBox.WORDS, 1), items[2].key)
    }
}
