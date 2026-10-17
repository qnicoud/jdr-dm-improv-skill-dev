package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseOption
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.OptionCondition
import io.github.qnicoud.dmgym.core.exercise.RubricQuestion
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlin.time.Duration.Companion.minutes

/**
 * One-Page Design (exercice 7) : condenser une situation en une page (PNJ, motivations, liens). Minuteur
 * conseillé de 15 min, sans minuteur par défaut (réglage `untimed`, voir `ExerciseId.UNTIMED_BY_DEFAULT`).
 */
object OnePage {
    const val SITUATION_SLOT = "situation"

    /** Option : sans campagne, une situation est tirée dans `one_page_situations` (sinon, champ « Situation »). */
    const val OPTION_DRAWN = "drawn_situation"

    const val CONTEXT_FIELD = "context"
    const val NAME_FIELD = "npc_name"
    const val MOTIVATION_FIELD = "npc_motivation"
    const val LINKS_FIELD = "npc_links"
    const val INACTION_FIELD = "if_pcs_idle"

    /** Plafond de PNJ (D40). */
    const val MAX_NPCS = 5

    /** Compteur souple de la motivation (D40). */
    const val MOTIVATION_SOFT_LIMIT = 80

    val definition =
        ExerciseDefinition(
            id = ExerciseId.ONE_PAGE,
            name = R.string.exercise_one_page,
            discipline = R.string.discipline_one_page,
            instructions = R.string.one_page_instructions,
            reminder = null,
            timer = TimerPolicy.Optional(15.minutes),
            slots =
                listOf(
                    TableSlot(
                        SITUATION_SLOT,
                        TableId.OnePageSituations,
                        onlyIf = OptionCondition(OPTION_DRAWN),
                    ),
                ),
            layout =
                FieldLayout.RepeatableGroup(
                    fields =
                        listOf(
                            FieldSpec(NAME_FIELD, label = R.string.one_page_name_label),
                            FieldSpec(
                                MOTIVATION_FIELD,
                                label = R.string.one_page_motivation_label,
                                hint = R.string.one_page_motivation_hint,
                                softLimit = MOTIVATION_SOFT_LIMIT,
                            ),
                            FieldSpec(
                                LINKS_FIELD,
                                label = R.string.one_page_links_label,
                                hint = R.string.one_page_links_hint,
                            ),
                        ),
                    max = MAX_NPCS,
                    before =
                        listOf(
                            FieldSpec(
                                CONTEXT_FIELD,
                                label = R.string.one_page_context_label,
                                hint = R.string.one_page_context_hint,
                                minLines = 2,
                                visibleWhen = OptionCondition(OPTION_DRAWN, expected = false),
                            ),
                        ),
                    after =
                        listOf(
                            FieldSpec(
                                INACTION_FIELD,
                                label = R.string.one_page_inaction_label,
                                hint = R.string.one_page_inaction_hint,
                                minLines = 2,
                            ),
                        ),
                    title = R.string.one_page_npc_title,
                    addLabel = R.string.one_page_npc_add,
                ),
            rubric =
                listOf(
                    RubricQuestion("fits", R.string.one_page_rubric_fits),
                    RubricQuestion("tomorrow", R.string.one_page_rubric_tomorrow),
                ),
            options = listOf(ExerciseOption(OPTION_DRAWN, R.string.one_page_option_drawn)),
            warmUp = false,
            acceptsFreeSubject = true,
        )
}
