package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.ProgressRule
import io.github.qnicoud.dmgym.core.exercise.RubricQuestion
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.DistinctBy
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlin.time.Duration.Companion.minutes

/**
 * Échec qui avance (ADR 0017 §2) : un jet raté est tiré, il faut écrire 3 complications, chacune d'un type
 * différent. Les 3 types sont tirés et imposés (D56) : le champ i est lié au i-ème type tiré.
 */
object FailingForward {
    const val SITUATION = "situation"
    const val TYPES = "types"
    private const val TYPE_COUNT = 3

    val FIELD_KEYS = (1..TYPE_COUNT).map { "complication_$it" }

    /** Indication de saisie propre à chaque type de complication (clé : `id` de `complication_types`). */
    private val hints =
        mapOf(
            "cost" to R.string.failing_forward_hint_cost,
            "time" to R.string.failing_forward_hint_time,
            "information" to R.string.failing_forward_hint_information,
            "relation" to R.string.failing_forward_hint_relation,
            "position" to R.string.failing_forward_hint_position,
            "danger" to R.string.failing_forward_hint_danger,
        )

    /** Définition et exemple de chaque type, montrés sur l'écran Prêt. */
    private val helps =
        mapOf(
            "cost" to R.string.complication_cost_help,
            "time" to R.string.complication_time_help,
            "information" to R.string.complication_information_help,
            "relation" to R.string.complication_relation_help,
            "position" to R.string.complication_position_help,
            "danger" to R.string.complication_danger_help,
        )

    val definition =
        ExerciseDefinition(
            id = ExerciseId.FAILING_FORWARD,
            name = R.string.exercise_failing_forward,
            discipline = R.string.discipline_failing_forward,
            instructions = R.string.failing_forward_instructions,
            reminder = null,
            timer = TimerPolicy.Required(4.minutes),
            slots =
                listOf(
                    TableSlot(SITUATION, TableId.FailureSituations),
                    TableSlot(TYPES, TableId.ComplicationTypes, count = TYPE_COUNT, distinctBy = DistinctBy.Id),
                ),
            layout =
                FieldLayout.Fixed(
                    fields =
                        FIELD_KEYS.map {
                            FieldSpec(key = it, label = R.string.failing_forward_field_label, minLines = 2)
                        },
                    boundSlot = TYPES,
                    hintByCode = hints,
                    helpByCode = helps,
                    boundHint = R.string.failing_forward_types_hint,
                ),
            rubric =
                listOf(
                    RubricQuestion("changes_situation", R.string.failing_forward_rubric_changes),
                    RubricQuestion("surprising", R.string.failing_forward_rubric_surprise),
                ),
            progress = ProgressRule.FilledFields,
            acceptsFreeSubject = true,
            endPrompt = R.string.failing_forward_end_prompt,
            endChoice = true,
            endChoiceLabel = R.string.failing_forward_favourite,
        )
}
