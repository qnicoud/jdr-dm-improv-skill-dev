package io.github.qnicoud.dmgym.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.pow

/** Contraste WCAG 2.x calculé sur les jeux de couleurs réels (design-system §2.4). */
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

    @Test
    fun `light scheme text pairs reach AA`() = assertScheme("clair", LightColors)

    @Test
    fun `dark scheme text pairs reach AA`() = assertScheme("sombre", DarkColors)

    @Test
    fun `outlines reach 3 to 1 on surfaces in both modes`() {
        listOf("clair" to LightColors, "sombre" to DarkColors).forEach { (name, s) ->
            listOf(s.surface, s.surfaceContainer).forEach {
                assertTrue(ratio(s.outline, it) >= 3.0, "$name outline/$it")
            }
        }
    }
}
