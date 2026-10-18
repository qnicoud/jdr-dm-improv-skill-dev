package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.RubricQuestion
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlin.time.Duration.Companion.minutes

/** Core Image Threading (exercice 3) : une image centrale, puis des questions de worldbuilding, une à la fois. */
object CoreImage {
    const val IMAGE_SLOT = "image"
    const val QUESTIONS_SLOT = "questions"
    const val ANSWER_FIELD = "answer"

    /** Nombre de questions par défaut (D31). */
    const val QUESTION_COUNT = 6

    /** Catégories de `threading_questions`, dans l'ordre où les questions sont posées (ADR 0009). */
    val CATEGORIES = listOf("what", "origin", "who", "link")

    val definition =
        ExerciseDefinition(
            id = ExerciseId.CORE_IMAGE,
            name = R.string.exercise_core_image,
            discipline = R.string.discipline_core_image,
            instructions = R.string.core_image_instructions,
            reminder = null,
            timer = TimerPolicy.Required(10.minutes),
            slots =
                listOf(
                    TableSlot(IMAGE_SLOT, TableId.CoreImages),
                    TableSlot(
                        QUESTIONS_SLOT,
                        TableId.ThreadingQuestions,
                        count = QUESTION_COUNT,
                        categoryCycle = CATEGORIES,
                    ),
                ),
            layout =
                FieldLayout.Sequential(
                    questionsSlot = QUESTIONS_SLOT,
                    fields =
                        listOf(
                            FieldSpec(
                                key = ANSWER_FIELD,
                                label = R.string.core_image_answer_label,
                                hint = R.string.core_image_answer_hint,
                                minLines = 2,
                            ),
                        ),
                ),
            rubric =
                listOf(
                    RubricQuestion("surprise", R.string.core_image_rubric_surprise),
                    RubricQuestion("sentence", R.string.core_image_rubric_sentence),
                ),
            acceptsFreeSubject = true,
            warmUp = false,
        )
}
