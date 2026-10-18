package io.github.qnicoud.dmgym.ui.components

import androidx.annotation.StringRes
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseId

/** Nom affiché d'un exercice. */
@get:StringRes
val ExerciseId.nameRes: Int
    get() =
        when (this) {
            ExerciseId.CRAZY_EIGHTS -> R.string.exercise_crazy_eights
            ExerciseId.CONSTRAINT_BOX -> R.string.exercise_constraint_box
            ExerciseId.CORE_IMAGE -> R.string.exercise_core_image
            ExerciseId.STATUS_NUMBERS -> R.string.exercise_status_numbers
            ExerciseId.DECISION_POINT -> R.string.exercise_decision_point
            ExerciseId.CONNECT_STORY -> R.string.exercise_connect_story
            ExerciseId.ONE_PAGE -> R.string.exercise_one_page
            ExerciseId.YES_AND -> R.string.exercise_yes_and
            else -> error("Unknown exercise: $value")
        }

/** Discipline d'origine de l'exercice. */
@get:StringRes
val ExerciseId.disciplineRes: Int
    get() =
        when (this) {
            ExerciseId.CRAZY_EIGHTS -> R.string.discipline_crazy_eights
            ExerciseId.CONSTRAINT_BOX -> R.string.discipline_constraint_box
            ExerciseId.CORE_IMAGE -> R.string.discipline_core_image
            ExerciseId.STATUS_NUMBERS -> R.string.discipline_status_numbers
            ExerciseId.DECISION_POINT -> R.string.discipline_decision_point
            ExerciseId.CONNECT_STORY -> R.string.discipline_connect_story
            ExerciseId.ONE_PAGE -> R.string.discipline_one_page
            ExerciseId.YES_AND -> R.string.discipline_yes_and
            else -> error("Unknown exercise: $value")
        }
