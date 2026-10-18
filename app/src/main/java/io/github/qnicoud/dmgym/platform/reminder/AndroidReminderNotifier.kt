package io.github.qnicoud.dmgym.platform.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.qnicoud.dmgym.MainActivity
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.reminder.ReminderDecision
import io.github.qnicoud.dmgym.core.reminder.ReminderNotifier

/** Constantes partagées entre la notification et `MainActivity` (ADR 0014 §2). */
object ReminderIntents {
    const val EXTRA_DESTINATION = "destination"
    const val DESTINATION_DAILY = "daily"
    const val CHANNEL_ID = "reminders"
    const val NOTIFICATION_ID = 1
    const val OPEN_REQUEST_CODE = 1
}

/** Crée le canal (importance **basse**, D60) et publie la notification du rappel. */
class AndroidReminderNotifier(
    private val context: Context,
) : ReminderNotifier {
    /** Idempotent ; l'appel à chaque démarrage met à jour le nom traduit. L'importance n'est jamais modifiée. */
    fun ensureChannel() {
        val channel =
            NotificationChannel(
                ReminderIntents.CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun show(notification: ReminderDecision.Notify) {
        ensureChannel()
        if (!canPost()) return
        val built =
            NotificationCompat
                .Builder(context, ReminderIntents.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_reminder)
                .setContentTitle(context.getString(R.string.reminder_notification_title))
                .setContentText(textOf(notification))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openDailyIntent())
                .build()
        // La permission est vérifiée par canPost().
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(ReminderIntents.NOTIFICATION_ID, built)
    }

    private fun canPost(): Boolean {
        val granted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun textOf(notification: ReminderDecision.Notify): String =
        if (notification.firstExercise) {
            context.getString(R.string.reminder_text_first)
        } else {
            val texts = context.resources.getStringArray(R.array.reminder_texts_general)
            texts[notification.textIndex.mod(texts.size)]
        }

    private fun openDailyIntent(): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java)
                .putExtra(ReminderIntents.EXTRA_DESTINATION, ReminderIntents.DESTINATION_DAILY)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
        return PendingIntent.getActivity(
            context,
            ReminderIntents.OPEN_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
