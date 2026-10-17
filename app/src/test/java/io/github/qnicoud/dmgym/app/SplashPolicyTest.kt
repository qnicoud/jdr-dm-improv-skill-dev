package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.core.settings.ThemePalette
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SplashPolicyTest {
    @Test
    fun `each static palette has its own splash variant`() {
        assertEquals(SplashVariant.PARCHMENT, SplashVariant.of(ThemePalette.PARCHMENT))
        assertEquals(SplashVariant.FOREST, SplashVariant.of(ThemePalette.FOREST))
        assertEquals(SplashVariant.NIGHT, SplashVariant.of(ThemePalette.NIGHT))
        assertEquals(SplashVariant.MIST, SplashVariant.of(ThemePalette.MIST))
    }

    @Test
    fun `phone colors use the parchment splash`() {
        assertEquals(SplashVariant.PARCHMENT, SplashVariant.of(ThemePalette.DYNAMIC))
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
}
