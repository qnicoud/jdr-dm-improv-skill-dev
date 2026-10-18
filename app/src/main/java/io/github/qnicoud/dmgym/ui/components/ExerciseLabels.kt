package io.github.qnicoud.dmgym.ui.components

import androidx.annotation.StringRes
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseRegistry

/** Nom affiché d'un exercice. */
@get:StringRes
val ExerciseId.nameRes: Int
    get() = definition()?.name ?: R.string.exercise_unknown

/** Discipline d'origine de l'exercice. */
@get:StringRes
val ExerciseId.disciplineRes: Int
    get() = definition()?.discipline ?: R.string.discipline_unknown

/**
 * Nom et discipline viennent de la définition du registre : un nouvel exercice n'ajoute rien ici. Un identifiant
 * inconnu (session d'une version plus récente) prend un libellé neutre au lieu de faire planter l'écran.
 */
private fun ExerciseId.definition() = ExerciseRegistry.Default[this]
