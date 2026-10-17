package io.github.qnicoud.dmgym.core.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemePaletteTest {
    @Test
    fun `parchment is the default`() {
        assertEquals(ThemePalette.PARCHMENT, Settings().themePalette)
    }

    @Test
    fun `dynamic colors fall back to parchment below API 31`() {
        assertEquals(ThemePalette.PARCHMENT, ThemePalette.DYNAMIC.effective(sdkInt = 30))
        assertEquals(ThemePalette.DYNAMIC, ThemePalette.DYNAMIC.effective(sdkInt = 31))
    }

    @Test
    fun `static palettes are never altered by the API level`() {
        ThemePalette.entries.filter { it != ThemePalette.DYNAMIC }.forEach {
            assertEquals(it, it.effective(sdkInt = 26))
        }
    }

    @Test
    fun `dynamic is only offered from API 31`() {
        assertFalse(ThemePalette.DYNAMIC in ThemePalette.available(sdkInt = 30))
        assertTrue(ThemePalette.DYNAMIC in ThemePalette.available(sdkInt = 31))
        assertEquals(ThemePalette.PARCHMENT, ThemePalette.available(sdkInt = 26).first())
    }

    @Test
    fun `parsing an unknown or missing name gives parchment`() {
        assertEquals(ThemePalette.FOREST, ThemePalette.parse("FOREST"))
        assertEquals(ThemePalette.PARCHMENT, ThemePalette.parse("PURPLE"))
        assertEquals(ThemePalette.PARCHMENT, ThemePalette.parse(null))
    }
}
