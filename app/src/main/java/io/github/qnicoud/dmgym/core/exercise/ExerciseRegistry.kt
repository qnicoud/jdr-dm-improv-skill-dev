package io.github.qnicoud.dmgym.core.exercise

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
        /** Registre livré : vide jusqu'aux lots L8 et L10. */
        val Default = ExerciseRegistry(emptyList())
    }
}
