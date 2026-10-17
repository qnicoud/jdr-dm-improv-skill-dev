package io.github.qnicoud.dmgym.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.core.settings.ThemePalette

/** Formes de docs/ux/design-system.md §4.1 (la forme « carte de tirage » viendra avec l'écran d'exercice). */
val DmGymShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

/**
 * Thème de l'app. [palette] est celle choisie dans les réglages (D53) ; [ThemePalette.DYNAMIC] n'est honorée
 * qu'à partir de l'API 31, sinon on retombe sur « Parchemin ». [darkTheme] vient du réglage Clair / Sombre / Système.
 */
@Composable
fun DmGymTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: ThemePalette = ThemePalette.PARCHMENT,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorSchemeFor(palette.effective(Build.VERSION.SDK_INT), darkTheme),
        typography = DmGymTypography,
        shapes = DmGymShapes,
    ) {
        CompositionLocalProvider(LocalReduceMotion provides reduceMotion, content = content)
    }
}

@Composable
private fun colorSchemeFor(
    palette: ThemePalette,
    dark: Boolean,
): ColorScheme =
    if (palette == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        staticColorScheme(palette.takeUnless { it == ThemePalette.DYNAMIC } ?: ThemePalette.PARCHMENT, dark)
    }
