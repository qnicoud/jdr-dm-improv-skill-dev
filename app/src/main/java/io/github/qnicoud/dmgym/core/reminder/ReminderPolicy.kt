package io.github.qnicoud.dmgym.core.reminder

enum class SkipReason { AlreadyPracticed, Paused14Days }

sealed interface ReminderDecision {
    data class Skip(
        val reason: SkipReason,
    ) : ReminderDecision

    /** [firstExercise] : aucune session encore ; sinon [textIndex] choisit dans le jeu de textes généraux. */
    data class Notify(
        val textIndex: Int,
        val firstExercise: Boolean,
    ) : ReminderDecision
}

/** Décide, au moment de l'envoi, si le rappel part et quel texte il porte (ADR 0014, UX §2.3). */
object ReminderPolicy {
    /** Nombre de textes du cas général dans `strings.xml`. */
    const val GENERAL_TEXT_COUNT = 3

    /** Jours sans session après lesquels le rappel se met en pause (D62). */
    const val PAUSE_AFTER_DAYS = 14

    fun decide(
        today: Long,
        practicedDays: Set<Long>,
        sessionCount: Int,
        resumedEpochDay: Long?,
    ): ReminderDecision =
        when {
            today in practicedDays -> ReminderDecision.Skip(SkipReason.AlreadyPracticed)
            isPaused(today, practicedDays, resumedEpochDay) -> ReminderDecision.Skip(SkipReason.Paused14Days)
            sessionCount == 0 -> ReminderDecision.Notify(textIndex = 0, firstExercise = true)
            else -> ReminderDecision.Notify(Math.floorMod(today, GENERAL_TEXT_COUNT.toLong()).toInt(), false)
        }

    /** La pause est calculée, jamais stockée : `today − max(dernier jour pratiqué, reprise) ≥ 14`. */
    fun isPaused(
        today: Long,
        practicedDays: Set<Long>,
        resumedEpochDay: Long?,
    ): Boolean {
        val reference = listOfNotNull(practicedDays.maxOrNull(), resumedEpochDay).maxOrNull() ?: return false
        return today - reference >= PAUSE_AFTER_DAYS
    }
}
