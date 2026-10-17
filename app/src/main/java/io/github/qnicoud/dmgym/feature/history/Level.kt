package io.github.qnicoud.dmgym.feature.history

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.icons.ExerciseIcons

/** Niveau d'auto-évaluation : valeur stockée (1 à 3), libellé et icône (jamais la couleur seule). */
enum class Level(
    val value: Int,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Draft(1, R.string.level_draft, ExerciseIcons.Edit),
    Solid(2, R.string.level_solid, ExerciseIcons.Build),
    Ready(3, R.string.level_ready, ExerciseIcons.Shield),
    ;

    companion object {
        fun of(rating: Int?): Level? = entries.firstOrNull { it.value == rating }
    }
}
