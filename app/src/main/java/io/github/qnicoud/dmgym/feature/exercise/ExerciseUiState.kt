package io.github.qnicoud.dmgym.feature.exercise

import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.ExerciseSession
import io.github.qnicoud.dmgym.core.exercise.Milestone
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.core.exercise.Progress
import io.github.qnicoud.dmgym.core.exercise.TimerKind
import io.github.qnicoud.dmgym.core.exercise.TimerUi

enum class LoadStatus { Loading, Ready, DrawFailed }

enum class SaveStatus { Idle, Saved, Failed }

/** État immuable de l'écran d'exercice. */
data class ExerciseUiState(
    val definition: ExerciseDefinition,
    val session: ExerciseSession,
    val timer: TimerUi,
    val load: LoadStatus = LoadStatus.Loading,
    val progress: Progress? = null,
    val save: SaveStatus = SaveStatus.Idle,
    /** Nombre de groupes de saisie affichés (disposition répétable). */
    val groupCount: Int = 1,
) {
    /** Écran maintenu allumé en décompte et en temps additionnel, pas en pause (ADR 0006). */
    val keepScreenOn: Boolean
        get() = timer.kind in KEEP_ON_KINDS && session.phase == Phase.Active

    private companion object {
        val KEEP_ON_KINDS = setOf(TimerKind.Running, TimerKind.LastMinute, TimerKind.Overtime, TimerKind.Untimed)
    }
}

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
}
