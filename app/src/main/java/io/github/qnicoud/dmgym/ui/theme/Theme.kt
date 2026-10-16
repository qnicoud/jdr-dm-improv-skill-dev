package io.github.qnicoud.dmgym.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Formes de docs/ux/design-system.md §4.1 (la forme « carte de tirage » viendra avec l'écran d'exercice). */
val DmGymShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

/** Thème de l'app : palette « thème » fixe, sans couleurs dynamiques (design-system §2). */
@Composable
fun DmGymTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DmGymTypography,
        shapes = DmGymShapes,
        content = content,
    )
}
