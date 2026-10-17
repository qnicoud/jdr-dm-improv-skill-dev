package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.core.settings.ThemePalette
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SplashPolicyTest {
    @Test
    fun `each static palette has a light and a dark splash variant`() {
        assertEquals(SplashVariant.PARCHMENT_LIGHT, SplashVariant.of(ThemePalette.PARCHMENT, dark = false))
        assertEquals(SplashVariant.PARCHMENT_DARK, SplashVariant.of(ThemePalette.PARCHMENT, dark = true))
        assertEquals(SplashVariant.FOREST_LIGHT, SplashVariant.of(ThemePalette.FOREST, dark = false))
        assertEquals(SplashVariant.FOREST_DARK, SplashVariant.of(ThemePalette.FOREST, dark = true))
        assertEquals(SplashVariant.NIGHT_LIGHT, SplashVariant.of(ThemePalette.NIGHT, dark = false))
        assertEquals(SplashVariant.NIGHT_DARK, SplashVariant.of(ThemePalette.NIGHT, dark = true))
        assertEquals(SplashVariant.MIST_LIGHT, SplashVariant.of(ThemePalette.MIST, dark = false))
        assertEquals(SplashVariant.MIST_DARK, SplashVariant.of(ThemePalette.MIST, dark = true))
    }

    @Test
    fun `phone colors use the parchment splash in both modes`() {
        assertEquals(SplashVariant.PARCHMENT_LIGHT, SplashVariant.of(ThemePalette.DYNAMIC, dark = false))
        assertEquals(SplashVariant.PARCHMENT_DARK, SplashVariant.of(ThemePalette.DYNAMIC, dark = true))
    }

    @Test
    fun `a cold start stays on screen until one second has passed`() {
        val policy = SplashPolicy(isColdStart = true)
        assertTrue(policy.keepOnScreen(settingsReady = true, elapsedMillis = 0))
        assertTrue(policy.keepOnScreen(settingsReady = true, elapsedMillis = 999))
        assertFalse(policy.keepOnScreen(settingsReady = true, elapsedMillis = 1_000))
    }

    @Test
    fun `a cold start is never lengthened beyond the settings read`() {
        val policy = SplashPolicy(isColdStart = true)
        assertTrue(policy.keepOnScreen(settingsReady = false, elapsedMillis = 1_500))
        assertFalse(policy.keepOnScreen(settingsReady = true, elapsedMillis = 1_500))
    }

    @Test
    fun `a warm start only waits for the settings`() {
        val policy = SplashPolicy(isColdStart = false)
        assertFalse(policy.keepOnScreen(settingsReady = true, elapsedMillis = 0))
        assertTrue(policy.keepOnScreen(settingsReady = false, elapsedMillis = 0))
    }

    @Test
    fun `the cold start is consumed once and can be reset`() {
        ColdStartTracker.reset()
        assertTrue(ColdStartTracker.consume())
        assertFalse(ColdStartTracker.consume())
        ColdStartTracker.reset()
        assertTrue(ColdStartTracker.consume())
        ColdStartTracker.reset()
    }

    @Test
    fun `the system is only asked again when the variant changes`() {
        var stored: String? = null
        val applied = mutableListOf<SplashVariant>()
        val sync = SplashThemeSync({ stored }, { stored = it }, { applied += it })
        sync.onVariant(SplashVariant.FOREST_DARK)
        sync.onVariant(SplashVariant.FOREST_DARK)
        sync.onVariant(SplashVariant.MIST_LIGHT)
        assertEquals(listOf(SplashVariant.FOREST_DARK, SplashVariant.MIST_LIGHT), applied)
        val restarted = SplashThemeSync({ stored }, { stored = it }, { applied += it })
        restarted.onVariant(SplashVariant.MIST_LIGHT)
        assertEquals(2, applied.size)
    }
}
