package io.github.qnicoud.dmgym.core.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemeModeTest {
    @Test
    fun `system follows the phone, light and dark force the mode`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
    }

    @Test
    fun `parse falls back on system`() {
        assertEquals(ThemeMode.DARK, ThemeMode.parse("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse("SEPIA"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse(null))
    }

    @Test
    fun `reduce animations follows the system unless chosen`() {
        assertTrue(Settings().reduceAnimations.resolve(systemReduced = true))
        assertFalse(Settings().reduceAnimations.resolve(systemReduced = false))
        assertFalse(Settings(reduceAnimations = false).reduceAnimations.resolve(systemReduced = true))
        assertTrue(Settings(reduceAnimations = true).reduceAnimations.resolve(systemReduced = false))
    }
}
