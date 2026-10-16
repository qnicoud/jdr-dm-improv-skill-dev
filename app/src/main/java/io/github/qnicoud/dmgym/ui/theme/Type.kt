package io.github.qnicoud.dmgym.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.qnicoud.dmgym.R

private val WEIGHTS = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

/**
 * Polices variables embarquées (OFL, D38). minSdk 26 gère les polices variables ;
 * en cas d'échec de chargement, Compose retombe sur la police système.
 */
val AlegreyaFamily: FontFamily =
    FontFamily(
        WEIGHTS.map { w ->
            Font(
                R.font.alegreya,
                weight = w,
                variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)),
            )
        },
    )

val AtkinsonFamily: FontFamily =
    FontFamily(
        WEIGHTS.map { w ->
            Font(
                R.font.atkinson_hyperlegible_next,
                weight = w,
                variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)),
            )
        },
    )

private fun serif(
    size: Int,
    line: Int,
    weight: FontWeight,
) = TextStyle(fontFamily = AlegreyaFamily, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun sans(
    size: Int,
    line: Int,
    weight: FontWeight,
) = TextStyle(fontFamily = AtkinsonFamily, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

/** Échelle typographique de docs/ux/design-system.md §3. display* non utilisés : valeurs M3 par défaut. */
val DmGymTypography: Typography =
    Typography(
        headlineLarge = serif(32, 40, FontWeight.Bold),
        headlineMedium = serif(28, 36, FontWeight.Bold),
        headlineSmall = serif(24, 32, FontWeight.SemiBold),
        titleLarge = serif(22, 28, FontWeight.SemiBold),
        titleMedium = sans(16, 24, FontWeight.Bold),
        titleSmall = sans(14, 20, FontWeight.Bold),
        bodyLarge = sans(18, 28, FontWeight.Normal),
        bodyMedium = sans(16, 24, FontWeight.Normal),
        bodySmall = sans(14, 20, FontWeight.Normal),
        labelLarge = sans(14, 20, FontWeight.Bold),
        labelMedium = sans(12, 16, FontWeight.Bold),
        labelSmall = sans(12, 16, FontWeight.Medium),
    )
