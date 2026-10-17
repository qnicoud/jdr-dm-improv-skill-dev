package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.DetectionSpec
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.RubricQuestion
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.DistinctBy
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlin.time.Duration.Companion.minutes

/**
 * Constraint Box (jazz) : un lieu et 3 mots sensoriels de sens distincts, 5 minutes (SPEC exercice 2).
 * Un seul domaine, « Lieu », donc aucun sélecteur (D40).
 */
object ConstraintBox {
    const val PLACE = "place"
    const val WORDS = "words"
    const val TEXT = "text"
    const val WORD_COUNT = 3
    private const val MIN_LINES = 6

    val definition =
        ExerciseDefinition(
            id = ExerciseId.CONSTRAINT_BOX,
            name = R.string.exercise_constraint_box,
            discipline = R.string.discipline_constraint_box,
            instructions = R.string.constraint_box_instructions,
            reminder = null,
            timer = TimerPolicy.Required(5.minutes),
            slots =
                listOf(
                    TableSlot(PLACE, TableId.Places, label = R.string.constraint_box_place),
                    TableSlot(
                        key = WORDS,
                        table = TableId.SensoryWords,
                        count = WORD_COUNT,
                        distinctBy = DistinctBy.Category,
                        label = R.string.constraint_box_words,
                    ),
                ),
            layout =
                FieldLayout.Single(
                    FieldSpec(
                        key = TEXT,
                        label = R.string.constraint_box_field_label,
                        hint = R.string.constraint_box_field_hint,
                        minLines = MIN_LINES,
                    ),
                ),
            detection = DetectionSpec(slotKeys = listOf(WORDS), field = TEXT),
            rubric =
                listOf(
                    RubricQuestion("avoided_easy_visual", R.string.constraint_box_rubric_visual),
                    RubricQuestion("say_it_aloud", R.string.constraint_box_rubric_aloud),
                ),
        )
}
