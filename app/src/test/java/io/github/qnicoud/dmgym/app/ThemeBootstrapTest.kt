package io.github.qnicoud.dmgym.app

import io.github.qnicoud.dmgym.core.settings.Settings
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.settings.ThemeMode
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemeBootstrapTest {
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Unconfined)

    @AfterEach
    fun tearDown() = job.cancel()

    private class SourceRepository(
        override val settings: Flow<Settings>,
    ) : SettingsRepository {
        override suspend fun update(transform: (Settings) -> Settings) = error("not used")
    }

    @Test
    fun `is not ready until the first settings are read`() {
        val source = MutableSharedFlow<Settings>()
        val bootstrap = ThemeBootstrap(SourceRepository(source), scope)
        assertNull(bootstrap.palette.value)
        assertFalse(bootstrap.isReady)
        runBlocking { source.emit(Settings(themePalette = ThemePalette.FOREST)) }
        assertEquals(ThemePalette.FOREST, bootstrap.palette.value)
        assertTrue(bootstrap.isReady)
    }

    @Test
    fun `follows later palette changes`() {
        val source = MutableSharedFlow<Settings>()
        val bootstrap = ThemeBootstrap(SourceRepository(source), scope)
        runBlocking {
            source.emit(Settings(themePalette = ThemePalette.FOREST))
            source.emit(Settings(themePalette = ThemePalette.MIST))
        }
        assertEquals(ThemePalette.MIST, bootstrap.palette.value)
    }

    @Test
    fun `a failing read falls back to parchment instead of blocking the splash screen`() {
        val bootstrap = ThemeBootstrap(SourceRepository(flow { error("disk") }), scope)
        assertEquals(ThemePalette.PARCHMENT, bootstrap.palette.value)
        assertTrue(bootstrap.isReady)
    }

    @Test
    fun `exposes the theme mode and the animation choice with the palette`() {
        val source = MutableSharedFlow<Settings>()
        val bootstrap = ThemeBootstrap(SourceRepository(source), scope)
        assertNull(bootstrap.appearance.value)
        runBlocking {
            source.emit(
                Settings(themePalette = ThemePalette.NIGHT, themeMode = ThemeMode.DARK, reduceAnimations = true),
            )
        }
        assertEquals(
            Appearance(ThemePalette.NIGHT, ThemeMode.DARK, reduceAnimations = true),
            bootstrap.appearance.value,
        )
        assertEquals(ThemePalette.NIGHT, bootstrap.palette.value)
    }

    @Test
    fun `a failing read gives the default appearance`() {
        val bootstrap = ThemeBootstrap(SourceRepository(flow { error("disk") }), scope)
        assertEquals(Appearance(), bootstrap.appearance.value)
    }
}
