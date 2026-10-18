package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.ProgressRule
import io.github.qnicoud.dmgym.core.exercise.RubricQuestion
import io.github.qnicoud.dmgym.core.exercise.ScheduledEvent
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.TableId
import io.github.qnicoud.dmgym.core.timer.TimedEvent
import kotlin.time.Duration.Companion.minutes

/** Crazy Eights (design thinking) : un problème, 8 minutes, 8 solutions (SPEC exercice 1, D41). */
object CrazyEights {
    const val PROBLEM = "problem"
    const val IDEA_COUNT = 8
    const val MINUTE_REMINDER = "minute_reminder"
    private const val MINUTE_MS = 60_000L

    val definition =
        ExerciseDefinition(
            id = ExerciseId.CRAZY_EIGHTS,
            name = R.string.exercise_crazy_eights,
            discipline = R.string.discipline_crazy_eights,
            instructions = R.string.crazy_eights_instructions,
            reminder = null,
            timer = TimerPolicy.Required(8.minutes),
            slots = listOf(TableSlot(PROBLEM, TableId.Problems)),
            layout =
                FieldLayout.Fixed(
                    (1..IDEA_COUNT).map { FieldSpec(key = "idea$it", hint = R.string.crazy_eights_hint) },
                ),
            progress = ProgressRule.FilledFields,
            events = listOf(ScheduledEvent(MINUTE_REMINDER, TimedEvent.Periodic(MINUTE_MS, haptic = true))),
            rubric =
                listOf(
                    RubricQuestion("beyond_first_ideas", R.string.crazy_eights_rubric_beyond),
                    RubricQuestion("surprising_idea", R.string.crazy_eights_rubric_surprise),
                ),
            acceptsFreeSubject = true,
            warmUp = false,
        )
}
