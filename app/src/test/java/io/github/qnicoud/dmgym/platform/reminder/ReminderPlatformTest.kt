package io.github.qnicoud.dmgym.platform.reminder

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.app.FakeAppContainer
import io.github.qnicoud.dmgym.app.FakeHistoryRepository
import io.github.qnicoud.dmgym.app.FakeSettingsRepository
import io.github.qnicoud.dmgym.app.MutableClock
import io.github.qnicoud.dmgym.core.reminder.RecordingNotifier
import io.github.qnicoud.dmgym.core.reminder.ReminderDecision
import io.github.qnicoud.dmgym.core.reminder.ReminderDelivery
import io.github.qnicoud.dmgym.core.reminder.ReminderSchedule
import io.github.qnicoud.dmgym.core.reminder.ReminderSettings
import io.github.qnicoud.dmgym.core.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Application de test : le conteneur est fourni par les tests (récepteurs, ADR 0014). */
class ReminderTestApplication :
    Application(),
    ContainerProvider {
    override val container by lazy { checkNotNull(factory) { "factory not set" }(this) }

    companion object {
        var factory: ((Application) -> FakeAppContainer)? = null
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = ReminderTestApplication::class)
class ReminderPlatformTest {
    private val app = ApplicationProvider.getApplicationContext<ReminderTestApplication>()
    private val zone = ZoneId.of("Europe/Paris")

    // Mercredi 14 octobre 2026, 12 h.
    private val clock = MutableClock(Instant.parse("2026-10-14T10:00:00Z"), zone)
    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java))
    private val schedule =
        ReminderSchedule(true, LocalTime.of(18, 30), setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
    private val rescheduleReceiver = ComponentName(app, ReminderRescheduleReceiver::class.java)

    private fun receiverState() = app.packageManager.getComponentEnabledSetting(rescheduleReceiver)

    @Test
    fun `an active schedule sets one inexact wakeup alarm at the next occurrence`() =
        runBlocking {
            AlarmReminderScheduler(app, clock).sync(schedule)

            val alarm = checkNotNull(alarms.peekNextScheduledAlarm())
            assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
            assertEquals(Instant.parse("2026-10-14T16:30:00Z").toEpochMilli(), alarm.triggerAtTime)
            assertEquals(1, alarms.scheduledAlarms.size)
        }

    @Test
    fun `syncing again replaces the alarm instead of adding one`() =
        runBlocking {
            val scheduler = AlarmReminderScheduler(app, clock)
            scheduler.sync(schedule)
            scheduler.sync(schedule.copy(time = LocalTime.of(20, 0)))

            assertEquals(1, alarms.scheduledAlarms.size)
            assertEquals(
                Instant.parse("2026-10-14T18:00:00Z").toEpochMilli(),
                alarms.peekNextScheduledAlarm()!!.triggerAtTime,
            )
        }

    @Test
    fun `disabling cancels the alarm and the boot receiver`() =
        runBlocking {
            val scheduler = AlarmReminderScheduler(app, clock)
            scheduler.sync(schedule)
            assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, receiverState())

            scheduler.sync(schedule.copy(enabled = false))

            assertNull(alarms.peekNextScheduledAlarm())
            assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, receiverState())
        }

    @Test
    fun `the alarm targets the non exported explicit receiver`() =
        runBlocking {
            AlarmReminderScheduler(app, clock).sync(schedule)

            val intent = shadowOf(alarms.peekNextScheduledAlarm()!!.operation).savedIntent
            assertEquals(ReminderAlarmReceiver::class.java.name, intent.component!!.className)
            val info = app.packageManager.getReceiverInfo(ComponentName(app, ReminderAlarmReceiver::class.java), 0)
            assertFalse(info.exported)
        }

    @Test
    fun `the channel has low importance and keeps it on repeated creation`() {
        val notifier = AndroidReminderNotifier(app)
        notifier.ensureChannel()
        notifier.ensureChannel()

        val channel = app.getSystemService(NotificationManager::class.java).getNotificationChannel("reminders")
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
    }

    @Test
    fun `show publishes the general text and opens the daily session on tap`() {
        shadowOf(app).grantPermissions("android.permission.POST_NOTIFICATIONS")

        AndroidReminderNotifier(app).show(ReminderDecision.Notify(textIndex = 1, firstExercise = false))

        val posted = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
        assertEquals("DM Gym", posted.extras.getString("android.title"))
        assertEquals("5 minutes pour échauffer ton imagination ?", posted.extras.getString("android.text"))
        assertTrue(posted.flags and android.app.Notification.FLAG_AUTO_CANCEL != 0)
        assertEquals(0, posted.actions?.size ?: 0)
        val tap = shadowOf(posted.contentIntent).savedIntent
        assertEquals("daily", tap.getStringExtra("destination"))
    }

    @Test
    fun `the first exercise text is used when there is no session`() {
        shadowOf(app).grantPermissions("android.permission.POST_NOTIFICATIONS")

        AndroidReminderNotifier(app).show(ReminderDecision.Notify(0, firstExercise = true))

        val text = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
        assertEquals("Un premier exercice ? Cinq minutes suffisent.", text.extras.getString("android.text"))
    }

    @Test
    fun `no notification is published without the permission`() {
        shadowOf(app).denyPermissions("android.permission.POST_NOTIFICATIONS")

        AndroidReminderNotifier(app).show(ReminderDecision.Notify(0, firstExercise = false))

        assertTrue(shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.isEmpty())
    }

    @Test
    fun `notification texts never mention the streak or loss`() {
        val texts =
            app.resources.getStringArray(io.github.qnicoud.dmgym.R.array.reminder_texts_general).toList() +
                app.getString(io.github.qnicoud.dmgym.R.string.reminder_text_first)
        texts.forEach { text ->
            assertFalse(
                text,
                Regex("série|perd|jours? de suite|!|manqu", RegexOption.IGNORE_CASE).containsMatchIn(text),
            )
        }
    }

    @Test
    fun `the gate reports a blocked channel and opens the settings screens`() {
        val gate = AndroidNotificationPermissionGate(app)
        AndroidReminderNotifier(app).ensureChannel()
        assertTrue(gate.needsRuntimePermission)

        gate.openChannelSettings()
        gate.openAppNotificationSettings()

        val started = generateSequence { shadowOf(app).nextStartedActivity }.toList()
        assertEquals(
            listOf("android.settings.APP_NOTIFICATION_SETTINGS", "android.settings.CHANNEL_NOTIFICATION_SETTINGS"),
            started.map { it.action },
        )
        assertEquals("reminders", started.last().getStringExtra("android.provider.extra.CHANNEL_ID"))
    }

    // Récepteurs ---------------------------------------------------------------------------------------------------

    private val notifier = RecordingNotifier()

    private fun install(
        reminder: ReminderSettings,
        practiced: List<Long> = emptyList(),
    ) {
        val settings = FakeSettingsRepository(Settings(reminder = reminder))
        val history =
            FakeHistoryRepository(practicedDays = MutableStateFlow(practiced), sessionCount = MutableStateFlow(3))
        ReminderTestApplication.factory = {
            FakeAppContainer(
                settingsRepository = settings,
                historyRepository = history,
                clock = clock,
                reminderDelivery =
                    ReminderDelivery(settings, history, notifier, AlarmReminderScheduler(it, clock), clock),
            )
        }
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertTrue("condition not reached in time", condition())
    }

    @Test
    fun `the boot receiver re-arms the alarm from the stored settings`() {
        install(ReminderSettings(enabled = true))

        ReminderRescheduleReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        awaitUntil { alarms.peekNextScheduledAlarm() != null }
        assertEquals(
            Instant.parse("2026-10-14T16:30:00Z").toEpochMilli(),
            alarms.peekNextScheduledAlarm()!!.triggerAtTime,
        )
    }

    @Test
    fun `the reschedule receiver ignores unrelated actions`() {
        install(ReminderSettings(enabled = true))

        ReminderRescheduleReceiver().onReceive(app, Intent("com.example.OTHER"))

        Thread.sleep(200)
        assertNull(alarms.peekNextScheduledAlarm())
    }

    @Test
    fun `the alarm receiver notifies then arms the next occurrence`() {
        install(ReminderSettings(enabled = true), practiced = listOf(20_738L))
        clock.now = Instant.parse("2026-10-14T16:31:00Z")

        ReminderAlarmReceiver().onReceive(app, Intent())

        awaitUntil { alarms.peekNextScheduledAlarm() != null }
        awaitUntil { notifier.shown.size == 1 }
        // Prochain jour choisi : vendredi 16 octobre à 18 h 30 (heure de Paris).
        assertEquals(
            Instant.parse("2026-10-16T16:30:00Z").toEpochMilli(),
            alarms.peekNextScheduledAlarm()!!.triggerAtTime,
        )
    }

    @Test
    fun `the alarm receiver stays silent after a session today but still re-arms`() {
        val today =
            java.time.LocalDate
                .of(2026, 10, 14)
                .toEpochDay()
        install(ReminderSettings(enabled = true), practiced = listOf(today))
        clock.now = Instant.parse("2026-10-14T16:31:00Z")

        ReminderAlarmReceiver().onReceive(app, Intent())

        awaitUntil { alarms.peekNextScheduledAlarm() != null }
        Thread.sleep(200)
        assertTrue(notifier.shown.isEmpty())
        assertNotNull(alarms.peekNextScheduledAlarm())
    }
}
