package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.core.exercise.definitions.ConnectStory
import io.github.qnicoud.dmgym.core.exercise.definitions.ConstraintBox
import io.github.qnicoud.dmgym.core.exercise.definitions.CoreImage
import io.github.qnicoud.dmgym.core.exercise.definitions.CrazyEights
import io.github.qnicoud.dmgym.core.exercise.definitions.DecisionPoint
import io.github.qnicoud.dmgym.core.exercise.definitions.FailingForward
import io.github.qnicoud.dmgym.core.exercise.definitions.OnePage
import io.github.qnicoud.dmgym.core.exercise.definitions.StatusNumbers

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
        /**
         * Registre livré. Son ordre est celui de l'accueil, et c'est la **seule source** du catalogue
         * ([ExerciseId.ALL], les durées par défaut, les échauffements) : un exercice s'ajoute par une ligne ici,
         * dans le bloc de son lot, plus son identifiant dans `ExerciseId`.
         */
        val Default =
            ExerciseRegistry(
                listOf(
                    // v1
                    CrazyEights.definition,
                    ConstraintBox.definition,
                    CoreImage.definition,
                    StatusNumbers.definition,
                    DecisionPoint.definition,
                    ConnectStory.definition,
                    OnePage.definition,
                    // v1.1 : V2a
                    FailingForward.definition,
                    // v1.1 : V2b
                    // v1.1 : V2c
                ),
            )
    }
}
