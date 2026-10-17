package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.StringResId
import io.github.qnicoud.dmgym.core.exercise.TableSlot

/**
 * Une ligne de la carte de tirage d'un exercice à détection. [checkable] vaut faux pour un tirage qui n'est pas
 * détecté (le lieu de Constraint Box). [auto] vaut vrai quand la case est cochée par la détection seule : un
 * cochage manuel (dans un sens ou dans l'autre) fait foi et retire l'étiquette « auto » (ADR 0008).
 */
data class ChecklistItem(
    val key: DrawKey,
    val label: StringResId?,
    val text: String,
    val checkable: Boolean,
    val checked: Boolean,
    val auto: Boolean,
)

/** Lignes de [draws] dans l'ordre du tirage, avec le libellé du slot et l'état de la case. */
fun checklistItems(
    definition: ExerciseDefinition,
    draws: List<Draw>,
): List<ChecklistItem> {
    val detected =
        definition.detection
            ?.slotKeys
            .orEmpty()
            .toSet()
    val labels = definition.slots.filterIsInstance<TableSlot>().associate { it.key to it.label }
    return draws.map { draw ->
        val check = draw.check
        ChecklistItem(
            key = DrawKey(draw.slotKey, draw.position),
            label = labels[draw.slotKey],
            text = draw.text,
            checkable = draw.slotKey in detected,
            checked = check?.checked == true,
            auto = check != null && check.manual == null && check.auto,
        )
    }
}

/** Nombre de lignes cochées et de lignes cochables. */
fun List<ChecklistItem>.progress(): Pair<Int, Int> = count { it.checkable && it.checked } to count { it.checkable }
