package io.github.qnicoud.dmgym.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import io.github.qnicoud.dmgym.core.settings.ThemePalette
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.pow

/**
 * Contraste WCAG 2.x calculé sur les jeux de couleurs réels (design-system §2.4), pour chaque palette statique
 * (Parchemin, Forêt, Nuit, Brume), en clair et en sombre.
 *
 * La palette `DYNAMIC` n'est pas testée : ses couleurs sont générées par le système (Material You, API 31+),
 * conformes aux règles de contraste Material, et dépendent du fond d'écran, donc inaccessibles en JVM.
 */
class ContrastTest {
    private fun channel(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun ratio(
        a: Color,
        b: Color,
    ): Double {
        val (hi, lo) = luminance(a).coerceAtLeast(luminance(b)) to luminance(a).coerceAtMost(luminance(b))
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun textPairs(s: ColorScheme): Map<String, Pair<Color, Color>> =
        mapOf(
            "onPrimary/primary" to (s.onPrimary to s.primary),
            "onPrimaryContainer/primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
            "onSecondary/secondary" to (s.onSecondary to s.secondary),
            "onSecondaryContainer/secondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
            "onTertiary/tertiary" to (s.onTertiary to s.tertiary),
            "onTertiaryContainer/tertiaryContainer" to (s.onTertiaryContainer to s.tertiaryContainer),
            "onError/error" to (s.onError to s.error),
            "onErrorContainer/errorContainer" to (s.onErrorContainer to s.errorContainer),
            "onBackground/background" to (s.onBackground to s.background),
            "onSurface/surface" to (s.onSurface to s.surface),
            "onSurface/surfaceContainerLow" to (s.onSurface to s.surfaceContainerLow),
            "onSurface/surfaceContainerHighest" to (s.onSurface to s.surfaceContainerHighest),
            "onSurface/tertiaryContainer" to (s.onSurface to s.tertiaryContainer),
            "onSurfaceVariant/surfaceVariant" to (s.onSurfaceVariant to s.surfaceVariant),
            "onSurfaceVariant/surfaceContainerLow" to (s.onSurfaceVariant to s.surfaceContainerLow),
            "onSurfaceVariant/surfaceContainer" to (s.onSurfaceVariant to s.surfaceContainer),
            "inverseOnSurface/inverseSurface" to (s.inverseOnSurface to s.inverseSurface),
            "onSurface/surfaceContainerLowest" to (s.onSurface to s.surfaceContainerLowest),
            "onSurface/surfaceContainer" to (s.onSurface to s.surfaceContainer),
            "onSurface/surfaceContainerHigh" to (s.onSurface to s.surfaceContainerHigh),
            "onSurface/surfaceDim" to (s.onSurface to s.surfaceDim),
            "onSurface/surfaceBright" to (s.onSurface to s.surfaceBright),
            "onSurfaceVariant/surfaceContainerHigh" to (s.onSurfaceVariant to s.surfaceContainerHigh),
            "onSurfaceVariant/surfaceContainerHighest" to (s.onSurfaceVariant to s.surfaceContainerHighest),
            "onSecondaryContainer/surfaceContainer" to (s.onSecondaryContainer to s.surfaceContainer),
            "onSecondaryContainer/surface" to (s.onSecondaryContainer to s.surface),
            "onPrimaryContainer/surface" to (s.onPrimaryContainer to s.surface),
            "onTertiaryContainer/surface" to (s.onTertiaryContainer to s.surface),
            "secondary/surface" to (s.secondary to s.surface),
            "tertiary/surface" to (s.tertiary to s.surface),
            "inversePrimary/inverseSurface" to (s.inversePrimary to s.inverseSurface),
            "primary/surface" to (s.primary to s.surface),
            "error/surface" to (s.error to s.surface),
        )

    private fun assertScheme(
        name: String,
        scheme: ColorScheme,
    ) {
        textPairs(scheme).forEach { (pair, colors) ->
            val r = ratio(colors.first, colors.second)
            assertTrue(r >= 4.5, "$name $pair: contraste $r < 4,5")
        }
    }

    /** Chaque palette statique, en clair et en sombre (la palette dynamique dépend du téléphone). */
    private val staticSchemes: List<Pair<String, ColorScheme>> =
        ThemePalette.entries.filter { it != ThemePalette.DYNAMIC }.flatMap { palette ->
            listOf(
                "${palette.name} clair" to staticColorScheme(palette, dark = false),
                "${palette.name} sombre" to staticColorScheme(palette, dark = true),
            )
        }

    @Test
    fun `parchment keeps the original schemes`() {
        assertSame(LightColors, staticColorScheme(ThemePalette.PARCHMENT, dark = false))
        assertSame(DarkColors, staticColorScheme(ThemePalette.PARCHMENT, dark = true))
    }

    @Test
    fun `there are at least three Material palettes besides parchment`() {
        assertTrue(ThemePalette.entries.count { it != ThemePalette.DYNAMIC && it != ThemePalette.PARCHMENT } >= 3)
    }

    @Test
    fun `every static palette reaches AA on text pairs in both modes`() {
        staticSchemes.forEach { (name, scheme) -> assertScheme(name, scheme) }
    }

    @Test
    fun `outlines reach 3 to 1 on surfaces in every static palette`() {
        staticSchemes.forEach { (name, s) ->
            listOf(s.surface, s.surfaceContainer).forEach {
                assertTrue(ratio(s.outline, it) >= 3.0, "$name outline/$it")
            }
        }
    }

    @Test
    fun `light and dark variants of a palette differ`() {
        ThemePalette.entries.filter { it != ThemePalette.DYNAMIC }.forEach {
            assertNotEquals(staticColorScheme(it, dark = false).surface, staticColorScheme(it, dark = true).surface)
        }
    }
}
