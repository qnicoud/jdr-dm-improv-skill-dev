package io.github.qnicoud.dmgym.core.reminder

import io.github.qnicoud.dmgym.core.history.HistoryRepository
import io.github.qnicoud.dmgym.core.settings.SettingsRepository
import io.github.qnicoud.dmgym.core.time.localEpochDay
import kotlinx.coroutines.flow.first
import java.time.Clock

/** Port de la notification (ADR 0014 §2) ; l'implémentation Android publie sur le canal « reminders ». */
interface ReminderNotifier {
    fun show(notification: ReminderDecision.Notify)
}

/**
 * Ce qui se passe quand l'alarme sonne, ou au redémarrage (ADR 0014 §1). L'occurrence suivante est armée **avant**
 * toute lecture de l'historique : même si celle-ci échoue ou dépasse le délai du récepteur, le rappel survit.
 */
class ReminderDelivery(
    private val settings: SettingsRepository,
    private val history: HistoryRepository,
    private val notifier: ReminderNotifier,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) {
    suspend fun onAlarm() {
        val reminder = settings.settings.first().reminder
        scheduler.sync(reminder.toSchedule())
        if (!reminder.enabled) return
        val decision =
            ReminderPolicy.decide(
                today = clock.localEpochDay(),
                practicedDays = history.observePracticedDays().first().toSet(),
                sessionCount = history.observeSessionCount().first(),
                resumedEpochDay = reminder.resumedEpochDay,
            )
        if (decision is ReminderDecision.Notify) notifier.show(decision)
    }

    /** Réarme depuis les réglages : démarrage de l'app, redémarrage du téléphone, mise à jour, changement d'heure. */
    suspend fun resync() {
        scheduler.sync(
            settings.settings
                .first()
                .reminder
                .toSchedule(),
        )
    }

    /** « Envoyer un test » : même chemin de publication, sans [ReminderPolicy]. */
    fun sendTest() {
        notifier.show(ReminderDecision.Notify(textIndex = 0, firstExercise = false))
    }
}
