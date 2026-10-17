package io.github.qnicoud.dmgym.core.exercise.definitions

import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.DetectionSpec
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseId
import io.github.qnicoud.dmgym.core.exercise.FieldLayout
import io.github.qnicoud.dmgym.core.exercise.FieldSpec
import io.github.qnicoud.dmgym.core.exercise.RubricQuestion
import io.github.qnicoud.dmgym.core.exercise.StringResId
import io.github.qnicoud.dmgym.core.exercise.TableSlot
import io.github.qnicoud.dmgym.core.exercise.TimerPolicy
import io.github.qnicoud.dmgym.core.tables.TableId
import kotlin.time.Duration.Companion.minutes

/** Connect the Story (écriture) : 5 éléments, un par catégorie, 10 minutes pour les relier (SPEC exercice 6). */
object ConnectStory {
    const val TEXT = "scene"
    private const val MIN_LINES = 8

    private val categories: List<Pair<String, StringResId>> =
        listOf(
            "place" to R.string.connect_story_place,
            "object" to R.string.connect_story_object,
            "trait" to R.string.connect_story_trait,
            "sensory" to R.string.connect_story_sensory,
            "line" to R.string.connect_story_line,
        )

    val definition =
        ExerciseDefinition(
            id = ExerciseId.CONNECT_STORY,
            name = R.string.exercise_connect_story,
            discipline = R.string.discipline_connect_story,
            instructions = R.string.connect_story_instructions,
            reminder = null,
            timer = TimerPolicy.Required(10.minutes),
            slots =
                categories.map { (category, label) ->
                    TableSlot(category, TableId.StoryElements, category = category, label = label)
                },
            layout =
                FieldLayout.Single(
                    FieldSpec(
                        key = TEXT,
                        label = R.string.connect_story_field_label,
                        hint = R.string.connect_story_field_hint,
                        minLines = MIN_LINES,
                    ),
                ),
            detection = DetectionSpec(slotKeys = categories.map { it.first }, field = TEXT),
            rubric =
                listOf(
                    RubricQuestion("connection_felt_planned", R.string.connect_story_rubric_planned),
                    RubricQuestion("linked_unexpected_element", R.string.connect_story_rubric_unexpected),
                ),
            collapsiblePanel = true,
        )
}
