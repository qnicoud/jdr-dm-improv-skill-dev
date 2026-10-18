package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.exercise.definitions.ConnectStory
import io.github.qnicoud.dmgym.core.exercise.definitions.ConstraintBox
import io.github.qnicoud.dmgym.core.exercise.definitions.CoreImage
import io.github.qnicoud.dmgym.core.exercise.definitions.CrazyEights
import io.github.qnicoud.dmgym.core.exercise.definitions.DecisionPoint
import io.github.qnicoud.dmgym.core.exercise.definitions.OnePage
import io.github.qnicoud.dmgym.core.exercise.definitions.StatusNumbers
import io.github.qnicoud.dmgym.core.exercise.definitions.ThreeClues

/**
 * Catalogue des définitions d'exercice (ADR 0012). Les définitions des 7 exercices s'y ajoutent aux lots L8
 * et L10 ; tant qu'une définition manque, [get] renvoie `null` et l'écran affiche l'écran provisoire.
 */
class ExerciseRegistry(
    definitions: List<ExerciseDefinition>,
) {
    private val byId: Map<ExerciseId, ExerciseDefinition>

    init {
        require(definitions.map { it.id }.distinct().size == definitions.size) { "duplicate exercise definition" }
        byId = definitions.associateBy { it.id }
    }

    val all: List<ExerciseDefinition> get() = byId.values.toList()

    operator fun get(id: ExerciseId): ExerciseDefinition? = byId[id]

    companion object {
        /** Registre livré : les exercices s'ajoutent par lots (L8, L10), une ligne par exercice. */
        val Default =
            ExerciseRegistry(
                listOf(
                    // L8 : Crazy Eights, Constraint Box, Connect the Story
                    CrazyEights.definition,
                    ConstraintBox.definition,
                    ConnectStory.definition,
                    // L10 : exercices 3, 4, 5 et 7
                    CoreImage.definition,
                    StatusNumbers.definition,
                    DecisionPoint.definition,
                    OnePage.definition,
                    ThreeClues.definition,
                ),
            )
    }
}
