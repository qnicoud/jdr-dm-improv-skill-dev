package io.github.qnicoud.dmgym.core.detection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DamerauLevenshteinTest {
    @Test
    fun `identical and empty strings`() {
        assertEquals(0, DamerauLevenshtein.distance("pont", "pont"))
        assertEquals(0, DamerauLevenshtein.distance("", ""))
        assertEquals(4, DamerauLevenshtein.distance("", "pont"))
        assertEquals(4, DamerauLevenshtein.distance("pont", ""))
    }

    @Test
    fun `single edits cost one`() {
        assertEquals(1, DamerauLevenshtein.distance("chandelier", "chandelie"))
        assertEquals(1, DamerauLevenshtein.distance("chandelier", "chandelierr"))
        assertEquals(1, DamerauLevenshtein.distance("chandelier", "chandelior"))
    }

    @Test
    fun `adjacent transposition costs one`() {
        assertEquals(1, DamerauLevenshtein.distance("chandelier", "chadnelier"))
        assertEquals(1, DamerauLevenshtein.distance("ab", "ba"))
    }

    @Test
    fun `larger differences`() {
        assertEquals(3, DamerauLevenshtein.distance("kitten", "sitting"))
        assertEquals(2, DamerauLevenshtein.distance("abcd", "badc"))
    }
}
