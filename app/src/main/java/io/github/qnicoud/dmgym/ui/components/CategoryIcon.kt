package io.github.qnicoud.dmgym.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.icons.ComplicationIcons
import io.github.qnicoud.dmgym.ui.icons.SenseIcons
import io.github.qnicoud.dmgym.ui.icons.StoryIcons

/**
 * Icône d'une catégorie d'élément tiré : les cinq sens d'un mot sensoriel (Constraint Box), les types d'élément
 * d'histoire et le PNJ (Connect the Story, Règle des 3 indices) et les six types de complication (Échec qui
 * avance, clé = `id`). Chaque catégorie a une forme propre et un libellé lu par TalkBack :
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
    Cost("cost", ComplicationIcons.Cost, R.string.complication_cost),
    Time("time", ComplicationIcons.Time, R.string.complication_time),
    Information("information", ComplicationIcons.Information, R.string.complication_information),
    Relation("relation", ComplicationIcons.Relation, R.string.complication_relation),
    Position("position", ComplicationIcons.Position, R.string.complication_position),
    Danger("danger", ComplicationIcons.Danger, R.string.complication_danger),
    Npc("npc", StoryIcons.Npc, R.string.category_npc),
    ;

    companion object {
        /** L'icône de la catégorie, `null` si la catégorie est absente ou inconnue. */
        fun fromCategory(category: String?): CategoryIcon? = entries.firstOrNull { it.category == category }
    }
}
