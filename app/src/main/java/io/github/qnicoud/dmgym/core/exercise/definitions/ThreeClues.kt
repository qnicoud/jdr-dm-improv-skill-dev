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
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlin.time.Duration.Companion.minutes

/**
 * Règle des 3 indices (enquête) : pour une conclusion tirée, trois indices dans trois registres imposés (objet,
 * PNJ, lieu), 5 minutes. Les registres sont fixes (pas de tirage) et l'ordre de saisie est libre.
 */
object ThreeClues {
    const val CONCLUSION_SLOT = "conclusion"
    const val OBJECT_FIELD = "clue_object"
    const val NPC_FIELD = "clue_npc"
    const val PLACE_FIELD = "clue_place"

    val definition =
        ExerciseDefinition(
            id = ExerciseId.THREE_CLUES,
            name = R.string.exercise_three_clues,
            discipline = R.string.discipline_three_clues,
            instructions = R.string.three_clues_instructions,
            reminder = null,
            timer = TimerPolicy.Required(5.minutes),
            slots = listOf(TableSlot(CONCLUSION_SLOT, TableId.ClueConclusions)),
            layout =
                FieldLayout.Fixed(
                    fields =
                        listOf(
                            clueField(
                                OBJECT_FIELD,
                                "object",
                                R.string.three_clues_object_label,
                                R.string.three_clues_object_hint,
                            ),
                            clueField(NPC_FIELD, "npc", R.string.three_clues_npc_label, R.string.three_clues_npc_hint),
                            clueField(
                                PLACE_FIELD,
                                "place",
                                R.string.three_clues_place_label,
                                R.string.three_clues_place_hint,
                            ),
                        ),
                ),
            rubric =
                listOf(
                    RubricQuestion("points_without_saying", R.string.three_clues_rubric_points),
                    RubricQuestion("reusable", R.string.three_clues_rubric_reusable),
                ),
            progress = ProgressRule.FilledFields,
            warmUp = false,
            acceptsFreeSubject = true,
        )

    private fun clueField(
        key: String,
        icon: String,
        label: Int,
        hint: Int,
    ) = FieldSpec(key = key, label = label, hint = hint, minLines = 2, icon = icon)
}
