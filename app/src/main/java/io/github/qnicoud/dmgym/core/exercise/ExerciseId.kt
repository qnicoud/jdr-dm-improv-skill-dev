package io.github.qnicoud.dmgym.core.exercise

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Identifiant stable d'un exercice (clé de réglages, d'historique et de route). */
@JvmInline
@Serializable
value class ExerciseId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "exercise id must not be blank" }
    }

    companion object {
        val CRAZY_EIGHTS = ExerciseId("crazy_eights")
        val CONSTRAINT_BOX = ExerciseId("constraint_box")
        val CORE_IMAGE = ExerciseId("core_image")
        val STATUS_NUMBERS = ExerciseId("status_numbers")
        val DECISION_POINT = ExerciseId("decision_point")
        val CONNECT_STORY = ExerciseId("connect_story")
        val ONE_PAGE = ExerciseId("one_page")

        /** Les 7 exercices de la v1, dans l'ordre d'affichage de l'accueil. */
        val ALL: List<ExerciseId> =
            listOf(CRAZY_EIGHTS, CONSTRAINT_BOX, CORE_IMAGE, STATUS_NUMBERS, DECISION_POINT, CONNECT_STORY, ONE_PAGE)

        /** Durées par défaut (docs/ux/parcours.md, DÉCISION 4). One-Page : 15 min conseillées mais sans minuteur par défaut. */
        val DEFAULT_DURATIONS: Map<ExerciseId, Duration> =
            mapOf(
                CRAZY_EIGHTS to 8.minutes,
                CONSTRAINT_BOX to 5.minutes,
                CORE_IMAGE to 10.minutes,
                STATUS_NUMBERS to 3.minutes,
                DECISION_POINT to 30.seconds,
                CONNECT_STORY to 10.minutes,
                ONE_PAGE to 15.minutes,
            )

        val UNTIMED_BY_DEFAULT: Set<ExerciseId> = setOf(ONE_PAGE)
    }
}
