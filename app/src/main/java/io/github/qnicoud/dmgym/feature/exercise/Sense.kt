package io.github.qnicoud.dmgym.feature.exercise

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.icons.SenseIcons

/**
 * Sens d'un mot sensoriel (catégorie de `sensory_words`). Chaque sens a une icône de forme propre et un libellé
 * lu par TalkBack : la couleur ne porte jamais l'information.
 */
enum class Sense(
    val category: String,
    val icon: ImageVector,
    @param:StringRes val label: Int,
) {
    Sight("sight", SenseIcons.Sight, R.string.sense_sight),
    Hearing("hearing", SenseIcons.Hearing, R.string.sense_hearing),
    Smell("smell", SenseIcons.Smell, R.string.sense_smell),
    Touch("touch", SenseIcons.Touch, R.string.sense_touch),
    Taste("taste", SenseIcons.Taste, R.string.sense_taste),
    ;

    companion object {
        /** Le sens de la catégorie, `null` si la catégorie est absente ou n'est pas un sens. */
        fun fromCategory(category: String?): Sense? = entries.firstOrNull { it.category == category }
    }
}
