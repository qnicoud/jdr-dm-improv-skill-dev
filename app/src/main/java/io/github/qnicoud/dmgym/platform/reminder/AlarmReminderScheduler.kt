package io.github.qnicoud.dmgym.platform.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.github.qnicoud.dmgym.core.reminder.ReminderSchedule
import io.github.qnicoud.dmgym.core.reminder.ReminderScheduler
import io.github.qnicoud.dmgym.core.reminder.nextTrigger
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Planificateur par `AlarmManager` **inexact** (D61) : `setAndAllowWhileIdle`, aucune permission d'alarme exacte.
 * Une seule alarme à la fois ; le récepteur de redémarrage n'est activé que tant qu'un rappel est programmé.
 */
class AlarmReminderScheduler(
    private val context: Context,
    private val clock: Clock,
) : ReminderScheduler {
    override suspend fun sync(schedule: ReminderSchedule) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = alarmIntent()
        alarms.cancel(pending)
        val trigger = nextTrigger(ZonedDateTime.now(clock), schedule)
        setRescheduleReceiverEnabled(trigger != null)
        if (trigger != null) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toEpochMilli(), pending)
    }

    private fun alarmIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun setRescheduleReceiverEnabled(enabled: Boolean) {
        val state =
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, ReminderRescheduleReceiver::class.java),
            state,
            PackageManager.DONT_KILL_APP,
        )
    }

    private companion object {
        const val REQUEST_CODE = 2
    }
}
