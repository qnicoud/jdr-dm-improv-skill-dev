package io.github.qnicoud.dmgym.feature.exercise

import androidx.annotation.StringRes
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.exercise.Progress

enum class LoadStatus { Loading, Ready, DrawFailed }

enum class SaveStatus { Idle, Saved, Failed }

/** État immuable de l'écran d'exercice. */
data class ExerciseUiState(
    val definition: ExerciseDefinition,
    val session: ExerciseSession,
    val load: LoadStatus = LoadStatus.Loading,
    val progress: Progress? = null,
    val save: SaveStatus = SaveStatus.Idle,
    /** Nombre de groupes de saisie affichés (disposition répétable). */
    val groupCount: Int = 1,
    /** Écran maintenu allumé tant que le chrono tourne (ADR 0006). */
    val keepScreenOn: Boolean = false,
)

/** Événements ponctuels, consommés par l'écran pendant l'état `STARTED`. */
sealed interface ExerciseEvent {
    /** Annonce TalkBack polie, une seule fois par jalon. */
    data class Announce(
        val milestone: Milestone,
    ) : ExerciseEvent

    /** Rappel périodique (par ex. vibration chaque minute de Crazy Eights). */
    data class Reminder(
        val eventId: String,
        val haptic: Boolean,
    ) : ExerciseEvent

    /** Un événement unique a eu lieu (bascule de Status Numbers) : annonce et vibration courte. */
    data class Shift(
        val text: String,
    ) : ExerciseEvent

    /** Message bref (snackbar) : l'action n'a pas abouti, rien n'est perdu. */
    data class Snackbar(
        @StringRes val textRes: Int,
    ) : ExerciseEvent
}
