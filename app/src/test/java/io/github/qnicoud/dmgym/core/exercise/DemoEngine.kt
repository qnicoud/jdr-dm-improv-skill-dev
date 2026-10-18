package io.github.qnicoud.dmgym.core.exercise

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import kotlin.time.Duration.Companion.minutes

/**
 * Exercice de démonstration des extensions génériques du moteur (ADR 0017, lot V1). Il n'est jamais enregistré dans
 * `ExerciseRegistry.Default` : il sert aux tests de rendu. Deux dispositions, car `FieldLayout` est exclusif :
 * [bound] (champs liés aux tirages) et [offers] (une offre à la fois).
 */
object DemoEngine {
    val id = ExerciseId("demo_engine")
    const val TYPES_SLOT = "types"
    const val OFFERS_SLOT = "offers"
    const val OFFER_COUNT = 3
    const val BLOCK_OPTION = "block_first"
    const val BLOCK_FIELD = "block"
    const val YES_FIELD = "yes"

    private val rubric =
        listOf(
            RubricQuestion("q1", R.string.rubric_yes),
            RubricQuestion("q2", R.string.rubric_no),
            RubricQuestion("q3", R.string.rubric_yes, visibleWhen = OptionCondition(BLOCK_OPTION)),
        )

    private val base =
        ExerciseDefinition(
            id = id,
            name = R.string.exercise_crazy_eights,
            discipline = R.string.discipline_crazy_eights,
            instructions = R.string.exercise_crazy_eights,
            reminder = null,
            timer = TimerPolicy.Required(5.minutes),
            slots = emptyList(),
            layout = FieldLayout.Single(FieldSpec("unused")),
            rubric = rubric,
            options = listOf(ExerciseOption(BLOCK_OPTION, R.string.exercise_crazy_eights)),
        )

    /** Trois champs liés à trois types tirés, icône propre au champ, choix « ma préférée » au bilan. */
    val bound =
        base.copy(
            slots = listOf(TableSlot(TYPES_SLOT, TableId.ComplicationTypes, count = 3)),
            layout =
                FieldLayout.Fixed(
                    fields =
                        (1..3).map {
                            FieldSpec(
                                "complication_$it",
                                label = R.string.core_image_answer_label,
                                icon = "place",
                            )
                        },
                    boundSlot = TYPES_SLOT,
                    hintByCode = mapOf("cost" to R.string.core_image_answer_hint),
                ),
            endChoice = true,
            endChoiceLabel = R.string.export_played_first,
        )

    /** Offres révélées une à une, deux champs avec l'option, préfixe, rythme de 60 s. */
    val offers =
        base.copy(
            slots = listOf(TableSlot(OFFERS_SLOT, TableId.PlayerOffers, count = OFFER_COUNT)),
            layout =
                FieldLayout.Sequential(
                    questionsSlot = OFFERS_SLOT,
                    fields =
                        listOf(
                            FieldSpec(BLOCK_FIELD, visibleWhen = OptionCondition(BLOCK_OPTION), softLimit = 80),
                            FieldSpec(YES_FIELD, prefix = R.string.rubric_yes),
                        ),
                    itemLabel = R.string.exercise_progress,
                    revealProgressively = true,
                ),
            events = listOf(ScheduledEvent("beat", TimedEvent.Periodic(60_000, haptic = true))),
        )
}
