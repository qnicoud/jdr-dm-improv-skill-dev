package io.github.qnicoud.dmgym.platform.reminder

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import io.github.qnicoud.dmgym.feature.settings.NotificationPermissionGate

class AndroidNotificationPermissionGate(
    private val context: Context,
) : NotificationPermissionGate {
    override val needsRuntimePermission: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun notificationsAllowed(): Boolean {
        val channel =
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(ReminderIntents.CHANNEL_ID)
        val channelBlocked = channel?.importance == NotificationManager.IMPORTANCE_NONE
        return NotificationManagerCompat.from(context).areNotificationsEnabled() && !channelBlocked
    }

    override fun openAppNotificationSettings() {
        start(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS))
    }

    override fun openChannelSettings() {
        start(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_CHANNEL_ID, ReminderIntents.CHANNEL_ID),
        )
    }

    private fun start(intent: Intent) {
        context.startActivity(
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
