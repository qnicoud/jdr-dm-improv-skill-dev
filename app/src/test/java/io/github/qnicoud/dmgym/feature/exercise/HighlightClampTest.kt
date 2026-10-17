package io.github.qnicoud.dmgym.feature.exercise

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HighlightClampTest {
    @Test
    fun `ranges are turned into exclusive bounds inside the text`() {
        assertEquals(listOf(4 to 8), spanBounds(listOf(4..7), 10))
    }

    @Test
    fun `ranges past the end are clamped and empty ones dropped`() {
        assertEquals(listOf(8 to 10), spanBounds(listOf(8..20, 12..15), 10))
        assertEquals(emptyList<Pair<Int, Int>>(), spanBounds(listOf(-5..-2), 10))
    }
}
