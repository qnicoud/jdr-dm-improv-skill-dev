package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.ExerciseOption
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.OptionCondition
import io.github.qnicoud.dmgym.core.exercise.RubricQuestion
import io.github.qnicoud.dmgym.core.exercise.ScheduledEvent
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import kotlin.time.Duration.Companion.minutes

/**
 * Oui, et… (théâtre d'impro, ADR 0017 §2) : cinq offres de joueurs révélées une à une, une réponse « Oui, et » à
 * chacune. Le minuteur est global ; les impulsions de rythme ne font jamais avancer d'offre (D58).
 */
object YesAnd {
    const val OFFERS_SLOT = "offers"
    const val BLOCK_FIELD = "block"
    const val YES_AND_FIELD = "yes_and"
    const val BLOCK_FIRST = "block_first"
    const val OFFER_COUNT = 5
    const val BEAT = "minute_beat"
    private const val BLOCK_SOFT_LIMIT = 80

    /** Catégories de `player_offers`, dans l'ordre où les offres sont tirées. */
    val CATEGORIES = listOf("social", "action", "exploration", "unexpected")

    val definition =
        ExerciseDefinition(
            id = ExerciseId.YES_AND,
            name = R.string.exercise_yes_and,
            discipline = R.string.discipline_yes_and,
            instructions = R.string.yes_and_instructions,
            reminder = null,
            timer = TimerPolicy.Required(5.minutes),
            slots =
                listOf(
                    TableSlot(OFFERS_SLOT, TableId.PlayerOffers, count = OFFER_COUNT, categoryCycle = CATEGORIES),
                ),
            layout =
                FieldLayout.Sequential(
                    questionsSlot = OFFERS_SLOT,
                    fields =
                        listOf(
                            FieldSpec(
                                key = BLOCK_FIELD,
                                label = R.string.yes_and_block_label,
                                hint = R.string.yes_and_block_hint,
                                visibleWhen = OptionCondition(BLOCK_FIRST),
                                softLimit = BLOCK_SOFT_LIMIT,
                            ),
                            FieldSpec(
                                key = YES_AND_FIELD,
                                label = R.string.yes_and_title,
                                hint = R.string.yes_and_answer_hint,
                                minLines = 2,
                            ),
                        ),
                    itemLabel = R.string.yes_and_item_label,
                    revealProgressively = true,
                ),
            events = listOf(ScheduledEvent(BEAT, TimedEvent.Steps(OFFER_COUNT, haptic = true))),
            options =
                listOf(
                    ExerciseOption(
                        key = BLOCK_FIRST,
                        label = R.string.yes_and_option_block,
                        default = false,
                        help = R.string.yes_and_option_block_help,
                    ),
                ),
            rubric =
                listOf(
                    RubricQuestion("adds_something", R.string.yes_and_rubric_adds),
                    RubricQuestion("builds_on_offer", R.string.yes_and_rubric_builds),
                    RubricQuestion(
                        "spotted_reflex",
                        R.string.yes_and_rubric_reflex,
                        visibleWhen = OptionCondition(BLOCK_FIRST),
                    ),
                ),
            acceptsFreeSubject = false,
        )
}
