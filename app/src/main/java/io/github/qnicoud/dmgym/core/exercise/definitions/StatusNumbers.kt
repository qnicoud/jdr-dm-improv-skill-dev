package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.CardSlot
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseOption
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.RankContrast
import io.github.qnicoud.dmgym.core.exercise.RubricQuestion
import io.github.qnicoud.dmgym.core.exercise.ScheduledEvent
import io.github.qnicoud.dmgym.core.exercise.StatusTagRule
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import kotlin.time.Duration.Companion.minutes

/**
 * Status Numbers (exercice 4) : une carte de 1 à 10 et un rôle de PNJ, joués à voix haute. Pas de clavier pendant
 * le jeu ; des notes facultatives après la scène.
 */
object StatusNumbers {
    const val ROLE_SLOT = "role"
    const val CARD_SLOT = "card"
    const val SHIFT_SLOT = "shift"
    const val SHIFT_EVENT = "shift"
    const val NOTES_FIELD = "notes"

    /** Option : statut opposé au rang `rank:*` du rôle (D45). */
    const val OPTION_COUNTER = "counter_stereotype"

    /** Option : un événement change le statut entre 40 et 60 % du temps. */
    const val OPTION_SHIFT = "shift"

    /** Étiquettes de `status_shifts` : un statut haut descend, un statut bas monte. */
    private const val SHIFT_DOWN = "shift:down"
    private const val SHIFT_UP = "shift:up"

    private const val SHIFT_FROM = 0.4
    private const val SHIFT_TO = 0.6

    val definition =
        ExerciseDefinition(
            id = ExerciseId.STATUS_NUMBERS,
            name = R.string.exercise_status_numbers,
            discipline = R.string.discipline_status_numbers,
            instructions = R.string.status_instructions,
            reminder = R.string.status_reminder,
            timer = TimerPolicy.Required(3.minutes),
            slots =
                listOf(
                    TableSlot(ROLE_SLOT, TableId.NpcRoles),
                    CardSlot(CARD_SLOT, contrast = RankContrast(ROLE_SLOT, OPTION_COUNTER)),
                ),
            layout = FieldLayout.None(finishLabel = R.string.status_scene_done),
            rubric =
                listOf(
                    RubricQuestion("readable", R.string.status_rubric_readable),
                    RubricQuestion("cliche", R.string.status_rubric_cliche),
                ),
            events =
                listOf(
                    ScheduledEvent(
                        id = SHIFT_EVENT,
                        timing = TimedEvent.Once(SHIFT_FROM, SHIFT_TO),
                        draw =
                            TableSlot(
                                SHIFT_SLOT,
                                TableId.StatusShifts,
                                tagByStatus = StatusTagRule(CARD_SLOT, high = SHIFT_DOWN, low = SHIFT_UP),
                            ),
                        requiresOption = OPTION_SHIFT,
                    ),
                ),
            options =
                listOf(
                    ExerciseOption(
                        OPTION_COUNTER,
                        R.string.status_option_counter,
                        help = R.string.status_option_counter_help,
                    ),
                    ExerciseOption(
                        OPTION_SHIFT,
                        R.string.status_option_shift,
                        help = R.string.status_option_shift_help,
                    ),
                ),
            endNote =
                FieldSpec(
                    key = NOTES_FIELD,
                    label = R.string.status_notes_label,
                    hint = R.string.status_notes_hint,
                    minLines = 3,
                ),
        )
}
