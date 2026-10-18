package io.github.qnicoud.dmgym.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.icons.SenseIcons
import io.github.qnicoud.dmgym.ui.icons.StoryIcons

/**
 * Icône d'une catégorie d'élément tiré : les cinq sens d'un mot sensoriel (Constraint Box) et les cinq types
 * d'élément d'histoire (Connect the Story). Chaque catégorie a une forme propre et un libellé lu par TalkBack :
 * la couleur ne porte jamais l'information.
 */
enum class CategoryIcon(
    val category: String,
    val icon: ImageVector,
    @param:StringRes val label: Int,
) {
    Sight("sight", SenseIcons.Sight, R.string.sense_sight),
    Hearing("hearing", SenseIcons.Hearing, R.string.sense_hearing),
    Smell("smell", SenseIcons.Smell, R.string.sense_smell),
    Touch("touch", SenseIcons.Touch, R.string.sense_touch),
    Taste("taste", SenseIcons.Taste, R.string.sense_taste),
    Place("place", StoryIcons.Place, R.string.connect_story_place),
    Object("object", StoryIcons.Object, R.string.connect_story_object),
    Trait("trait", StoryIcons.Trait, R.string.connect_story_trait),
    Sensory("sensory", StoryIcons.Sensory, R.string.connect_story_sensory),
    Line("line", StoryIcons.Line, R.string.connect_story_line),
    Npc("npc", StoryIcons.Npc, R.string.category_npc),
    ;

    companion object {
        /** L'icône de la catégorie, `null` si la catégorie est absente ou inconnue. */
        fun fromCategory(category: String?): CategoryIcon? = entries.firstOrNull { it.category == category }
    }
}
