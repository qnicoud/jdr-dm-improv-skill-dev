package io.github.qnicoud.dmgym.platform.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.qnicoud.dmgym.app.AppContainer
import io.github.qnicoud.dmgym.core.reminder.ReminderDelivery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Application qui expose l'[AppContainer] (injection manuelle, ADR 0005) ; permet de le remplacer dans les tests. */
interface ContainerProvider {
    val container: AppContainer
}

private const val TAG = "DmGymReminder"

private fun BroadcastReceiver.runDelivery(
    context: Context,
    block: suspend ReminderDelivery.() -> Unit,
) {
    val pending = goAsync()
    val delivery = (context.applicationContext as ContainerProvider).container.reminderDelivery
    CoroutineScope(Dispatchers.IO).launch {
        try {
            delivery.block()
        } catch (
            @Suppress("TooGenericExceptionCaught") error: Exception,
        ) {
            Log.e(TAG, "Reminder delivery failed", error)
        } finally {
            pending?.finish()
        }
    }
}

/** L'alarme sonne : décide, publie ou non, et réarme toujours l'occurrence suivante (non exporté). */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        runDelivery(context) { onAlarm() }
    }
}

/**
 * Démarrage, mise à jour de l'app, changement de fuseau ou d'heure : réarme l'alarme. Désactivé dans le manifeste,
 * activé par [AlarmReminderScheduler] seulement quand un rappel est programmé. Aucune donnée de l'intent n'est lue ;
 * seule l'action est filtrée.
 */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in ACTIONS) return
        runDelivery(context) { resync() }
    }

    companion object {
        val ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
            )
    }
}
