package io.github.qnicoud.dmgym.core.exercise

/** Identifiant stable d'un exercice (clé de réglages et d'historique). */
@JvmInline
value class ExerciseId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "exercise id must not be blank" }
    }
}
