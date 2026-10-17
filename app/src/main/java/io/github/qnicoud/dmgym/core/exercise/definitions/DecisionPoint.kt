package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseOption
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.OptionCondition
import io.github.qnicoud.dmgym.core.exercise.ProgressRule
import io.github.qnicoud.dmgym.core.exercise.RubricQuestion
import io.github.qnicoud.dmgym.core.exercise.StartVariant
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlin.time.Duration.Companion.seconds

/**
 * Decision Point Drill (exercice 5) : 30 secondes pour écrire 3 réponses, dont une que l'on ne ferait jamais.
 * Mode « vidéo » par défaut (la situation vient de l'actual play) ; mode « table » : situation tirée.
 */
object DecisionPoint {
    const val SITUATION_SLOT = "situation"

    /** Option : situation tirée dans `surprise_situations` au lieu d'une vidéo. */
    const val OPTION_SITUATION = "situation_mode"

    val FIELD_KEYS = listOf("answer_1", "answer_2", "answer_3")

    val definition =
        ExerciseDefinition(
            id = ExerciseId.DECISION_POINT,
            name = R.string.exercise_decision_point,
            discipline = R.string.discipline_decision_point,
            instructions = R.string.decision_point_instructions,
            reminder = null,
            timer = TimerPolicy.Required(30.seconds),
            slots =
                listOf(
                    TableSlot(
                        SITUATION_SLOT,
                        TableId.SurpriseSituations,
                        onlyIf = OptionCondition(OPTION_SITUATION),
                    ),
                ),
            layout =
                FieldLayout.Fixed(
                    FIELD_KEYS.map {
                        FieldSpec(
                            key = it,
                            hint = R.string.decision_point_hint,
                            flagLabel = R.string.decision_point_never,
                        )
                    },
                ),
            rubric =
                listOf(
                    RubricQuestion("playable", R.string.decision_point_rubric_playable),
                    RubricQuestion("dared", R.string.decision_point_rubric_dared),
                ),
            progress = ProgressRule.FilledFields,
            options = listOf(ExerciseOption(OPTION_SITUATION, R.string.decision_point_option_situation)),
            startVariant =
                StartVariant(R.string.decision_point_pause_done, OptionCondition(OPTION_SITUATION, expected = false)),
            endPrompt = R.string.decision_point_end_prompt,
            endFlagHint = R.string.decision_point_end_hint,
            endChoice = true,
        )
}
