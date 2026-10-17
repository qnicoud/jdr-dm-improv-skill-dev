package io.github.qnicoud.dmgym.core.exercise

/** Intentions appliquées par [reduce]. Le tirage est fait hors du réducteur, qui reste déterministe. */
sealed interface ExerciseAction {
    data class Drawn(
        val draws: List<Draw>,
    ) : ExerciseAction

    /**
     * Relance : les slots présents dans [draws] sont remplacés à leur place, les autres tirages (par exemple la
     * bascule) sont conservés. [slotKey] indique le slot visé (`null` : tous) et sert d'information ; les réponses
     * sont conservées.
     */
    data class Rerolled(
        val slotKey: String?,
        val draws: List<Draw>,
    ) : ExerciseAction

    data class Started(
        val sessionId: Long,
    ) : ExerciseAction

    data class AnswerChanged(
        val key: AnswerKey,
        val text: String,
    ) : ExerciseAction

    /** Notes de fin de séance : acceptées aussi une fois l'exercice terminé. */
    data class NoteChanged(
        val key: AnswerKey,
        val text: String,
    ) : ExerciseAction

    data class FlagToggled(
        val key: AnswerKey,
    ) : ExerciseAction

    /** [occurrences] : nombre d'occurrences par tirage ; absent, il vaut 1 si détecté et 0 sinon. */
    data class AutoDetected(
        val detected: Map<DrawKey, Boolean>,
        val occurrences: Map<DrawKey, Int> = emptyMap(),
    ) : ExerciseAction

    data class CheckToggled(
        val key: DrawKey,
    ) : ExerciseAction

    data object PreviousQuestion : ExerciseAction

    /** Question suivante ; [lastIndex] est l'indice de la dernière question. */
    data class NextQuestion(
        val lastIndex: Int,
    ) : ExerciseAction

    data class OptionChanged(
        val key: String,
        val value: Boolean,
    ) : ExerciseAction

    data class EventFired(
        val eventId: String,
        val draws: List<Draw>,
    ) : ExerciseAction

    data object Finished : ExerciseAction

    data class Assessed(
        val rating: Int?,
        val isGem: Boolean,
        val rubric: Map<String, Boolean>,
    ) : ExerciseAction
}
