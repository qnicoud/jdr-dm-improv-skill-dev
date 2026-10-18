package io.github.qnicoud.dmgym.ui.components

import androidx.annotation.StringRes
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry

/** Nom affiché d'un exercice. */
@get:StringRes
val ExerciseId.nameRes: Int
    get() = definition().name

/** Discipline d'origine de l'exercice. */
@get:StringRes
val ExerciseId.disciplineRes: Int
    get() = definition().discipline

/** Nom et discipline viennent de la définition du registre : un nouvel exercice n'ajoute rien ici. */
private fun ExerciseId.definition() = requireNotNull(ExerciseRegistry.Default[this]) { "Unknown exercise: $value" }
